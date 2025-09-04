package com.fcfb.arceus.util.ml

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class XGBoostPredictorTest {

    @Test
    fun `should create feature array correctly`() {
        // Given
        val modelLoader = mockk<XGBoostModelLoader>(relaxed = true)
        val predictor = XGBoostPredictor(modelLoader)

        // When
        val features = predictor.createFeatureArray(
            down = 1,
            distance = 10,
            position = 50,
            margin = 7,
            secondsLeftGame = 900,
            secondsLeftHalf = 420,
            half = 1,
            hadFirstPossession = 0,
            eloDiffTime = 200.0
        )

        // Then
        assertEquals(9, features.size, "Should have 9 features")
        assertEquals(1.0, features[0], 0.01, "Down should be 1.0")
        assertEquals(10.0, features[1], 0.01, "Distance should be 10.0")
        assertEquals(50.0, features[2], 0.01, "Position should be 50.0")
        assertEquals(7.0, features[3], 0.01, "Margin should be 7.0")
        assertEquals(900.0, features[4], 0.01, "Seconds left game should be 900.0")
        assertEquals(420.0, features[5], 0.01, "Seconds left half should be 420.0")
        assertEquals(1.0, features[6], 0.01, "Half should be 1.0")
        assertEquals(0.0, features[7], 0.01, "Had first possession should be 0.0")
        assertEquals(200.0, features[8], 0.01, "ELO diff time should be 200.0")
    }

    @Test
    fun `should predict with valid features`() {
        // Given
        val modelLoader = mockk<XGBoostModelLoader>(relaxed = true)
        val predictor = XGBoostPredictor(modelLoader)
        val features = doubleArrayOf(1.0, 10.0, 50.0, 7.0, 900.0, 420.0, 1.0, 0.0, 200.0)

        // Mock model data
        every { modelLoader.getModel() } returns mockk(relaxed = true)
        every { modelLoader.getNumFeatures() } returns 9
        every { modelLoader.getBaseScore() } returns 0.5

        // When
        val prediction = predictor.predict(features)

        // Then
        assertTrue(prediction >= 0.0, "Prediction should be >= 0.0")
        assertTrue(prediction <= 1.0, "Prediction should be <= 1.0")
    }

    @Test
    fun `should return default probability when model is null`() {
        // Given
        val modelLoader = mockk<XGBoostModelLoader>(relaxed = true)
        val predictor = XGBoostPredictor(modelLoader)
        val features = doubleArrayOf(1.0, 10.0, 50.0, 7.0, 900.0, 420.0, 1.0, 0.0, 200.0)

        // Mock null model
        every { modelLoader.getModel() } returns null

        // When
        val prediction = predictor.predict(features)

        // Then
        assertEquals(0.5, prediction, 0.01, "Should return default probability of 0.5")
    }
}
