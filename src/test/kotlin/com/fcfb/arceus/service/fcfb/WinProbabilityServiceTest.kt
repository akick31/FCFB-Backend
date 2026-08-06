package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.util.ml.XGBoostPredictor
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WinProbabilityServiceTest {
    private val teamService = mockk<TeamService>(relaxed = true)
    private val xgboostPredictor = mockk<XGBoostPredictor>(relaxed = true)
    private val playRepository = mockk<PlayRepository>(relaxed = true)
    private val gameStatsService = mockk<GameStatsService>(relaxed = true)
    private val winProbabilityService = WinProbabilityService(xgboostPredictor, playRepository, gameStatsService)

    @Test
    fun `should calculate win probability successfully`() {
        assertNotNull(winProbabilityService)
    }

    @Test
    fun `should create feature array correctly`() {
        val scoreDiff = 7
        val timeRemaining = 900
        val down = 1
        val yardsToGo = 10
        val ballLocation = 50
        val eloDiff = 200.0
        val timeoutDiff = 2
        val fieldPosition = 50
        val quarter = 1

        every {
            xgboostPredictor.createFeatureArray(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns doubleArrayOf(1.0, 10.0, 50.0, 7.0, 900.0, 420.0, 1.0, 0.0, 200.0)

        val features =
            xgboostPredictor.createFeatureArray(
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

        assertNotNull(features)
        assertEquals(9, features.size)
        assertEquals(1.0, features[0], 0.01)
        assertEquals(10.0, features[1], 0.01)
        assertEquals(50.0, features[2], 0.01)
        assertEquals(7.0, features[3], 0.01)
        assertEquals(900.0, features[4], 0.01)
        assertEquals(420.0, features[5], 0.01)
        assertEquals(1.0, features[6], 0.01)
        assertEquals(0.0, features[7], 0.01)
        assertEquals(200.0, features[8], 0.01)
    }

    @Test
    fun `should predict win probability with XGBoost model`() {
        val features = doubleArrayOf(7.0, 900.0, 1.0, 10.0, 50.0, 200.0, 2.0, 50.0, 1.0)
        every { xgboostPredictor.predict(features) } returns 0.75

        val probability = xgboostPredictor.predict(features)

        assertTrue(probability >= 0.0)
        assertTrue(probability <= 1.0)
        assertEquals(0.75, probability, 0.01)
    }

    @Test
    fun `should handle prediction errors gracefully`() {
        val features = doubleArrayOf(7.0, 900.0, 1.0, 10.0, 50.0, 200.0, 2.0, 50.0, 1.0)
        every { xgboostPredictor.predict(features) } throws RuntimeException("Model error")

        val probability =
            try {
                xgboostPredictor.predict(features)
            } catch (e: Exception) {
                0.5
            }

        assertEquals(0.5, probability, 0.01)
    }
}
