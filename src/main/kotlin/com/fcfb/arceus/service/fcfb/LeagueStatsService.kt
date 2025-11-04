package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.LeagueStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.LeagueStatsRepository
import com.fcfb.arceus.repositories.SeasonStatsRepository
import com.fcfb.arceus.service.specification.LeagueStatsSpecificationService
import com.fcfb.arceus.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class LeagueStatsService(
    private val leagueStatsRepository: LeagueStatsRepository,
    private val seasonStatsRepository: SeasonStatsRepository,
    private val leagueStatsSpecificationService: LeagueStatsSpecificationService,
) {
    /**
     * Get filtered league stats with pagination
     */
    fun getFilteredLeagueStats(
        subdivision: Subdivision?,
        season: Int?,
        pageable: Pageable,
    ): Page<LeagueStats> {
        val spec = leagueStatsSpecificationService.createSpecification(subdivision, season)
        val sortOrders = leagueStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return leagueStatsRepository.findAll(spec, sortedPageable)
    }

    /**
     * Generate all league stats (recalculate all league stats)
     */
    fun generateAllLeagueStats() {
        Logger.info("Starting generation of all league stats")

        // Get all season stats
        val allSeasonStats = seasonStatsRepository.findAllByOrderBySeasonNumberDescTeamAsc()

        // Group by subdivision and season
        val groupedStats =
            allSeasonStats.groupBy {
                Pair(it.subdivision, it.seasonNumber)
            }.filterKeys { it.first != null }

        // Generate league stats for each subdivision/season combination
        for ((subdivisionSeason) in groupedStats) {
            val subdivision = subdivisionSeason.first!!
            val seasonNumber = subdivisionSeason.second

            Logger.info("Generating league stats for $subdivision in season $seasonNumber")
            generateLeagueStatsForSubdivisionAndSeason(subdivision, seasonNumber)
        }

        Logger.info("Completed generation of all league stats")
    }

    /**
     * Generate league stats for a specific subdivision and season
     */
    private fun generateLeagueStatsForSubdivisionAndSeason(
        subdivision: Subdivision,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of league stats for $subdivision in season $seasonNumber")

        // Get all season stats for this subdivision and season
        val seasonStatsList =
            seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(seasonNumber)
                .filter { seasonStats -> seasonStats.subdivision == subdivision }

        if (seasonStatsList.isEmpty()) {
            Logger.warn("No season stats found for $subdivision in season $seasonNumber")
            return
        }

        // Delete existing league stats for this subdivision and season
        leagueStatsRepository.findBySubdivisionAndSeasonNumber(subdivision, seasonNumber)?.let {
            leagueStatsRepository.delete(it)
        }

        // Create new league stats
        val leagueStats = aggregateSeasonStatsToLeagueStats(seasonStatsList, subdivision, seasonNumber)

        leagueStatsRepository.save(leagueStats)
        Logger.info("Completed generating league stats for $subdivision in season $seasonNumber")
    }

    /**
     * Aggregate season stats into league stats
     */
    private fun aggregateSeasonStatsToLeagueStats(
        seasonStatsList: List<SeasonStats>,
        subdivision: Subdivision,
        seasonNumber: Int,
    ): LeagueStats {
        val totalTeams = seasonStatsList.size
        val totalGames = seasonStatsList.sumOf { it.wins + it.losses }

        return LeagueStats(
            subdivision = subdivision,
            seasonNumber = seasonNumber,
            totalTeams = totalTeams,
            totalGames = totalGames,
            // Aggregate all the stats
            passAttempts = seasonStatsList.sumOf { it.passAttempts },
            passCompletions = seasonStatsList.sumOf { it.passCompletions },
            passCompletionPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.passCompletions },
                    seasonStatsList.sumOf { it.passAttempts },
                ),
            passYards = seasonStatsList.sumOf { it.passYards },
            passTouchdowns = seasonStatsList.sumOf { it.passTouchdowns },
            passInterceptions = seasonStatsList.sumOf { it.interceptionsLost },
            passSuccesses = seasonStatsList.sumOf { it.passSuccesses },
            passSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.passSuccesses },
                    seasonStatsList.sumOf { it.passAttempts },
                ),
            longestPass = seasonStatsList.maxOfOrNull { it.longestPass } ?: 0,
            rushAttempts = seasonStatsList.sumOf { it.rushAttempts },
            rushSuccesses = seasonStatsList.sumOf { it.rushSuccesses },
            rushSuccessPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.rushSuccesses },
                    seasonStatsList.sumOf { it.rushAttempts },
                ),
            rushYards = seasonStatsList.sumOf { it.rushYards },
            rushTouchdowns = seasonStatsList.sumOf { it.rushTouchdowns },
            longestRun = seasonStatsList.maxOfOrNull { it.longestRun } ?: 0,
            totalYards = seasonStatsList.sumOf { it.totalYards },
            averageYardsPerPlay = calculateAverage(seasonStatsList.mapNotNull { it.averageYardsPerPlay }) ?: 0.0,
            firstDowns = seasonStatsList.sumOf { it.firstDowns },
            sacksAllowed = seasonStatsList.sumOf { it.sacksAllowed },
            sacksForced = seasonStatsList.sumOf { it.sacksForced },
            interceptionsForced = seasonStatsList.sumOf { it.interceptionsForced },
            fumblesForced = seasonStatsList.sumOf { it.fumblesForced },
            fumblesRecovered = seasonStatsList.sumOf { it.fumblesForced },
            defensiveTouchdowns = seasonStatsList.sumOf { it.turnoverTouchdownsForced },
            fieldGoalsAttempted = seasonStatsList.sumOf { it.fieldGoalAttempts },
            fieldGoalsMade = seasonStatsList.sumOf { it.fieldGoalMade },
            fieldGoalPercentage =
                calculatePercentage(
                    seasonStatsList.sumOf { it.fieldGoalMade },
                    seasonStatsList.sumOf { it.fieldGoalAttempts },
                ),
            longestFieldGoal = seasonStatsList.maxOfOrNull { it.longestFieldGoal } ?: 0,
            punts = seasonStatsList.sumOf { it.puntsAttempted },
            longestPunt = seasonStatsList.maxOfOrNull { it.longestPunt } ?: 0,
            kickoffReturnTouchdowns = seasonStatsList.sumOf { it.kickReturnTd },
            puntReturnTouchdowns = seasonStatsList.sumOf { it.puntReturnTd },
            // Performance metrics are averages of team averages
            averageOffensiveDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageOffensiveDiff }) ?: 0.0,
            averageDefensiveDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDefensiveDiff }) ?: 0.0,
            averageOffensiveSpecialTeamsDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }) ?: 0.0,
            averageDefensiveSpecialTeamsDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }) ?: 0.0,
            averageDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDiff }) ?: 0.0,
            averageResponseSpeed = calculateAverage(seasonStatsList.mapNotNull { it.averageResponseSpeed }) ?: 0.0,
            lastModifiedTs = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT),
        )
    }

    /**
     * Calculate percentage from numerator and denominator
     */
    private fun calculatePercentage(
        numerator: Int,
        denominator: Int,
    ): Double {
        return if (denominator > 0) {
            (numerator.toDouble() / denominator.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * Calculate average from a list of values
     */
    private fun calculateAverage(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        return values.average()
    }
}
