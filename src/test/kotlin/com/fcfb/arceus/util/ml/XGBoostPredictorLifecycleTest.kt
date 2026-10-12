package com.fcfb.arceus.util.ml

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import ml.dmlc.xgboost4j.java.Booster
import ml.dmlc.xgboost4j.java.DMatrix
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.test.util.ReflectionTestUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Supplier

class XGBoostPredictorLifecycleTest {
    private val model = mockk<Booster>()
    private val predictor = XGBoostPredictor()
    private val features = doubleArrayOf(1.0, 10.0, 50.0, 7.0, 900.0, 420.0, 1.0, 0.0, 200.0)

    @BeforeEach
    fun substituteModel() {
        predictor.releaseModel()
        ReflectionTestUtils.setField(predictor, "booster", model)
        every { model.dispose() } returns Unit
    }

    @AfterEach
    fun releaseModel() {
        predictor.releaseModel()
    }

    @Test
    fun `Spring destruction releases the model once and later predictions retain the unloaded fallback`() {
        val context = AnnotationConfigApplicationContext()
        context.registerBean("predictor", XGBoostPredictor::class.java, Supplier { predictor })
        context.refresh()

        context.close()
        predictor.releaseModel()

        assertEquals(0.5, predictor.predict(features))
        verify(exactly = 1) { model.dispose() }
        verify(exactly = 0) { model.predict(any<DMatrix>()) }
    }

    @Test
    fun `shutdown drains concurrent predictions before releasing the model`() {
        val entered = CountDownLatch(2)
        val finish = CountDownLatch(1)
        val shutdownEntered = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(3)
        every { model.predict(any<DMatrix>()) } answers {
            entered.countDown()
            check(finish.await(5, TimeUnit.SECONDS))
            arrayOf(floatArrayOf(0.7f))
        }

        try {
            val first = executor.submit<Double> { predictor.predict(features) }
            val second = executor.submit<Double> { predictor.predict(features) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val shutdown =
                executor.submit {
                    shutdownEntered.countDown()
                    predictor.releaseModel()
                }
            assertTrue(shutdownEntered.await(5, TimeUnit.SECONDS))
            assertThrows(TimeoutException::class.java) { shutdown.get(100, TimeUnit.MILLISECONDS) }
            verify(exactly = 0) { model.dispose() }

            finish.countDown()
            assertEquals(0.7f.toDouble(), first.get(5, TimeUnit.SECONDS))
            assertEquals(0.7f.toDouble(), second.get(5, TimeUnit.SECONDS))
            shutdown.get(5, TimeUnit.SECONDS)
            verify(exactly = 1) { model.dispose() }
            assertEquals(0.5, predictor.predict(features))
        } finally {
            finish.countDown()
            executor.shutdown()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `prediction failure releases the lock so another thread can shut down`() {
        every { model.predict(any<DMatrix>()) } throws IllegalStateException("Prediction failed")
        assertEquals(0.5, predictor.predict(features))
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit { predictor.releaseModel() }.get(5, TimeUnit.SECONDS)
            verify(exactly = 1) { model.dispose() }
        } finally {
            executor.shutdown()
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }
}
