package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.SeasonStats
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatsAggregationTest {
    private val games =
        listOf(
            GameStats(team = "Home", gameId = 1, offensivePlayYards = 60, offensivePlayCount = 10, puntYards = 40, puntCount = 1),
            GameStats(team = "Home", gameId = 2, offensivePlayYards = 360, offensivePlayCount = 90, puntYards = 180, puntCount = 3),
        )
    private val seasons =
        listOf(
            SeasonStats(
                team = "Team", seasonNumber = 12,
                offensivePlayYards = 60, offensivePlayCount = 10, puntYards = 40, puntCount = 1,
                opponentOffensivePlayYards = -10, opponentOffensivePlayCount = 2, opponentPuntYards = 40, opponentPuntCount = 1,
            ),
            SeasonStats(
                team = "Team",
                seasonNumber = 12,
                offensivePlayYards = 360,
                offensivePlayCount = 90,
                puntYards = 180,
                puntCount = 3,
                opponentOffensivePlayYards = 10,
                opponentOffensivePlayCount = 8,
            ),
        )

    @Test
    fun `season averages use offensive and opponent totals and eligible counts`() {
        val opponents =
            listOf(
                GameStats(team = "Away", gameId = 1, offensivePlayYards = -10, offensivePlayCount = 2, puntYards = 40, puntCount = 1),
                GameStats(team = "Away", gameId = 2, offensivePlayYards = 10, offensivePlayCount = 8),
            )
        val service = SeasonStatsService(mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
        val stats = service.aggregateGameStatsToSeasonStats(games, "Home", 12, (games + opponents).groupBy { it.gameId }, "SEC")

        assertEquals(420, stats.offensivePlayYards)
        assertEquals(100, stats.offensivePlayCount)
        assertEquals(4.2, stats.averageYardsPerPlay)
        assertEquals(220, stats.puntYards)
        assertEquals(4, stats.puntCount)
        assertEquals(55.0, stats.averagePuntLength)
        assertEquals(10, stats.opponentOffensivePlayCount)
        assertEquals(0.0, stats.opponentAverageYardsPerPlay)
        assertEquals(1, stats.opponentPuntCount)
        assertEquals(40.0, stats.opponentAveragePuntLength)
    }

    @Test
    fun `conference averages use summed team totals and counts`() {
        val service = ConferenceStatsService(mockk(), mockk(), mockk())
        val stats = service.aggregateSeasonStatsToConferenceStats(seasons, Subdivision.FBS, "SEC", 12)

        assertEquals(420, stats.offensivePlayYards)
        assertEquals(100, stats.offensivePlayCount)
        assertEquals(4.2, stats.averageYardsPerPlay)
        assertEquals(220, stats.puntYards)
        assertEquals(4, stats.puntCount)
        assertEquals(55.0, stats.averagePuntLength)
        assertEquals(10, stats.opponentOffensivePlayCount)
        assertEquals(0.0, stats.opponentAverageYardsPerPlay)
        assertEquals(1, stats.opponentPuntCount)
        assertEquals(40.0, stats.opponentAveragePuntLength)
    }

    @Test
    fun `league averages use summed team totals and counts`() {
        val service = LeagueStatsService(mockk(), mockk(), mockk())
        val stats = service.aggregateSeasonStatsToLeagueStats(seasons, Subdivision.FBS, 12)

        assertEquals(420, stats.offensivePlayYards)
        assertEquals(100, stats.offensivePlayCount)
        assertEquals(4.2, stats.averageYardsPerPlay)
    }

    @Test
    fun `playbook averages use summed game totals and counts`() {
        val service = PlaybookStatsService(mockk(), mockk(), mockk())
        val stats = service.aggregateGameStatsToPlaybookStats(games, OffensivePlaybook.AIR_RAID, DefensivePlaybook.FOUR_THREE, 12)

        assertEquals(420, stats.offensivePlayYards)
        assertEquals(100, stats.offensivePlayCount)
        assertEquals(4.2, stats.averageYardsPerPlay)
    }

    @Test
    fun `empty playbooks have finite zero averages`() {
        val service = PlaybookStatsService(mockk(), mockk(), mockk())
        val stats = service.aggregateGameStatsToPlaybookStats(emptyList(), OffensivePlaybook.AIR_RAID, DefensivePlaybook.FOUR_THREE, 12)

        assertEquals(0, stats.offensivePlayCount)
        assertEquals(0.0, stats.averageYardsPerPlay)
    }
}
