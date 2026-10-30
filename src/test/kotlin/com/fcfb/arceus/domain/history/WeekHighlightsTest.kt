package com.fcfb.arceus.domain.history

import com.fcfb.arceus.model.game.Game
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WeekHighlightsTest {
    private fun game(
        id: Int,
        homeScore: Int,
        awayScore: Int,
        homeSpread: Double? = null,
        homeRank: Int? = 0,
        awayRank: Int? = 0,
    ) = Game().apply {
        gameId = id
        homeTeam = "Home $id"
        awayTeam = "Away $id"
        this.homeScore = homeScore
        this.awayScore = awayScore
        homeVegasSpread = homeSpread
        awayVegasSpread = homeSpread?.let { -it }
        homeTeamRank = homeRank
        awayTeamRank = awayRank
    }

    @Test
    fun `upsets order by spread, then by the higher-ranked loser`() {
        val highlights =
            WeekHighlights(
                listOf(
                    game(1, 21, 14, homeSpread = 7.0, awayRank = 10),
                    game(2, 21, 14, homeSpread = 14.0, awayRank = 10),
                    game(3, 21, 14, homeSpread = 14.0, awayRank = 3),
                    game(4, 21, 14, homeSpread = -3.0, awayRank = 1),
                ),
            )

        assertEquals(listOf(3, 2, 1), highlights.upsets().map { it.gameId })
        assertEquals(14.0, highlights.upsets().first().points)
        assertEquals("Home 3", highlights.upsets().first().winner)
    }

    @Test
    fun `without a spread an unranked team beating a ranked one is an upset`() {
        val upsets = WeekHighlights(listOf(game(1, 10, 7, awayRank = 5))).upsets()

        assertEquals(1, upsets.size)
        assertEquals(0.0, upsets.single().points)
        assertEquals(5, upsets.single().loserRank)
    }

    @Test
    fun `ranked losses list each ranked loser best rank first`() {
        val losses =
            WeekHighlights(listOf(game(1, 3, 10, homeRank = 12), game(2, 30, 0, awayRank = 4), game(3, 7, 0))).rankedLosses()

        assertEquals(listOf(4, 12), losses.map { it.rank })
        assertEquals("Away 2", losses.first().team)
        assertEquals("Home 2", losses.first().opponent)
    }

    @Test
    fun `the biggest spread names the favorite and whether it won`() {
        val spread = WeekHighlights(listOf(game(1, 10, 7, homeSpread = -3.0), game(2, 10, 17, homeSpread = -21.5))).biggestSpread()!!

        assertEquals(2, spread.gameId)
        assertEquals("Home 2", spread.favorite)
        assertEquals(21.5, spread.points)
        assertFalse(spread.favoriteWon)
    }

    @Test
    fun `score-based picks and overtime`() {
        val close = game(1, 21, 20)
        val blowout = game(2, 56, 0)
        val shootout = game(3, 49, 45).apply { quarter = 5 }
        val highlights = WeekHighlights(listOf(close, blowout, shootout))

        assertEquals(1, highlights.closestGameId())
        assertEquals(2, highlights.biggestBlowoutGameId())
        assertEquals(3, highlights.highestScoringGameId())
        assertEquals(listOf(3), highlights.overtimeGameIds())
    }

    @Test
    fun `fastest game runs from start to the last message and skips games without times`() {
        val quick =
            game(1, 7, 0).apply {
                timestamp = "2026-10-01T18:00:00"
                lastMessageTimestamp = "2026-10-01 20:30:00"
            }
        val slow =
            game(2, 7, 0).apply {
                timestamp = "2026-10-01T18:00:00"
                lastMessageTimestamp = "2026-10-03 18:00:00"
            }
        val untimed = game(3, 7, 0)

        val fastest = WeekHighlights(listOf(quick, slow, untimed)).fastestGame()!!

        assertEquals(1, fastest.gameId)
        assertEquals(150, fastest.minutes)
        assertNull(WeekHighlights(listOf(untimed)).fastestGame())
    }
}
