package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.response.RankingMetricComputeResult
import com.fcfb.arceus.dto.response.RankingMetricResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.ranking.RankingMetricType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.RankingMetric
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RankingMetricBatchRepository
import com.fcfb.arceus.repositories.RankingMetricRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.util.InvalidRankingMetricException
import com.fcfb.arceus.util.LinearAlgebraUtils
import com.fcfb.arceus.util.MetricNotImplementedException
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
    internal data class TeamSeasonAggregate(
        val teamId: Int,
        val gamesPlayed: Int,
        val wins: Int,
        val losses: Int,
        val pointsFor: Int,
        val pointsAgainst: Int,
    )

    private data class TeamDiffAggregate(
        val averageOffensiveDiff: Double,
        val averageDefensiveDiff: Double,
        val averageOffensiveSpecialTeamsDiff: Double,
        val averageDefensiveSpecialTeamsDiff: Double,
    )

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
        val simpleTypes = RankingMetricType.values().filter { it.implemented && it !in crossTeamTypes }
        val metricValuesByType = mutableMapOf<RankingMetricType, Map<Int, Double>>()
        simpleTypes.forEach { type ->
            val values = aggregates.mapValues { (_, aggregate) -> calculate(type, aggregate) }
            values.forEach { (teamId, value) ->
                val aggregate = aggregates.getValue(teamId)
                rows.add(RankingMetric(season, week, type, teamId, value, aggregate.wins, aggregate.losses))
            }
            metricValuesByType[type] = values
        }

        val computedTypes = simpleTypes.map { it.name }.toMutableList()

        val powerRatings = calculatePowerRatings(season, week, aggregates, teamsByName)
        metricValuesByType[RankingMetricType.POWER_RATING] = powerRatings
        saveCrossTeamMetric(season, week, RankingMetricType.POWER_RATING, powerRatings, aggregates, computedTypes, rows)

        val colleyRatings = calculateColleyRatings(games, aggregates, teamsByName)
        metricValuesByType[RankingMetricType.COLLEY_MATRIX] = colleyRatings
        saveCrossTeamMetric(season, week, RankingMetricType.COLLEY_MATRIX, colleyRatings, aggregates, computedTypes, rows)

        val asrRatings = calculateAsrRatings(games, aggregates, teamsByName)
        metricValuesByType[RankingMetricType.ASR] = asrRatings
        saveCrossTeamMetric(season, week, RankingMetricType.ASR, asrRatings, aggregates, computedTypes, rows)

        val compositeRatings = calculateCompositeRatings(metricValuesByType)
        saveCrossTeamMetric(season, week, RankingMetricType.COMPOSITE, compositeRatings, aggregates, computedTypes, rows)

        val adjustedPointsForRatings = calculateAdjustedPointsForRatings(games, aggregates, teamsByName)
        saveCrossTeamMetric(season, week, RankingMetricType.ADJUSTED_POINTS_FOR, adjustedPointsForRatings, aggregates, computedTypes, rows)

        val adjustedPointsAgainstRatings = calculateAdjustedPointsAgainstRatings(games, aggregates, teamsByName)
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
        data class MutableAggregate(
            var gamesPlayed: Int = 0,
            var wins: Int = 0,
            var losses: Int = 0,
            var pointsFor: Int = 0,
            var pointsAgainst: Int = 0,
        )

        val byTeamId = mutableMapOf<Int, MutableAggregate>()

        fun record(
            teamName: String,
            pointsFor: Int,
            pointsAgainst: Int,
        ) {
            val team = teamsByName[teamName] ?: return
            val aggregate = byTeamId.getOrPut(team.id) { MutableAggregate() }
            aggregate.gamesPlayed += 1
            aggregate.pointsFor += pointsFor
            aggregate.pointsAgainst += pointsAgainst
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
            )
        }
    }

    private fun buildDiffAggregates(
        season: Int,
        week: Int,
        teamsByName: Map<String, Team>,
    ): Map<Int, TeamDiffAggregate> {
        data class MutableDiffAggregate(
            var count: Int = 0,
            var offensiveDiffSum: Double = 0.0,
            var defensiveDiffSum: Double = 0.0,
            var offensiveSpecialTeamsDiffSum: Double = 0.0,
            var defensiveSpecialTeamsDiffSum: Double = 0.0,
        )

        val byTeamId = mutableMapOf<Int, MutableDiffAggregate>()
        gameStatsRepository.findBySeasonOrderByGameIdAsc(season)
            .filter { (it.week ?: Int.MAX_VALUE) <= week }
            .forEach { stats ->
                val teamName = stats.team ?: return@forEach
                val team = teamsByName[teamName] ?: return@forEach
                val offensiveDiff = stats.averageOffensiveDiff ?: return@forEach
                val defensiveDiff = stats.averageDefensiveDiff ?: return@forEach
                val offensiveSpecialTeamsDiff = stats.averageOffensiveSpecialTeamsDiff ?: return@forEach
                val defensiveSpecialTeamsDiff = stats.averageDefensiveSpecialTeamsDiff ?: return@forEach

                val aggregate = byTeamId.getOrPut(team.id) { MutableDiffAggregate() }
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

    private fun calculatePowerRatings(
        season: Int,
        week: Int,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
    ): Map<Int, Double> {
        val diffAggregates = buildDiffAggregates(season, week, teamsByName)
        val teamIds = aggregates.keys.intersect(diffAggregates.keys)
        if (teamIds.isEmpty()) return emptyMap()

        val winPct =
            normalize(
                teamIds.associateWith { teamId ->
                    val aggregate = aggregates.getValue(teamId)
                    if (aggregate.gamesPlayed == 0) 0.0 else aggregate.wins.toDouble() / aggregate.gamesPlayed
                },
            )
        val equivalentWins =
            normalize(
                teamIds.associateWith { teamId ->
                    val aggregate = aggregates.getValue(teamId)
                    calculateEquivalentWins(aggregate.pointsFor, aggregate.pointsAgainst, aggregate.gamesPlayed)
                },
            )

        // Lower average difference is better for offense (harder for the defense to read); higher is better for defense
        // (this team read its opponents better). Same convention applies to the special-teams variants.
        val offensiveDiff = normalize(teamIds.associateWith { -diffAggregates.getValue(it).averageOffensiveDiff })
        val defensiveDiff = normalize(teamIds.associateWith { diffAggregates.getValue(it).averageDefensiveDiff })
        val offensiveSpecialTeamsDiff = normalize(teamIds.associateWith { -diffAggregates.getValue(it).averageOffensiveSpecialTeamsDiff })
        val defensiveSpecialTeamsDiff = normalize(teamIds.associateWith { diffAggregates.getValue(it).averageDefensiveSpecialTeamsDiff })

        return teamIds.associateWith { teamId ->
            val base = BASE_EQUIVALENT_WINS_WEIGHT * equivalentWins.getValue(teamId) + BASE_WIN_PCT_WEIGHT * winPct.getValue(teamId)
            val normalPlayDiff = (offensiveDiff.getValue(teamId) + defensiveDiff.getValue(teamId)) / 2
            val specialTeamsDiff = (offensiveSpecialTeamsDiff.getValue(teamId) + defensiveSpecialTeamsDiff.getValue(teamId)) / 2
            val differential = NORMAL_PLAY_DIFF_WEIGHT * normalPlayDiff + SPECIAL_TEAMS_DIFF_WEIGHT * specialTeamsDiff
            POWER_RATING_BASE_WEIGHT * base + POWER_RATING_DIFFERENTIAL_WEIGHT * differential
        }
    }

    private fun calculateColleyRatings(
        games: List<Game>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
    ): Map<Int, Double> {
        val teamIds = aggregates.keys.sorted()
        val n = teamIds.size
        if (n == 0) return emptyMap()
        val indexOfTeamId = teamIds.withIndex().associate { (index, teamId) -> teamId to index }

        val gamesBetween = Array(n) { IntArray(n) }
        games.forEach { game ->
            val homeTeamId = teamsByName[game.homeTeam]?.id
            val awayTeamId = teamsByName[game.awayTeam]?.id
            val homeIndex = indexOfTeamId[homeTeamId]
            val awayIndex = indexOfTeamId[awayTeamId]
            if (homeIndex != null && awayIndex != null) {
                gamesBetween[homeIndex][awayIndex] += 1
                gamesBetween[awayIndex][homeIndex] += 1
            }
        }

        val matrix =
            Array(n) { row ->
                DoubleArray(n) { col ->
                    if (row == col) {
                        2.0 + aggregates.getValue(teamIds[row]).gamesPlayed
                    } else {
                        -gamesBetween[row][col].toDouble()
                    }
                }
            }
        val b =
            DoubleArray(n) { row ->
                val aggregate = aggregates.getValue(teamIds[row])
                1.0 + (aggregate.wins - aggregate.losses) / 2.0
            }

        val ratings = LinearAlgebraUtils.solve(matrix, b)
        return teamIds.indices.associate { index -> teamIds[index] to ratings[index] }
    }

    /**
     * SRS-style rating (r_i - avg(opponent ratings) = perGameValue_i), same family as Sagarin/Massey.
     * Ridge term on the diagonal keeps the system solvable on a sparse/disconnected early-season schedule.
     * Shared by ASR (net margin) and the aPPf/aPPa splits (points-for-only / points-against-only).
     */
    private fun calculateAdjustedScheduleRatings(
        games: List<Game>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
        perGameValue: (TeamSeasonAggregate) -> Double,
    ): Map<Int, Double> {
        val teamIds = aggregates.keys.sorted()
        val n = teamIds.size
        if (n == 0) return emptyMap()
        val indexOfTeamId = teamIds.withIndex().associate { (index, teamId) -> teamId to index }

        val gamesBetween = Array(n) { IntArray(n) }
        games.forEach { game ->
            val homeIndex = indexOfTeamId[teamsByName[game.homeTeam]?.id]
            val awayIndex = indexOfTeamId[teamsByName[game.awayTeam]?.id]
            if (homeIndex != null && awayIndex != null) {
                gamesBetween[homeIndex][awayIndex] += 1
                gamesBetween[awayIndex][homeIndex] += 1
            }
        }

        val matrix =
            Array(n) { row ->
                val gamesPlayed = aggregates.getValue(teamIds[row]).gamesPlayed
                DoubleArray(n) { col ->
                    if (row == col) 1.0 + ASR_REGULARIZATION else -(gamesBetween[row][col].toDouble() / gamesPlayed)
                }
            }
        val b = DoubleArray(n) { row -> perGameValue(aggregates.getValue(teamIds[row])) }

        val ratings = LinearAlgebraUtils.solve(matrix, b)
        return teamIds.indices.associate { index -> teamIds[index] to ratings[index] }
    }

    private fun calculateAsrRatings(
        games: List<Game>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
    ): Map<Int, Double> =
        calculateAdjustedScheduleRatings(games, aggregates, teamsByName) {
            calculateMarginOfVictory(it.pointsFor, it.pointsAgainst, it.gamesPlayed)
        }

    private fun calculateAdjustedPointsForRatings(
        games: List<Game>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
    ): Map<Int, Double> =
        calculateAdjustedScheduleRatings(games, aggregates, teamsByName) {
            calculateScoringOffense(it.pointsFor, it.gamesPlayed)
        }

    private fun calculateAdjustedPointsAgainstRatings(
        games: List<Game>,
        aggregates: Map<Int, TeamSeasonAggregate>,
        teamsByName: Map<String, Team>,
    ): Map<Int, Double> =
        calculateAdjustedScheduleRatings(games, aggregates, teamsByName) {
            calculateScoringDefense(it.pointsAgainst, it.gamesPlayed)
        }

    /** Poll-of-polls, mirroring the BCS composite: average of every other metric normalized to 0-100 first so raw units don't dominate. */
    private fun calculateCompositeRatings(metricValuesByType: Map<RankingMetricType, Map<Int, Double>>): Map<Int, Double> {
        val teamIds = metricValuesByType.values.flatMap { it.keys }.toSet()
        if (teamIds.isEmpty()) return emptyMap()

        val normalizedByType =
            metricValuesByType.map { (type, values) ->
                val directed = if (type.higherIsBetter) values else values.mapValues { -it.value }
                normalize(directed)
            }

        return teamIds.associateWith { teamId ->
            val scores = normalizedByType.mapNotNull { it[teamId] }
            if (scores.isEmpty()) 0.0 else scores.sum() / scores.size
        }
    }

    private fun normalize(valuesByTeamId: Map<Int, Double>): Map<Int, Double> {
        if (valuesByTeamId.isEmpty()) return emptyMap()
        val min = valuesByTeamId.values.min()
        val max = valuesByTeamId.values.max()
        if (min == max) return valuesByTeamId.mapValues { 50.0 }
        return valuesByTeamId.mapValues { (_, value) -> (value - min) / (max - min) * 100 }
    }

    private fun calculate(
        type: RankingMetricType,
        aggregate: TeamSeasonAggregate,
    ): Double =
        when (type) {
            RankingMetricType.EQUIVALENT_WINS ->
                calculateEquivalentWins(aggregate.pointsFor, aggregate.pointsAgainst, aggregate.gamesPlayed)
            RankingMetricType.MARGIN_OF_VICTORY ->
                calculateMarginOfVictory(aggregate.pointsFor, aggregate.pointsAgainst, aggregate.gamesPlayed)
            RankingMetricType.SCORING_OFFENSE ->
                calculateScoringOffense(aggregate.pointsFor, aggregate.gamesPlayed)
            RankingMetricType.SCORING_DEFENSE ->
                calculateScoringDefense(aggregate.pointsAgainst, aggregate.gamesPlayed)
            else -> throw MetricNotImplementedException(type.name)
        }

    fun calculateMarginOfVictory(
        pointsFor: Int,
        pointsAgainst: Int,
        gamesPlayed: Int,
    ): Double = if (gamesPlayed == 0) 0.0 else (pointsFor - pointsAgainst).toDouble() / gamesPlayed

    fun calculateScoringOffense(
        pointsFor: Int,
        gamesPlayed: Int,
    ): Double = if (gamesPlayed == 0) 0.0 else pointsFor.toDouble() / gamesPlayed

    fun calculateScoringDefense(
        pointsAgainst: Int,
        gamesPlayed: Int,
    ): Double = if (gamesPlayed == 0) 0.0 else pointsAgainst.toDouble() / gamesPlayed

    fun calculateEquivalentWins(
        pointsFor: Int,
        pointsAgainst: Int,
        gamesPlayed: Int,
    ): Double {
        if (gamesPlayed == 0 || (pointsFor == 0 && pointsAgainst == 0)) return 0.0
        val pointsForExp = Math.pow(pointsFor.toDouble(), PYTHAGOREAN_EXPONENT)
        val pointsAgainstExp = Math.pow(pointsAgainst.toDouble(), PYTHAGOREAN_EXPONENT)
        return gamesPlayed * (pointsForExp / (pointsForExp + pointsAgainstExp))
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

    fun getAvailableWeeks(
        season: Int,
        metricType: String,
    ): List<Int> = rankingMetricRepository.findWeeks(season, parseMetricType(metricType).name)

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
        const val PYTHAGOREAN_EXPONENT = 2.37
        const val BASE_EQUIVALENT_WINS_WEIGHT = 0.65
        const val BASE_WIN_PCT_WEIGHT = 0.35
        const val NORMAL_PLAY_DIFF_WEIGHT = 0.8
        const val SPECIAL_TEAMS_DIFF_WEIGHT = 0.2
        const val POWER_RATING_BASE_WEIGHT = 0.9
        const val POWER_RATING_DIFFERENTIAL_WEIGHT = 0.1
        const val ASR_REGULARIZATION = 0.1
    }
}
