package com.fcfb.arceus.service.fcfb.statistics.league

import com.fcfb.arceus.domain.statistics.LeagueStatsCalculator
import com.fcfb.arceus.domain.statistics.PostseasonSeasonStatsMapper
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.statistics.LeagueStats
import com.fcfb.arceus.model.statistics.PostseasonLeagueStats
import com.fcfb.arceus.repositories.statistics.PostseasonLeagueStatsRepository
import com.fcfb.arceus.repositories.statistics.PostseasonSeasonStatsRepository
import com.fcfb.arceus.service.specification.PostseasonLeagueStatsSpecificationService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.statistics.StatsAverageCalculator
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostseasonLeagueStatsService(
    private val postseasonLeagueStatsRepository: PostseasonLeagueStatsRepository,
    private val postseasonSeasonStatsRepository: PostseasonSeasonStatsRepository,
    private val postseasonLeagueStatsSpecificationService: PostseasonLeagueStatsSpecificationService,
) {
    private val postseasonSeasonStatsMapper = PostseasonSeasonStatsMapper()
    private val leagueStatsCalculator = LeagueStatsCalculator(StatsAverageCalculator())

    fun getFilteredPostseasonLeagueStats(
        subdivision: Subdivision?,
        season: Int?,
        pageable: Pageable,
    ): Page<PostseasonLeagueStats> {
        val spec = postseasonLeagueStatsSpecificationService.createSpecification(subdivision, season)
        val sortOrders = postseasonLeagueStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return postseasonLeagueStatsRepository.findAll(spec, sortedPageable)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun generateAllPostseasonLeagueStats() {
        Logger().info("Starting generation of all postseason league stats")

        val allSeasonStats = postseasonSeasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()

        val groupedStats =
            allSeasonStats.groupBy {
                Pair(it.subdivision, it.seasonNumber)
            }.filterKeys { it.first != null }

        val total = groupedStats.size
        groupedStats.keys.forEachIndexed { index, subdivisionSeason ->
            val subdivision = subdivisionSeason.first!!
            val seasonNumber = subdivisionSeason.second

            Logger().info("Generating postseason league stats for $subdivision in season $seasonNumber (${index + 1}/$total)")
            generatePostseasonLeagueStatsForSubdivisionAndSeason(subdivision, seasonNumber)
        }

        Logger().info("Completed generation of all postseason league stats")
    }

    private fun generatePostseasonLeagueStatsForSubdivisionAndSeason(
        subdivision: Subdivision,
        seasonNumber: Int,
    ) {
        Logger().info("Starting generation of postseason league stats for $subdivision in season $seasonNumber")

        val seasonStatsList =
            postseasonSeasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
                .filter { seasonStats -> seasonStats.subdivision == subdivision }

        if (seasonStatsList.isEmpty()) {
            Logger().warn("No postseason season stats found for $subdivision in season $seasonNumber")
            return
        }

        val leagueStats =
            leagueStatsCalculator.aggregateSeasonStatsToLeagueStats(
                seasonStatsList.map { postseasonSeasonStatsMapper.toAggregationSeasonStats(it) },
                subdivision,
                seasonNumber,
            )
        val postseasonLeagueStats = leagueStats.toPostseason()
        postseasonLeagueStatsRepository.findBySubdivisionAndSeasonNumber(subdivision, seasonNumber)?.let {
            postseasonLeagueStats.id = it.id
        }

        postseasonLeagueStatsRepository.save(postseasonLeagueStats)
        Logger().info("Completed generating postseason league stats for $subdivision in season $seasonNumber")
    }
}

private fun LeagueStats.toPostseason(): PostseasonLeagueStats {
    return PostseasonLeagueStats(
        subdivision = subdivision,
        seasonNumber = seasonNumber,
        totalTeams = totalTeams,
        totalGames = totalGames,
        passAttempts = passAttempts,
        passCompletions = passCompletions,
        passCompletionPercentage = passCompletionPercentage,
        passYards = passYards,
        passTouchdowns = passTouchdowns,
        passInterceptions = passInterceptions,
        passSuccesses = passSuccesses,
        passSuccessPercentage = passSuccessPercentage,
        longestPass = longestPass,
        rushAttempts = rushAttempts,
        rushSuccesses = rushSuccesses,
        rushSuccessPercentage = rushSuccessPercentage,
        rushYards = rushYards,
        rushTouchdowns = rushTouchdowns,
        longestRun = longestRun,
        totalYards = totalYards,
        offensivePlayYards = offensivePlayYards,
        offensivePlayCount = offensivePlayCount,
        averageYardsPerPlay = averageYardsPerPlay,
        firstDowns = firstDowns,
        sacksAllowed = sacksAllowed,
        sacksForced = sacksForced,
        interceptionsForced = interceptionsForced,
        fumblesForced = fumblesForced,
        fumblesRecovered = fumblesRecovered,
        defensiveTouchdowns = defensiveTouchdowns,
        fieldGoalsAttempted = fieldGoalsAttempted,
        fieldGoalsMade = fieldGoalsMade,
        fieldGoalPercentage = fieldGoalPercentage,
        longestFieldGoal = longestFieldGoal,
        punts = punts,
        longestPunt = longestPunt,
        kickoffReturnTouchdowns = kickoffReturnTouchdowns,
        puntReturnTouchdowns = puntReturnTouchdowns,
        averageOffensiveDiff = averageOffensiveDiff,
        averageDefensiveDiff = averageDefensiveDiff,
        averageOffensiveSpecialTeamsDiff = averageOffensiveSpecialTeamsDiff,
        averageDefensiveSpecialTeamsDiff = averageDefensiveSpecialTeamsDiff,
        averageDiff = averageDiff,
        averageResponseSpeed = averageResponseSpeed,
        lastModifiedTs = lastModifiedTs,
    )
}
