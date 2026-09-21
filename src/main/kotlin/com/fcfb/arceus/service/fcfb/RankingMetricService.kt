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
        val cappedPointDifferential: Int,
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
        val simpleTypes = RankingMetricType.values().filter { it.implemented && it !in crossTeamTypes && it !in DIFF_TYPES }
        val diffAggregates = buildDiffAggregates(season, week, teamsByName)
        val equivalentWins =
            aggregates.mapValues { (_, aggregate) ->
                calculateEquivalentWins(aggregate.pointsFor, aggregate.pointsAgainst, aggregate.gamesPlayed)
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

        val powerRatings = calculatePowerRatings(aggregates, diffAggregates)
        saveCrossTeamMetric(season, week, RankingMetricType.POWER_RATING, powerRatings, aggregates, computedTypes, rows)

        val colleyRatings = calculateColleyRatings(games, aggregates, teamsByName)
        saveCrossTeamMetric(season, week, RankingMetricType.COLLEY_MATRIX, colleyRatings, aggregates, computedTypes, rows)

        val asrRatings = calculateAsrRatings(games, aggregates, teamsByName)
        saveCrossTeamMetric(season, week, RankingMetricType.ASR, asrRatings, aggregates, computedTypes, rows)

        val compositeRatings = calculateCompositeRatings(colleyRatings, asrRatings, equivalentWins)
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
            var cappedPointDifferential: Int = 0,
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

    private fun calculatePowerRatings(
        aggregates: Map<Int, TeamSeasonAggregate>,
        diffAggregates: Map<Int, TeamDiffAggregate>,
    ): Map<Int, Double> {
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
            calculateCappedMarginOfVictory(it.cappedPointDifferential, it.gamesPlayed)
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

    /**
     * Blends the three independent signals rather than averaging every metric: averaging them all weights
     * scoring margin five times over, since equivalent wins, margin of victory, scoring offense, scoring
     * defense and ASR are all functions of the same points for and points against.
     */
    private fun calculateCompositeRatings(
        colleyRatings: Map<Int, Double>,
        asrRatings: Map<Int, Double>,
        equivalentWins: Map<Int, Double>,
    ): Map<Int, Double> {
        val teamIds = colleyRatings.keys + asrRatings.keys + equivalentWins.keys
        if (teamIds.isEmpty()) return emptyMap()

        val normalizedColley = normalize(colleyRatings)
        val normalizedAsr = normalize(asrRatings)
        val normalizedEquivalentWins = normalize(equivalentWins)

        return teamIds.associateWith { teamId ->
            COMPOSITE_COLLEY_WEIGHT * (normalizedColley[teamId] ?: 0.0) +
                COMPOSITE_ASR_WEIGHT * (normalizedAsr[teamId] ?: 0.0) +
                COMPOSITE_EQUIVALENT_WINS_WEIGHT * (normalizedEquivalentWins[teamId] ?: 0.0)
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

    fun calculateCappedMarginOfVictory(
        cappedPointDifferential: Int,
        gamesPlayed: Int,
    ): Double = if (gamesPlayed == 0) 0.0 else cappedPointDifferential.toDouble() / gamesPlayed

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
        const val NORMAL_PLAY_DIFF_WEIGHT = 0.95
        const val SPECIAL_TEAMS_DIFF_WEIGHT = 0.05
        const val POWER_RATING_BASE_WEIGHT = 0.8
        const val POWER_RATING_DIFFERENTIAL_WEIGHT = 0.2
        val DIFF_TYPES =
            listOf(
                RankingMetricType.AVERAGE_OFFENSIVE_DIFF,
                RankingMetricType.AVERAGE_DEFENSIVE_DIFF,
                RankingMetricType.AVERAGE_OFFENSIVE_SPECIAL_TEAMS_DIFF,
                RankingMetricType.AVERAGE_DEFENSIVE_SPECIAL_TEAMS_DIFF,
            )
        const val ASR_REGULARIZATION = 0.1
        const val MARGIN_CAP = 28
        const val COMPOSITE_COLLEY_WEIGHT = 0.45
        const val COMPOSITE_ASR_WEIGHT = 0.40
        const val COMPOSITE_EQUIVALENT_WINS_WEIGHT = 0.15
    }
}
