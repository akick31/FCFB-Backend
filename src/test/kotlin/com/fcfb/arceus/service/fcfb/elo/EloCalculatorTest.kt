package com.fcfb.arceus.service.fcfb.elo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EloCalculatorTest {
    @Test
    fun `equal ratings split evenly and the winner gains half of K`() {
        val (home, away) = EloCalculator.updatedRatings(1500.0, 1500.0, homeWon = true)

        assertEquals(1516.0, home, 1e-9)
        assertEquals(1484.0, away, 1e-9)
    }

    @Test
    fun `update is zero sum`() {
        val (home, away) = EloCalculator.updatedRatings(1473.28, 1566.44, homeWon = true)

        assertEquals(1473.28 + 1566.44, home + away, 1e-9)
    }

    @Test
    fun `winner never loses rating and loser never gains`() {
        listOf(1000.0, 1400.0, 1500.0, 1800.0, 2200.0).forEach { winner ->
            listOf(1000.0, 1400.0, 1500.0, 1800.0, 2200.0).forEach { loser ->
                val (newWinner, newLoser) = EloCalculator.updatedRatings(winner, loser, homeWon = true)
                assertTrue(newWinner > winner)
                assertTrue(newLoser < loser)
            }
        }
    }

    @Test
    fun `away win moves ratings the opposite way`() {
        val (home, away) = EloCalculator.updatedRatings(1473.28, 1566.44, homeWon = false)

        assertTrue(home < 1473.28)
        assertTrue(away > 1566.44)
    }
}
