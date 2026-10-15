package com.fcfb.arceus.service.fcfb.ranking

import com.fcfb.arceus.domain.ranking.AdjustedPointsAgainstRatingsCalculator
import com.fcfb.arceus.domain.ranking.AdjustedPointsForRatingsCalculator
import com.fcfb.arceus.domain.ranking.AdjustedScheduleRatingsCalculator
import com.fcfb.arceus.domain.ranking.AsrRatingsCalculator
import com.fcfb.arceus.domain.ranking.ColleyRatingsCalculator
import com.fcfb.arceus.domain.ranking.CompositeRatingsCalculator
import com.fcfb.arceus.domain.ranking.PowerRatingsCalculator
import com.fcfb.arceus.domain.ranking.RankingFormulas
import com.fcfb.arceus.dto.response.RankingMetricComputeResult
import com.fcfb.arceus.dto.response.RankingMetricResponse
import com.fcfb.arceus.dto.response.TeamRankingMetricResponse
import com.fcfb.arceus.dto.response.TeamRankingMetricTrendResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.ranking.RankingMetricType
import com.fcfb.arceus.exception.ranking.InvalidRankingMetricException
import com.fcfb.arceus.exception.ranking.MetricNotImplementedException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.ranking.RankingMetric
import com.fcfb.arceus.model.ranking.TeamDiffAccumulator
import com.fcfb.arceus.model.ranking.TeamDiffAggregate
import com.fcfb.arceus.model.ranking.TeamSeasonAccumulator
import com.fcfb.arceus.model.ranking.TeamSeasonAggregate
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.ranking.RankingMetricBatchRepository
import com.fcfb.arceus.repositories.ranking.RankingMetricRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.util.ranking.RatingNormalizer
import org.springframework.stereotype.Service

@Service
class RankingMetricService(
    private val rankingMetricRepository: RankingMetricRepository,
    private val rankingMetricBatchRepository: RankingMetricBatchRepository,
    private val gameRepository: GameRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val teamRepository: TeamRepository,
    private val teamResumeMetricService: TeamResumeMetricService,
) {
    private val rankingFormulas = RankingFormulas()
    private val ratingNormalizer = RatingNormalizer()
    private val adjustedScheduleRatingsCalculator = AdjustedScheduleRatingsCalculator()
    private val powerRatingsCalculator = PowerRatingsCalculator(ratingNormalizer, rankingFormulas)
    private val colleyRatingsCalculator = ColleyRatingsCalculator()
    private val asrRatingsCalculator = AsrRatingsCalculator(adjustedScheduleRatingsCalculator, rankingFormulas)
    private val adjustedPointsForRatingsCalculator =
        AdjustedPointsForRatingsCalculator(adjustedScheduleRatingsCalculator, rankingFormulas)
    private val adjustedPointsAgainstRatingsCalculator =
        AdjustedPointsAgainstRatingsCalculator(adjustedScheduleRatingsCalculator, rankingFormulas)
    private val compositeRatingsCalculator = CompositeRatingsCalculator(ratingNormalizer)

    fun computeMetrics(
        season: Int,
        week: Int,
    ): RankingMetricComputeResult {
        val teamsByName = teamRepository.findAll().mapNotNull { team -> team.name?.let { it to team } }.toMap()
        val games = gameRepository.getFinalGamesThroughWeek(season, week)
        val aggregates = buildAggregates(games, teamsByName)
        val rows = mutableListOf<RankingMetric>()

        val crossTeamTypes =
            setOf(
                RankingMetricType.POWER_RATING,
                RankingMetricType.COLLEY_MATRIX,
                RankingMetricType.ASR,
                RankingMetricType.COMPOSITE,
                RankingMetricType.ADJUSTED_POINTS_FOR,
                RankingMetricType.ADJUSTED_POINTS_AGAINST,
                RankingMetricType.ADJUSTED_NET_POINTS,
            )
        val simpleTypes = RankingMetricType.values().filter { it.implemented && it !in crossTeamTypes && it !in DIFF_TYPES }
        val diffAggregates = buildDiffAggregates(season, week, teamsByName)
        val equivalentWins =
            aggregates.mapValues { (_, aggregate) ->
                rankingFormulas.calculateEquivalentWins(aggregate.pointsFor, aggregate.pointsAgainst, aggregate.gamesPlayed)
            }
        simpleTypes.forEach { type ->
            val values = aggregates.mapValues { (_, aggregate) -> calculate(type, aggregate) }
            values.forEach { (teamId, value) ->
                val aggregate = aggregates.getValue(teamId)
                rows.add(RankingMetric(season, week, type, teamId, value, aggregate.wins, aggregate.losses))
            }
        }

        val computedTypes = simpleTypes.map { it.name }.toMutableList()

        DIFF_TYPES.forEach { type ->
            diffAggregates.forEach { (teamId, diffAggregate) ->
                val aggregate = aggregates[teamId] ?: return@forEach
                rows.add(
                    RankingMetric(season, week, type, teamId, selectDiff(type, diffAggregate), aggregate.wins, aggregate.losses),
                )
            }
            computedTypes.add(type.name)
        }

        val powerRatings = powerRatingsCalculator.calculate(aggregates, diffAggregates)
        saveCrossTeamMetric(season, week, RankingMetricType.POWER_RATING, powerRatings, aggregates, computedTypes, rows)

        val colleyRatings = colleyRatingsCalculator.calculate(games, aggregates, teamsByName)
        saveCrossTeamMetric(season, week, RankingMetricType.COLLEY_MATRIX, colleyRatings, aggregates, computedTypes, rows)

        val asrRatings = asrRatingsCalculator.calculate(games, aggregates, teamsByName)
        saveCrossTeamMetric(season, week, RankingMetricType.ASR, asrRatings, aggregates, computedTypes, rows)

        val compositeRatings = compositeRatingsCalculator.calculate(colleyRatings, asrRatings, equivalentWins)
        saveCrossTeamMetric(season, week, RankingMetricType.COMPOSITE, compositeRatings, aggregates, computedTypes, rows)

        val adjustedPointsForRatings = adjustedPointsForRatingsCalculator.calculate(games, aggregates, teamsByName)
        saveCrossTeamMetric(season, week, RankingMetricType.ADJUSTED_POINTS_FOR, adjustedPointsForRatings, aggregates, computedTypes, rows)

        val adjustedPointsAgainstRatings = adjustedPointsAgainstRatingsCalculator.calculate(games, aggregates, teamsByName)
        saveCrossTeamMetric(
            season,
            week,
            RankingMetricType.ADJUSTED_POINTS_AGAINST,
            adjustedPointsAgainstRatings,
            aggregates,
            computedTypes,
            rows,
        )

        saveCrossTeamMetric(season, week, RankingMetricType.ADJUSTED_NET_POINTS, asrRatings, aggregates, computedTypes, rows)

        rankingMetricRepository.deleteBySeasonAndWeek(season, week)
        rankingMetricBatchRepository.batchInsert(rows)

        teamResumeMetricService.computeAndPersist(season, week, games, aggregates, teamsByName, compositeRatings)

        return RankingMetricComputeResult(season, week, computedTypes, aggregates.size)
    }

    private fun saveCrossTeamMetric(
        season: Int,
        week: Int,
        type: RankingMetricType,
        values: Map<Int, Double>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        computedTypes: MutableList<String>,
        rows: MutableList<RankingMetric>,
    ) {
        if (values.isEmpty()) return
        values.forEach { (teamId, value) ->
            val aggregate = aggregates[teamId]
            rows.add(RankingMetric(season, week, type, teamId, value, aggregate?.wins, aggregate?.losses))
        }
        computedTypes.add(type.name)
    }

    fun backfillSeason(season: Int): List<RankingMetricComputeResult> = getValidWeeks(season).map { week -> computeMetrics(season, week) }

    fun getValidWeeks(season: Int): List<Int> =
        gameRepository.getGamesBySeason(season)
            .filter { it.gameStatus == GameStatus.FINAL }
            .mapNotNull { it.week }
            .distinct()
            .sorted()

    private fun buildAggregates(
        games: List<Game>,
        teamsByName: Map<String, Team>,
    ): Map<Int, TeamSeasonAggregate> {
        val byTeamId = mutableMapOf<Int, TeamSeasonAccumulator>()

        fun record(
            teamName: String,
            pointsFor: Int,
            pointsAgainst: Int,
        ) {
            val team = teamsByName[teamName] ?: return
            val aggregate = byTeamId.getOrPut(team.id) { TeamSeasonAccumulator() }
            aggregate.gamesPlayed += 1
            aggregate.pointsFor += pointsFor
            aggregate.pointsAgainst += pointsAgainst
            aggregate.cappedPointDifferential += (pointsFor - pointsAgainst).coerceIn(-MARGIN_CAP, MARGIN_CAP)
            if (pointsFor > pointsAgainst) aggregate.wins += 1 else aggregate.losses += 1
        }

        games.forEach { game ->
            record(game.homeTeam, game.homeScore, game.awayScore)
            record(game.awayTeam, game.awayScore, game.homeScore)
        }

        return byTeamId.mapValues { (teamId, aggregate) ->
            TeamSeasonAggregate(
                teamId = teamId,
                gamesPlayed = aggregate.gamesPlayed,
                wins = aggregate.wins,
                losses = aggregate.losses,
                pointsFor = aggregate.pointsFor,
                pointsAgainst = aggregate.pointsAgainst,
                cappedPointDifferential = aggregate.cappedPointDifferential,
            )
        }
    }

    private fun buildDiffAggregates(
        season: Int,
        week: Int,
        teamsByName: Map<String, Team>,
    ): Map<Int, TeamDiffAggregate> {
        val byTeamId = mutableMapOf<Int, TeamDiffAccumulator>()
        gameStatsRepository.findBySeasonOrderByGameIdAsc(season)
            .filter { (it.week ?: Int.MAX_VALUE) <= week }
            .forEach { stats ->
                val teamName = stats.team ?: return@forEach
                val team = teamsByName[teamName] ?: return@forEach
                val offensiveDiff = stats.averageOffensiveDiff ?: return@forEach
                val defensiveDiff = stats.averageDefensiveDiff ?: return@forEach
                val offensiveSpecialTeamsDiff = stats.averageOffensiveSpecialTeamsDiff ?: return@forEach
                val defensiveSpecialTeamsDiff = stats.averageDefensiveSpecialTeamsDiff ?: return@forEach

                val aggregate = byTeamId.getOrPut(team.id) { TeamDiffAccumulator() }
                aggregate.count += 1
                aggregate.offensiveDiffSum += offensiveDiff
                aggregate.defensiveDiffSum += defensiveDiff
                aggregate.offensiveSpecialTeamsDiffSum += offensiveSpecialTeamsDiff
                aggregate.defensiveSpecialTeamsDiffSum += defensiveSpecialTeamsDiff
            }

        return byTeamId.filter { it.value.count > 0 }.mapValues { (_, aggregate) ->
            TeamDiffAggregate(
                averageOffensiveDiff = aggregate.offensiveDiffSum / aggregate.count,
                averageDefensiveDiff = aggregate.defensiveDiffSum / aggregate.count,
                averageOffensiveSpecialTeamsDiff = aggregate.offensiveSpecialTeamsDiffSum / aggregate.count,
                averageDefensiveSpecialTeamsDiff = aggregate.defensiveSpecialTeamsDiffSum / aggregate.count,
            )
        }
    }

    private fun selectDiff(
        type: RankingMetricType,
        diffAggregate: TeamDiffAggregate,
    ): Double =
        when (type) {
            RankingMetricType.AVERAGE_OFFENSIVE_DIFF -> diffAggregate.averageOffensiveDiff
            RankingMetricType.AVERAGE_DEFENSIVE_DIFF -> diffAggregate.averageDefensiveDiff
            RankingMetricType.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF -> diffAggregate.averageOffensiveSpecialTeamsDiff
            RankingMetricType.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF -> diffAggregate.averageDefensiveSpecialTeamsDiff
            else -> throw MetricNotImplementedException(type.name)
        }

    private fun calculate(
        type: RankingMetricType,
        aggregate: TeamSeasonAggregate,
    ): Double =
        when (type) {
            RankingMetricType.EQUIVALENT_WINS ->
                rankingFormulas.calculateEquivalentWins(aggregate.pointsFor, aggregate.pointsAgainst, aggregate.gamesPlayed)
            RankingMetricType.MARGIN_OF_VICTORY ->
                rankingFormulas.calculateMarginOfVictory(aggregate.pointsFor, aggregate.pointsAgainst, aggregate.gamesPlayed)
            RankingMetricType.SCORING_OFFENSE ->
                rankingFormulas.calculateScoringOffense(aggregate.pointsFor, aggregate.gamesPlayed)
            RankingMetricType.SCORING_DEFENSE ->
                rankingFormulas.calculateScoringDefense(aggregate.pointsAgainst, aggregate.gamesPlayed)
            else -> throw MetricNotImplementedException(type.name)
        }

    fun getMetrics(
        season: Int,
        week: Int,
        metricType: String,
    ): List<RankingMetricResponse> {
        val parsedType = parseMetricType(metricType)
        val rankingMetrics = rankingMetricRepository.findBySeasonWeekAndMetricType(season, week, parsedType.name)
        val teamNamesById = teamNamesById(rankingMetrics.map { it.teamId })
        return rankingMetrics.map { toResponse(it, parsedType, teamNamesById) }
    }

    fun getTeamMetricTrends(
        season: Int,
        teamId: Int,
    ): List<TeamRankingMetricTrendResponse> =
        rankingMetricRepository.findBySeason(season)
            .filter { it.metricType != null }
            .groupBy { it.metricType!! to it.week }
            .map { (key, rows) ->
                val (metricType, week) = key
                val sorted = rows.sortedBy { if (metricType.higherIsBetter) -it.value else it.value }
                val index = sorted.indexOfFirst { it.teamId == teamId }
                TeamRankingMetricTrendResponse(
                    metricType = metricType.name,
                    week = week,
                    rank = if (index >= 0) index + 1 else null,
                    total = sorted.size,
                )
            }

    fun getAvailableWeeks(
        season: Int,
        metricType: String,
    ): List<Int> = rankingMetricRepository.findWeeks(season, parseMetricType(metricType).name)

    fun getTeamMetrics(
        season: Int,
        week: Int,
        teamId: Int,
    ): List<TeamRankingMetricResponse> =
        rankingMetricRepository.findBySeasonAndWeek(season, week)
            .filter { it.metricType != null }
            .groupBy { it.metricType }
            .map { (metricType, rows) ->
                val sorted = rows.sortedBy { if (metricType!!.higherIsBetter) -it.value else it.value }
                val index = sorted.indexOfFirst { it.teamId == teamId }
                TeamRankingMetricResponse(
                    metricType = metricType!!.name,
                    value = if (index >= 0) sorted[index].value else null,
                    rank = if (index >= 0) index + 1 else null,
                    total = sorted.size,
                )
            }

    fun getMetricHistory(
        season: Int,
        team: String,
        metricType: String,
    ): List<RankingMetricResponse> {
        val parsedType = parseMetricType(metricType)
        val teamEntity = teamRepository.getTeamByName(team) ?: throw InvalidRankingMetricException("Unknown team: $team")
        val rankingMetrics = rankingMetricRepository.findByTeamAndMetricType(season, teamEntity.id, parsedType.name)
        return rankingMetrics.map { toResponse(it, parsedType, mapOf(teamEntity.id to teamEntity.name)) }
    }

    private fun teamNamesById(teamIds: Collection<Int>): Map<Int, String?> =
        teamRepository.findAllById(teamIds.distinct()).associate { it.id to it.name }

    private fun toResponse(
        rankingMetric: RankingMetric,
        metricType: RankingMetricType,
        teamNamesById: Map<Int, String?>,
    ): RankingMetricResponse =
        RankingMetricResponse(
            season = rankingMetric.season,
            week = rankingMetric.week,
            metricType = metricType.name,
            teamId = rankingMetric.teamId,
            teamName = teamNamesById[rankingMetric.teamId],
            value = rankingMetric.value,
            wins = rankingMetric.wins,
            losses = rankingMetric.losses,
        )

    private fun parseMetricType(metricType: String): RankingMetricType =
        RankingMetricType.fromString(metricType) ?: throw InvalidRankingMetricException("Unknown metric type: $metricType")

    companion object {
        val DIFF_TYPES =
            listOf(
                RankingMetricType.AVERAGE_OFFENSIVE_DIFF,
                RankingMetricType.AVERAGE_DEFENSIVE_DIFF,
                RankingMetricType.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF,
                RankingMetricType.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF,
            )
        const val MARGIN_CAP = 28
    }
}
