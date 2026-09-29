package com.fcfb.arceus.service.fcfb.elo

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EloReplayTest {
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

        val replay = EloReplay.replay(games)

        val afterWeekOne = replay.preGameElo.getValue(2 to "Air Force")
        assertTrue(afterWeekOne < 1500.0)
        assertTrue(replay.finalElo.getValue("Air Force") > afterWeekOne)
    }

    @Test
    fun `pre game rating is the rating before that game is applied`() {
        val replay = EloReplay.replay(listOf(game(1, 1, "A", "B", 10, 3)))

        assertEquals(1500.0, replay.preGameElo.getValue(1 to "A"), 1e-9)
        assertEquals(1500.0, replay.preGameElo.getValue(1 to "B"), 1e-9)
        assertEquals(1516.0, replay.finalElo.getValue("A"), 1e-9)
        assertEquals(1484.0, replay.finalElo.getValue("B"), 1e-9)
    }

    @Test
    fun `replay orders by season and week not input order`() {
        val weekOne = game(1, 1, "A", "B", 10, 3)
        val weekTwo = game(2, 2, "B", "C", 21, 7)

        val forward = EloReplay.replay(listOf(weekOne, weekTwo))
        val reversed = EloReplay.replay(listOf(weekTwo, weekOne))

        assertEquals(forward.finalElo, reversed.finalElo)
    }

    @Test
    fun `scrimmages and unfinished games do not change ratings`() {
        val games =
            listOf(
                game(1, 1, "A", "B", 10, 3, type = GameType.SCRIMMAGE),
                game(2, 1, "A", "C", 0, 0, status = GameStatus.IN_PROGRESS),
            )

        val replay = EloReplay.replay(games)

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

        val total = EloReplay.replay(games).finalElo.values.sum()

        assertEquals(4 * 1500.0, total, 1e-9)
    }
}
