package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.ranking.RankingMetricType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.RankingMetric
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RankingMetricBatchRepository
import com.fcfb.arceus.repositories.RankingMetricRepository
import com.fcfb.arceus.repositories.TeamRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RankingMetricServiceTest {
    private val rankingMetricRepository: RankingMetricRepository = mockk(relaxed = true)
    private val rankingMetricBatchRepository: RankingMetricBatchRepository = mockk(relaxed = true)
    private val gameRepository: GameRepository = mockk()
    private val gameStatsRepository: GameStatsRepository = mockk()
    private val teamRepository: TeamRepository = mockk()
    private val teamResumeMetricService: TeamResumeMetricService = mockk(relaxed = true)
    private lateinit var rankingMetricService: RankingMetricService

    private val teamNames = listOf("Villanova", "UNLV", "Army", "Toledo")

    private fun team(
        id: Int,
        name: String,
    ) = Team().apply {
        this.id = id
        this.name = name
    }

    private fun game(
        home: String,
        away: String,
        homeScore: Int,
        awayScore: Int,
    ) = Game().apply {
        this.homeTeam = home
        this.awayTeam = away
        this.homeScore = homeScore
        this.awayScore = awayScore
        this.season = 12
        this.week = 1
    }

    @BeforeEach
    fun setup() {
        rankingMetricService =
            RankingMetricService(
                rankingMetricRepository,
                rankingMetricBatchRepository,
                gameRepository,
                gameStatsRepository,
                teamRepository,
                teamResumeMetricService,
            )
        every { teamRepository.findAll() } returns teamNames.mapIndexed { index, name -> team(index + 1, name) }
        every { gameStatsRepository.findBySeasonOrderByGameIdAsc(any()) } returns emptyList()
    }

    private fun stats(
        team: String,
        week: Int,
        offensiveDiff: Double,
        defensiveDiff: Double,
    ) = GameStats().apply {
        this.team = team
        this.season = 12
        this.week = week
        this.averageOffensiveDiff = offensiveDiff
        this.averageDefensiveDiff = defensiveDiff
        this.averageOffensiveSpecialTeamsDiff = offensiveDiff
        this.averageDefensiveSpecialTeamsDiff = defensiveDiff
    }

    private fun computeAndCapture(games: List<Game>): List<RankingMetric> {
        every { gameRepository.getFinalGamesThroughWeek(12, 1) } returns games
        val rows = slot<List<RankingMetric>>()
        every { rankingMetricBatchRepository.batchInsert(capture(rows)) } returns Unit
        rankingMetricService.computeMetrics(12, 1)
        return rows.captured
    }

    private fun valuesFor(
        rows: List<RankingMetric>,
        type: RankingMetricType,
    ): Map<Int, Double> = rows.filter { it.metricType == type }.associate { it.teamId to it.value }

    @Test
    fun `test a blowout beyond the margin cap does not raise ASR further`() {
        val games = listOf(game("Villanova", "UNLV", 70, 0), game("Army", "Toledo", 28, 0))
        val cappedGames = listOf(game("Villanova", "UNLV", 28, 0), game("Army", "Toledo", 28, 0))

        val blowoutAsr = valuesFor(computeAndCapture(games), RankingMetricType.ASR)
        val cappedAsr = valuesFor(computeAndCapture(cappedGames), RankingMetricType.ASR)

        assertEquals(cappedAsr.getValue(1), blowoutAsr.getValue(1), 1e-9)
    }

    @Test
    fun `test margin of victory itself stays uncapped for display`() {
        val rows = computeAndCapture(listOf(game("Villanova", "UNLV", 70, 0), game("Army", "Toledo", 28, 0)))

        assertEquals(70.0, valuesFor(rows, RankingMetricType.MARGIN_OF_VICTORY).getValue(1), 1e-9)
    }

    @Test
    fun `test composite weights record ahead of margin`() {
        val games =
            listOf(
                game("Villanova", "UNLV", 70, 0),
                game("Army", "Toledo", 21, 20),
                game("UNLV", "Toledo", 24, 21),
                game("Army", "Villanova", 17, 14),
            )

        val composite = valuesFor(computeAndCapture(games), RankingMetricType.COMPOSITE)

        assertTrue(
            composite.getValue(3) > composite.getValue(1),
            "Army at 2-0 should outrank 1-1 Villanova despite Villanova's blowout: $composite",
        )
    }

    @Test
    fun `test power rating weights the play-read differential at a fifth of the metric`() {
        assertEquals(1.0, RankingMetricService.POWER_RATING_BASE_WEIGHT + RankingMetricService.POWER_RATING_DIFFERENTIAL_WEIGHT, 1e-9)
        assertEquals(0.2, RankingMetricService.POWER_RATING_DIFFERENTIAL_WEIGHT, 1e-9)
    }

    @Test
    fun `test composite ignores scoring offense and scoring defense`() {
        val base = listOf(game("Villanova", "UNLV", 24, 21), game("Army", "Toledo", 24, 21))
        val inflated = listOf(game("Villanova", "UNLV", 52, 49), game("Army", "Toledo", 52, 49))

        val baseComposite = valuesFor(computeAndCapture(base), RankingMetricType.COMPOSITE)
        val inflatedComposite = valuesFor(computeAndCapture(inflated), RankingMetricType.COMPOSITE)

        assertEquals(baseComposite.getValue(1), inflatedComposite.getValue(1), 1e-6)
    }

    @Test
    fun `test average diff rankings cover the season to date, not just the latest week`() {
        every { gameStatsRepository.findBySeasonOrderByGameIdAsc(12) } returns
            listOf(
                stats("Villanova", 1, offensiveDiff = 100.0, defensiveDiff = 600.0),
                stats("Villanova", 2, offensiveDiff = 300.0, defensiveDiff = 400.0),
            )
        every { gameRepository.getFinalGamesThroughWeek(12, 2) } returns listOf(game("Villanova", "UNLV", 24, 21))
        val rows = slot<List<RankingMetric>>()
        every { rankingMetricBatchRepository.batchInsert(capture(rows)) } returns Unit

        rankingMetricService.computeMetrics(12, 2)

        assertEquals(200.0, valuesFor(rows.captured, RankingMetricType.AVERAGE_OFFENSIVE_DIFF).getValue(1), 1e-9)
        assertEquals(500.0, valuesFor(rows.captured, RankingMetricType.AVERAGE_DEFENSIVE_DIFF).getValue(1), 1e-9)
    }

    @Test
    fun `test average diff rankings ignore weeks after the one being computed`() {
        every { gameStatsRepository.findBySeasonOrderByGameIdAsc(12) } returns
            listOf(
                stats("Villanova", 1, offensiveDiff = 100.0, defensiveDiff = 600.0),
                stats("Villanova", 2, offensiveDiff = 700.0, defensiveDiff = 100.0),
            )
        every { gameRepository.getFinalGamesThroughWeek(12, 1) } returns listOf(game("Villanova", "UNLV", 24, 21))
        val rows = slot<List<RankingMetric>>()
        every { rankingMetricBatchRepository.batchInsert(capture(rows)) } returns Unit

        rankingMetricService.computeMetrics(12, 1)

        assertEquals(100.0, valuesFor(rows.captured, RankingMetricType.AVERAGE_OFFENSIVE_DIFF).getValue(1), 1e-9)
    }

    @Test
    fun `test average diff rankings keep offense low-is-better and defense high-is-better`() {
        assertEquals(false, RankingMetricType.AVERAGE_OFFENSIVE_DIFF.higherIsBetter)
        assertEquals(true, RankingMetricType.AVERAGE_DEFENSIVE_DIFF.higherIsBetter)
        assertEquals(false, RankingMetricType.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF.higherIsBetter)
        assertEquals(true, RankingMetricType.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF.higherIsBetter)
    }

    @Test
    fun `test teams without stats rows are left out of the average diff rankings`() {
        every { gameStatsRepository.findBySeasonOrderByGameIdAsc(12) } returns
            listOf(stats("Villanova", 1, offensiveDiff = 100.0, defensiveDiff = 600.0))
        every { gameRepository.getFinalGamesThroughWeek(12, 1) } returns listOf(game("Villanova", "UNLV", 24, 21))
        val rows = slot<List<RankingMetric>>()
        every { rankingMetricBatchRepository.batchInsert(capture(rows)) } returns Unit

        rankingMetricService.computeMetrics(12, 1)

        val offensive = valuesFor(rows.captured, RankingMetricType.AVERAGE_OFFENSIVE_DIFF)
        assertEquals(setOf(1), offensive.keys)
    }
}
