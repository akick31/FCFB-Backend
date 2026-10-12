package com.fcfb.arceus.util.ml

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class XGBoostPredictorTest {
    private val predictor = XGBoostPredictor()
    private val features = doubleArrayOf(1.0, 10.0, 50.0, 7.0, 900.0, 420.0, 1.0, 0.0, 200.0)

    @AfterEach
    fun releaseModel() {
        predictor.releaseModel()
    }

    @Test
    fun `feature array retains the model input order`() {
        val actual =
            predictor.createFeatureArray(
                down = 1,
                distance = 10,
                position = 50,
                margin = 7,
                secondsLeftGame = 900,
                secondsLeftHalf = 420,
                half = 1,
                hadFirstPossession = 0,
                eloDiffTime = 200.0,
            )

        assertArrayEquals(features, actual)
    }

    @Test
    fun `valid features produce a probability`() {
        val prediction = predictor.predict(features)

        assertTrue(prediction in 0.0..1.0)
    }

    @Test
    fun `real model distinguishes late game leads from deficits without returning the fallback`() {
        val leading = predictor.predict(lateGameFeatures(21))
        val trailing = predictor.predict(lateGameFeatures(-21))

        assertTrue(leading > 0.5)
        assertTrue(trailing < 0.5)
        assertTrue(leading > trailing)
    }

    @Test
    fun `unloaded model retains the default probability`() {
        predictor.releaseModel()

        assertEquals(0.5, predictor.predict(features))
    }

    @Test
    fun `invalid feature count retains the default probability`() {
        assertEquals(0.5, predictor.predict(features.copyOf(8)))
    }

    private fun lateGameFeatures(margin: Int): DoubleArray =
        predictor.createFeatureArray(
            down = 1,
            distance = 10,
            position = 50,
            margin = margin,
            secondsLeftGame = 60,
            secondsLeftHalf = 60,
            half = 2,
            hadFirstPossession = 0,
            eloDiffTime = 0.0,
        )
}
