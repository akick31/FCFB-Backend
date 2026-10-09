package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.PostseasonConferenceStats
import com.fcfb.arceus.model.PostseasonLeagueStats
import com.fcfb.arceus.model.PostseasonPlaybookStats
import com.fcfb.arceus.model.PostseasonSeasonStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PostseasonConferenceStatsRepository
import com.fcfb.arceus.repositories.PostseasonLeagueStatsRepository
import com.fcfb.arceus.repositories.PostseasonPlaybookStatsRepository
import com.fcfb.arceus.repositories.PostseasonSeasonStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.util.POSTSEASON_START_WEEK
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
        val repository = mockk<PostseasonSeasonStatsRepository>(relaxed = true)
        val gameRepository = mockk<GameStatsRepository>()
        val teamRepository = mockk<TeamRepository>()
        val seasonService = mockk<SeasonStatsService>()
        val saved = slot<PostseasonSeasonStats>()
        every { gameRepository.findAll() } returns
            listOf(GameStats(team = "Home", season = 12, week = POSTSEASON_START_WEEK, gameType = null))
        every { teamRepository.findAll() } returns emptyList()
        every { seasonService.aggregateGameStatsToSeasonStats(any(), any(), any(), any(), any()) } returns
            SeasonStats(
                team = "Home", seasonNumber = 12, offensivePlayYards = 420, offensivePlayCount = 100,
                puntYards = 220, puntCount = 4, opponentOffensivePlayYards = -10, opponentOffensivePlayCount = 2,
                opponentPuntYards = 40, opponentPuntCount = 1,
            )
        every { repository.save(capture(saved)) } answers { saved.captured }
        val service =
            PostseasonSeasonStatsService(repository, gameRepository, teamRepository, seasonService, mockk(), mockk(), mockk(relaxed = true))

        service.generateAllPostseasonSeasonStats()

        assertEquals(420, saved.captured.offensivePlayYards)
        assertEquals(100, saved.captured.offensivePlayCount)
        assertEquals(220, saved.captured.puntYards)
        assertEquals(4, saved.captured.puntCount)
        assertEquals(-10, saved.captured.opponentOffensivePlayYards)
        assertEquals(2, saved.captured.opponentOffensivePlayCount)
        assertEquals(40, saved.captured.opponentPuntYards)
        assertEquals(1, saved.captured.opponentPuntCount)
    }

    @Test
    fun `postseason conference generation preserves totals through both conversions`() {
        val repository = mockk<PostseasonConferenceStatsRepository>(relaxed = true)
        val saved = slot<PostseasonConferenceStats>()
        every { repository.save(capture(saved)) } answers { saved.captured }
        val service =
            PostseasonConferenceStatsService(repository, seasonRepository(), ConferenceStatsService(mockk(), mockk(), mockk()), mockk())

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
        val service = PostseasonLeagueStatsService(repository, seasonRepository(), LeagueStatsService(mockk(), mockk(), mockk()), mockk())

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
                    week = POSTSEASON_START_WEEK,
                    offensivePlaybook = OffensivePlaybook.AIR_RAID,
                    defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                    offensivePlayYards = 60,
                    offensivePlayCount = 10,
                ),
                GameStats(
                    team = "Away",
                    season = 12,
                    week = POSTSEASON_START_WEEK,
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
        val service = PostseasonPlaybookStatsService(repository, gameRepository, PlaybookStatsService(mockk(), mockk(), mockk()), mockk())

        service.generateAllPostseasonPlaybookStats()

        assertEquals(420, saved.captured.offensivePlayYards)
        assertEquals(100, saved.captured.offensivePlayCount)
        assertEquals(4.2, saved.captured.averageYardsPerPlay)
    }
}
