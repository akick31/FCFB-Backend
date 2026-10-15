package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.statistics.SeasonStats
import com.fcfb.arceus.repositories.statistics.SeasonStatsRepository
import com.fcfb.arceus.service.fcfb.statistics.season.SeasonStatsService
import com.fcfb.arceus.service.specification.SeasonStatsSpecificationService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification

class SeasonStatsLeaderboardTest {
    private val repository = mockk<SeasonStatsRepository>()
    private val service =
        SeasonStatsService(repository, mockk(), mockk(), mockk(), SeasonStatsSpecificationService(), mockk(), mockk())
    private val stats =
        listOf(
            SeasonStats(team = "Matching", seasonNumber = 2025, subdivision = Subdivision.FBS, conference = "SEC", wins = 2),
            SeasonStats(team = "Other conference", seasonNumber = 2025, subdivision = Subdivision.FBS, conference = "Big Ten", wins = 4),
            SeasonStats(team = "Other subdivision", seasonNumber = 2025, subdivision = Subdivision.FCS, conference = "SEC", wins = 3),
            SeasonStats(team = "Other season", seasonNumber = 2024, subdivision = Subdivision.FBS, conference = "SEC", wins = 5),
            SeasonStats(team = "Unassigned", seasonNumber = 2025, wins = 1),
        )

    @ParameterizedTest
    @CsvSource(
        "NONE,NONE,NONE,Other season|Other conference|Other subdivision|Matching|Unassigned",
        "2025,NONE,NONE,Other conference|Other subdivision|Matching|Unassigned",
        "NONE,fbs,NONE,Other season|Other conference|Matching",
        "NONE,NONE,sec,Other season|Other subdivision|Matching",
        "2025,fbs,NONE,Other conference|Matching",
        "2025,NONE,sec,Other subdivision|Matching",
        "NONE,fbs,sec,Other season|Matching",
        "2025,fbs,sec,Matching",
        nullValues = ["NONE"],
    )
    fun `leaderboard applies every supplied filter`(
        season: Int?,
        subdivision: String?,
        conference: String?,
        expectedTeams: String,
    ) {
        every { repository.findAll(any<Specification<SeasonStats>>(), any<Pageable>()) } returns PageImpl(stats)

        val response = service.getLeaderboard("wins", season, subdivision, conference)

        assertEquals(expectedTeams.split('|'), response.map { it.team })
    }

    @ParameterizedTest
    @CsvSource(
        "W_INS,false,High|Low|Missing",
        "W_INS,true,Missing|Low|High",
        "average_offensive_diff,false,Low|High|Missing",
        "average_offensive_diff,true,Missing|High|Low",
    )
    fun `leaderboard retains normalized metrics sort directions and null ordering`(
        metric: String,
        ascending: Boolean,
        expectedTeams: String,
    ) {
        val rows =
            listOf(
                SeasonStats(team = "Low", seasonNumber = 2025, wins = 2, averageOffensiveDiff = 10.0),
                SeasonStats(team = "Missing", seasonNumber = 2025, wins = 0, averageOffensiveDiff = null),
                SeasonStats(team = "High", seasonNumber = 2025, wins = 5, averageOffensiveDiff = 20.0),
            )
        every { repository.findAll(any<Specification<SeasonStats>>(), any<Pageable>()) } returns PageImpl(rows)

        val response = service.getLeaderboard(metric, ascending = ascending)

        assertEquals(expectedTeams.split('|'), response.map { it.team })
    }

    @Test
    fun `leaderboard limits the sorted results`() {
        every { repository.findAll(any<Specification<SeasonStats>>(), any<Pageable>()) } returns PageImpl(stats)

        assertEquals(listOf("Other season", "Other conference"), service.getLeaderboard("wins", limit = 2).map { it.team })
    }

    @Test
    fun `leaderboard propagates the original repository failure`() {
        val failure = IllegalStateException("Statistics unavailable")
        every { repository.findAll(any<Specification<SeasonStats>>(), any<Pageable>()) } throws failure

        assertSame(failure, assertThrows(IllegalStateException::class.java) { service.getLeaderboard("wins") })
    }
}
