package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.domain.season.SeasonStructure
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.model.statistics.PostseasonConferenceStats
import com.fcfb.arceus.model.statistics.PostseasonLeagueStats
import com.fcfb.arceus.model.statistics.PostseasonPlaybookStats
import com.fcfb.arceus.model.statistics.PostseasonSeasonStats
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.repositories.statistics.PostseasonConferenceStatsRepository
import com.fcfb.arceus.repositories.statistics.PostseasonLeagueStatsRepository
import com.fcfb.arceus.repositories.statistics.PostseasonPlaybookStatsRepository
import com.fcfb.arceus.repositories.statistics.PostseasonSeasonStatsRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.statistics.conference.PostseasonConferenceStatsService
import com.fcfb.arceus.service.fcfb.statistics.league.PostseasonLeagueStatsService
import com.fcfb.arceus.service.fcfb.statistics.playbook.PostseasonPlaybookStatsService
import com.fcfb.arceus.service.fcfb.statistics.season.PostseasonSeasonStatsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PostseasonStatsAggregationTest {
    private val seasons =
        listOf(
            PostseasonSeasonStats(
                team = "Home", seasonNumber = 12, subdivision = Subdivision.FBS, conference = "SEC",
                offensivePlayYards = 60, offensivePlayCount = 10, puntYards = 40, puntCount = 1,
                opponentOffensivePlayYards = -10, opponentOffensivePlayCount = 2, opponentPuntYards = 40, opponentPuntCount = 1,
            ),
            PostseasonSeasonStats(
                team = "Away", seasonNumber = 12, subdivision = Subdivision.FBS, conference = "SEC",
                offensivePlayYards = 360, offensivePlayCount = 90, puntYards = 180, puntCount = 3,
                opponentOffensivePlayYards = 10, opponentOffensivePlayCount = 8,
            ),
        )

    private fun seasonRepository(): PostseasonSeasonStatsRepository =
        mockk<PostseasonSeasonStatsRepository>(relaxed = true).also {
            every { it.findAllByOrderBySeasonNumberDescTeamAsc() } returns seasons
            every { it.findBySeasonNumberOrderByTeamAsc(12) } returns seasons
        }

    @Test
    fun `postseason season generation preserves all totals and counts`() {
        val gameId = 500
        val homeGame =
            GameStats(
                gameId = gameId, team = "Home", season = 12, week = SeasonStructure.POSTSEASON_START_WEEK, gameType = null,
                offensivePlayYards = 420, offensivePlayCount = 100, puntYards = 220, puntCount = 4,
            )
        val awayGame =
            GameStats(
                gameId = gameId, team = "Away", season = 12, week = SeasonStructure.POSTSEASON_START_WEEK, gameType = null,
                offensivePlayYards = -10, offensivePlayCount = 2, puntYards = 40, puntCount = 1,
            )
        val repository = mockk<PostseasonSeasonStatsRepository>(relaxed = true)
        val gameRepository = mockk<GameStatsRepository>()
        val teamRepository = mockk<TeamRepository>()
        val savedStats = mutableListOf<PostseasonSeasonStats>()
        every { gameRepository.findAll() } returns listOf(homeGame, awayGame)
        every { teamRepository.findAll() } returns emptyList()
        every { repository.save(any()) } answers { firstArg<PostseasonSeasonStats>().also { savedStats.add(it) } }
        val service =
            PostseasonSeasonStatsService(
                repository,
                gameRepository,
                teamRepository,
                mockk(),
                mockk(),
                mockk(relaxed = true),
            )

        service.generateAllPostseasonSeasonStats()

        val homeStats = savedStats.first { it.team == "Home" }
        assertEquals(420, homeStats.offensivePlayYards)
        assertEquals(100, homeStats.offensivePlayCount)
        assertEquals(220, homeStats.puntYards)
        assertEquals(4, homeStats.puntCount)
        assertEquals(-10, homeStats.opponentOffensivePlayYards)
        assertEquals(2, homeStats.opponentOffensivePlayCount)
        assertEquals(40, homeStats.opponentPuntYards)
        assertEquals(1, homeStats.opponentPuntCount)
    }

    @Test
    fun `postseason conference generation preserves totals through both conversions`() {
        val repository = mockk<PostseasonConferenceStatsRepository>(relaxed = true)
        val saved = slot<PostseasonConferenceStats>()
        every { repository.save(capture(saved)) } answers { saved.captured }
        val service = PostseasonConferenceStatsService(repository, seasonRepository(), mockk())

        service.generateAllPostseasonConferenceStats()

        assertEquals(420, saved.captured.offensivePlayYards)
        assertEquals(100, saved.captured.offensivePlayCount)
        assertEquals(4.2, saved.captured.averageYardsPerPlay)
        assertEquals(220, saved.captured.puntYards)
        assertEquals(4, saved.captured.puntCount)
        assertEquals(55.0, saved.captured.averagePuntLength)
        assertEquals(0, saved.captured.opponentOffensivePlayYards)
        assertEquals(10, saved.captured.opponentOffensivePlayCount)
        assertEquals(0.0, saved.captured.opponentAverageYardsPerPlay)
        assertEquals(40, saved.captured.opponentPuntYards)
        assertEquals(1, saved.captured.opponentPuntCount)
        assertEquals(40.0, saved.captured.opponentAveragePuntLength)
    }

    @Test
    fun `postseason league generation preserves totals through both conversions`() {
        val repository = mockk<PostseasonLeagueStatsRepository>(relaxed = true)
        val saved = slot<PostseasonLeagueStats>()
        every { repository.save(capture(saved)) } answers { saved.captured }
        val service = PostseasonLeagueStatsService(repository, seasonRepository(), mockk())

        service.generateAllPostseasonLeagueStats()

        assertEquals(420, saved.captured.offensivePlayYards)
        assertEquals(100, saved.captured.offensivePlayCount)
        assertEquals(4.2, saved.captured.averageYardsPerPlay)
    }

    @Test
    fun `postseason playbook generation preserves exact average and counts`() {
        val games =
            listOf(
                GameStats(
                    team = "Home",
                    season = 12,
                    week = SeasonStructure.POSTSEASON_START_WEEK,
                    offensivePlaybook = OffensivePlaybook.AIR_RAID,
                    defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                    offensivePlayYards = 60,
                    offensivePlayCount = 10,
                ),
                GameStats(
                    team = "Away",
                    season = 12,
                    week = SeasonStructure.POSTSEASON_START_WEEK,
                    offensivePlaybook = OffensivePlaybook.AIR_RAID,
                    defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                    offensivePlayYards = 360,
                    offensivePlayCount = 90,
                ),
            )
        val repository = mockk<PostseasonPlaybookStatsRepository>(relaxed = true)
        val gameRepository = mockk<GameStatsRepository>()
        val saved = slot<PostseasonPlaybookStats>()
        every { gameRepository.findAllByOrderBySeasonDescGameIdAsc() } returns games
        every { gameRepository.findBySeasonOrderByGameIdAsc(12) } returns games
        every { repository.save(capture(saved)) } answers { saved.captured }
        val service = PostseasonPlaybookStatsService(repository, gameRepository, mockk())

        service.generateAllPostseasonPlaybookStats()

        assertEquals(420, saved.captured.offensivePlayYards)
        assertEquals(100, saved.captured.offensivePlayCount)
        assertEquals(4.2, saved.captured.averageYardsPerPlay)
    }
}
