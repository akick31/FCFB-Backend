package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.domain.statistics.ConferenceStatsCalculator
import com.fcfb.arceus.domain.statistics.LeagueStatsCalculator
import com.fcfb.arceus.domain.statistics.PlaybookStatsCalculator
import com.fcfb.arceus.domain.statistics.SeasonStatsCalculator
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.model.statistics.SeasonStats
import com.fcfb.arceus.util.statistics.StatsAverageCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatsAggregationTest {
    private val statsAverageCalculator = StatsAverageCalculator()
    private val seasonStatsCalculator = SeasonStatsCalculator(statsAverageCalculator)
    private val conferenceStatsCalculator = ConferenceStatsCalculator(statsAverageCalculator)
    private val leagueStatsCalculator = LeagueStatsCalculator(statsAverageCalculator)
    private val playbookStatsCalculator = PlaybookStatsCalculator(statsAverageCalculator)
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
        val stats =
            seasonStatsCalculator.aggregateGameStatsToSeasonStats(
                games,
                "Home",
                12,
                (games + opponents).groupBy { it.gameId },
                "SEC",
            )

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
        val stats = conferenceStatsCalculator.aggregateSeasonStatsToConferenceStats(seasons, Subdivision.FBS, "SEC", 12)

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
        val stats = leagueStatsCalculator.aggregateSeasonStatsToLeagueStats(seasons, Subdivision.FBS, 12)

        assertEquals(420, stats.offensivePlayYards)
        assertEquals(100, stats.offensivePlayCount)
        assertEquals(4.2, stats.averageYardsPerPlay)
    }

    @Test
    fun `playbook averages use summed game totals and counts`() {
        val stats =
            playbookStatsCalculator.aggregateGameStatsToPlaybookStats(
                games,
                OffensivePlaybook.AIR_RAID,
                DefensivePlaybook.FOUR_THREE,
                12,
            )

        assertEquals(420, stats.offensivePlayYards)
        assertEquals(100, stats.offensivePlayCount)
        assertEquals(4.2, stats.averageYardsPerPlay)
    }

    @Test
    fun `empty playbooks have finite zero averages`() {
        val stats =
            playbookStatsCalculator.aggregateGameStatsToPlaybookStats(
                emptyList(),
                OffensivePlaybook.AIR_RAID,
                DefensivePlaybook.FOUR_THREE,
                12,
            )

        assertEquals(0, stats.offensivePlayCount)
        assertEquals(0.0, stats.averageYardsPerPlay)
    }
}
