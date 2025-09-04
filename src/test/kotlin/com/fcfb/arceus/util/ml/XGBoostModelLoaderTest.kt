package com.fcfb.arceus.util.ml

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class XGBoostModelLoaderTest {

    @Test
    fun `should load model successfully`() {
        // Given
        val modelLoader = XGBoostModelLoader()

        // When
        val model = modelLoader.loadModel()

        // Then
        assertNotNull(model, "Model should not be null")
        assertTrue(modelLoader.getNumTrees() > 0, "Should have trees")
        assertEquals(9, modelLoader.getNumFeatures(), "Should have 9 features")
        assertTrue(modelLoader.getBaseScore() >= 0.0, "Base score should be non-negative")
    }

    @Test
    fun `should get feature names correctly`() {
        // Given
        val modelLoader = XGBoostModelLoader()

        // When
        val featureNames = modelLoader.getFeatureNames()

        // Then
        assertEquals(9, featureNames.size, "Should have 9 feature names")
        assertTrue(featureNames.contains("score_diff"), "Should contain score_diff")
        assertTrue(featureNames.contains("time_remaining"), "Should contain time_remaining")
        assertTrue(featureNames.contains("down"), "Should contain down")
        assertTrue(featureNames.contains("yards_to_go"), "Should contain yards_to_go")
        assertTrue(featureNames.contains("ball_location"), "Should contain ball_location")
        assertTrue(featureNames.contains("elo_diff"), "Should contain elo_diff")
        assertTrue(featureNames.contains("timeout_diff"), "Should contain timeout_diff")
        assertTrue(featureNames.contains("field_position"), "Should contain field_position")
        assertTrue(featureNames.contains("quarter"), "Should contain quarter")
    }
}
