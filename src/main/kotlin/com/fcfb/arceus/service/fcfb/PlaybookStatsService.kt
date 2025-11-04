package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.PlaybookStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlaybookStatsRepository
import com.fcfb.arceus.service.specification.PlaybookStatsSpecificationService
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
class PlaybookStatsService(
    private val playbookStatsRepository: PlaybookStatsRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val playbookStatsSpecificationService: PlaybookStatsSpecificationService,
) {
    /**
     * Get filtered playbook stats with pagination
     */
    fun getFilteredPlaybookStats(
        offensivePlaybook: OffensivePlaybook?,
        defensivePlaybook: DefensivePlaybook?,
        season: Int?,
        pageable: Pageable,
    ): Page<PlaybookStats> {
        val spec = playbookStatsSpecificationService.createSpecification(offensivePlaybook, defensivePlaybook, season)
        val sortOrders = playbookStatsSpecificationService.createSort()
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )
        return playbookStatsRepository.findAll(spec, sortedPageable)
    }

    /**
     * Generate all playbook stats (recalculate all playbook stats)
     */
    fun generateAllPlaybookStats() {
        Logger.info("Starting generation of all playbook stats")

        // Get all game stats
        val allGameStats = gameStatsRepository.findAllByOrderBySeasonDescGameIdAsc()
        Logger.info("Found ${allGameStats.size} total game stats records")

        // Group by offensive playbook, defensive playbook, and season
        val groupedStats =
            allGameStats.groupBy {
                Triple(it.offensivePlaybook, it.defensivePlaybook, it.season)
            }.filterKeys { it.first != null && it.second != null }

        Logger.info("Found ${groupedStats.size} valid offensive/defensive playbook/season combinations")

        // Generate playbook stats for each offensive/defensive playbook/season combination
        for ((playbookSeason, gameStatsList) in groupedStats) {
            val offensivePlaybook = playbookSeason.first!!
            val defensivePlaybook = playbookSeason.second!!
            val seasonNumber = playbookSeason.third

            if (seasonNumber != null) {
                Logger.info(
                    "Generating playbook stats for $offensivePlaybook/$defensivePlaybook " +
                        "in season $seasonNumber with ${gameStatsList.size} games",
                )
                generateByPlaybooksAndSeason(
                    offensivePlaybook,
                    defensivePlaybook,
                    seasonNumber,
                )
            }
        }

        Logger.info("Completed generation of all playbook stats")
    }

    /**
     * Generate playbook stats for a specific offensive playbook, defensive playbook, and season
     */
    private fun generateByPlaybooksAndSeason(
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of playbook stats for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")

        // Get all game stats for this offensive playbook, defensive playbook, and season
        val gameStatsList =
            gameStatsRepository.findBySeasonOrderByGameIdAsc(seasonNumber)
                .filter { gameStats ->
                    gameStats.offensivePlaybook == offensivePlaybook && gameStats.defensivePlaybook == defensivePlaybook
                }

        if (gameStatsList.isEmpty()) {
            Logger.warn("No game stats found for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")
            return
        }

        // Delete existing playbook stats for this offensive playbook, defensive playbook, and season
        playbookStatsRepository.findByOffensivePlaybookAndDefensivePlaybookAndSeasonNumber(
            offensivePlaybook,
            defensivePlaybook,
            seasonNumber,
        )?.let {
            playbookStatsRepository.delete(it)
        }

        // Create new playbook stats
        val playbookStats = aggregateGameStatsToPlaybookStats(gameStatsList, offensivePlaybook, defensivePlaybook, seasonNumber)

        playbookStatsRepository.save(playbookStats)
        Logger.info("Completed generating playbook stats for $offensivePlaybook/$defensivePlaybook in season $seasonNumber")
    }

    /**
     * Aggregate game stats into playbook stats
     */
    private fun aggregateGameStatsToPlaybookStats(
        gameStatsList: List<GameStats>,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ): PlaybookStats {
        val totalTeams = gameStatsList.map { it.team }.distinct().size
        val totalGames = gameStatsList.size

        return PlaybookStats(
            offensivePlaybook = offensivePlaybook,
            defensivePlaybook = defensivePlaybook,
            seasonNumber = seasonNumber,
            totalTeams = totalTeams,
            totalGames = totalGames,
            // Aggregate all the stats
            passAttempts = gameStatsList.sumOf { it.passAttempts },
            passCompletions = gameStatsList.sumOf { it.passCompletions },
            passCompletionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.passCompletions },
                    gameStatsList.sumOf { it.passAttempts },
                ),
            passYards = gameStatsList.sumOf { it.passYards },
            passTouchdowns = gameStatsList.sumOf { it.passTouchdowns },
            passInterceptions = gameStatsList.sumOf { it.interceptionsLost },
            passSuccesses = gameStatsList.sumOf { it.passSuccesses },
            passSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.passSuccesses },
                    gameStatsList.sumOf { it.passAttempts },
                ),
            longestPass = gameStatsList.maxOfOrNull { it.longestPass } ?: 0,
            rushAttempts = gameStatsList.sumOf { it.rushAttempts },
            rushSuccesses = gameStatsList.sumOf { it.rushSuccesses },
            rushSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.rushSuccesses },
                    gameStatsList.sumOf { it.rushAttempts },
                ),
            rushYards = gameStatsList.sumOf { it.rushYards },
            rushTouchdowns = gameStatsList.sumOf { it.rushTouchdowns },
            longestRun = gameStatsList.maxOfOrNull { it.longestRun } ?: 0,
            totalYards = gameStatsList.sumOf { it.totalYards },
            averageYardsPerPlay = calculateAverage(gameStatsList.mapNotNull { it.averageYardsPerPlay }) ?: 0.0,
            firstDowns = gameStatsList.sumOf { it.firstDowns },
            sacksAllowed = gameStatsList.sumOf { it.sacksAllowed },
            sacksForced = gameStatsList.sumOf { it.sacksForced },
            interceptionsForced = gameStatsList.sumOf { it.interceptionsForced },
            fumblesForced = gameStatsList.sumOf { it.fumblesForced },
            fumblesRecovered = gameStatsList.sumOf { it.fumblesForced },
            defensiveTouchdowns = gameStatsList.sumOf { it.turnoverTouchdownsForced },
            fieldGoalsAttempted = gameStatsList.sumOf { it.fieldGoalAttempts },
            fieldGoalsMade = gameStatsList.sumOf { it.fieldGoalMade },
            fieldGoalPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.fieldGoalMade },
                    gameStatsList.sumOf { it.fieldGoalAttempts },
                ),
            longestFieldGoal = gameStatsList.maxOfOrNull { it.longestFieldGoal } ?: 0,
            punts = gameStatsList.sumOf { it.puntsAttempted },
            longestPunt = gameStatsList.maxOfOrNull { it.longestPunt } ?: 0,
            kickoffReturnTouchdowns = gameStatsList.sumOf { it.kickReturnTd },
            puntReturnTouchdowns = gameStatsList.sumOf { it.puntReturnTd },
            // Performance metrics are averages of individual game values
            averageOffensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveDiff }) ?: 0.0,
            averageDefensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveDiff }) ?: 0.0,
            averageOffensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }) ?: 0.0,
            averageDefensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }) ?: 0.0,
            averageDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDiff }) ?: 0.0,
            averageResponseSpeed = calculateAverage(gameStatsList.mapNotNull { it.averageResponseSpeed }) ?: 0.0,
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
