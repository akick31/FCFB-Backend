package com.fcfb.arceus.service.fcfb.elo

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
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

    private fun game(
        id: Int,
        week: Int,
        home: String,
        away: String,
        homeScore: Int,
        awayScore: Int,
        status: GameStatus = GameStatus.FINAL,
        type: GameType = GameType.OUT_OF_CONFERENCE,
    ) = Game().apply {
        gameId = id
        season = 1
        this.week = week
        homeTeam = home
        awayTeam = away
        this.homeScore = homeScore
        this.awayScore = awayScore
        gameStatus = status
        gameType = type
    }

    @Test
    fun `a team that wins then loses is rated from the previous game result`() {
        val games =
            listOf(
                game(1, 1, "Air Force", "Wyoming", 28, 38),
                game(2, 2, "Air Force", "Florida", 35, 12),
            )

        val replay = EloCalculator.replay(games)

        val afterWeekOne = replay.preGameElo.getValue(2 to "Air Force")
        assertTrue(afterWeekOne < 1500.0)
        assertTrue(replay.finalElo.getValue("Air Force") > afterWeekOne)
    }

    @Test
    fun `pre game rating is the rating before that game is applied`() {
        val replay = EloCalculator.replay(listOf(game(1, 1, "A", "B", 10, 3)))

        assertEquals(1500.0, replay.preGameElo.getValue(1 to "A"), 1e-9)
        assertEquals(1500.0, replay.preGameElo.getValue(1 to "B"), 1e-9)
        assertEquals(1516.0, replay.finalElo.getValue("A"), 1e-9)
        assertEquals(1484.0, replay.finalElo.getValue("B"), 1e-9)
    }

    @Test
    fun `replay orders by season and week not input order`() {
        val weekOne = game(1, 1, "A", "B", 10, 3)
        val weekTwo = game(2, 2, "B", "C", 21, 7)

        val forward = EloCalculator.replay(listOf(weekOne, weekTwo))
        val reversed = EloCalculator.replay(listOf(weekTwo, weekOne))

        assertEquals(forward.finalElo, reversed.finalElo)
    }

    @Test
    fun `scrimmages and unfinished games do not change ratings`() {
        val games =
            listOf(
                game(1, 1, "A", "B", 10, 3, type = GameType.SCRIMMAGE),
                game(2, 1, "A", "C", 0, 0, status = GameStatus.IN_PROGRESS),
            )

        val replay = EloCalculator.replay(games)

        assertEquals(1500.0, replay.finalElo.getValue("A"), 1e-9)
        assertEquals(1500.0, replay.finalElo.getValue("B"), 1e-9)
        assertEquals(1500.0, replay.finalElo.getValue("C"), 1e-9)
    }

    @Test
    fun `total rating across all teams is conserved`() {
        val games =
            listOf(
                game(1, 1, "A", "B", 10, 3),
                game(2, 1, "C", "D", 3, 24),
                game(3, 2, "A", "C", 7, 14),
                game(4, 2, "B", "D", 28, 27),
            )

        val total = EloCalculator.replay(games).finalElo.values.sum()

        assertEquals(4 * 1500.0, total, 1e-9)
    }
}
