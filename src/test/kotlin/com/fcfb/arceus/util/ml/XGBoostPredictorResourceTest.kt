package com.fcfb.arceus.util.ml

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import io.mockk.verifyOrder
import ml.dmlc.xgboost4j.java.Booster
import ml.dmlc.xgboost4j.java.DMatrix
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

class XGBoostPredictorResourceTest {
    private val model = mockk<Booster>()
    private lateinit var predictor: XGBoostPredictor
    private val features = doubleArrayOf(1.0, 10.0, 50.0, 7.0, 900.0, 420.0, 1.0, 0.0, 200.0)

    @BeforeEach
    fun setUp() {
        predictor = XGBoostPredictor()
        predictor.releaseModel()
        every { model.dispose() } returns Unit
        ReflectionTestUtils.setField(predictor, "booster", model)
        mockkConstructor(DMatrix::class)
        every { anyConstructed<DMatrix>().dispose() } answers { callOriginal() }
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(DMatrix::class)
        predictor.releaseModel()
    }

    @Test
    fun `prediction releases its matrix after returning the model probability`() {
        every { model.predict(any<DMatrix>()) } returns arrayOf(floatArrayOf(0.7f))

        assertEquals(0.7f.toDouble(), predictor.predict(features))

        verify(exactly = 1) { anyConstructed<DMatrix>().dispose() }
        verifyOrder {
            model.predict(any<DMatrix>())
            anyConstructed<DMatrix>().dispose()
        }
    }

    @Test
    fun `prediction failure releases its matrix and retains the fallback`() {
        every { model.predict(any<DMatrix>()) } throws IllegalStateException("Prediction failed")

        assertEquals(0.5, predictor.predict(features))

        verify(exactly = 1) { anyConstructed<DMatrix>().dispose() }
    }

    @Test
    fun `invalid model output releases its matrix and retains the fallback`() {
        every { model.predict(any<DMatrix>()) } returns emptyArray()

        assertEquals(0.5, predictor.predict(features))

        verify(exactly = 1) { anyConstructed<DMatrix>().dispose() }
    }
}
