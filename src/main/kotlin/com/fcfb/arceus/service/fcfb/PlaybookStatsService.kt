package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.PlaybookStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlaybookStatsRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class PlaybookStatsService(
    private val playbookStatsRepository: PlaybookStatsRepository,
    private val gameStatsRepository: GameStatsRepository,
) {
    /**
     * Get all playbook stats
     */
    fun getAllPlaybookStats(): List<PlaybookStats> {
        return playbookStatsRepository.findAllByOrderBySeasonNumberDescOffensivePlaybookAscDefensivePlaybookAsc()
    }

    /**
     * Get playbook stats for a specific offensive playbook, defensive playbook, and season
     */
    fun getPlaybookStatsByOffensivePlaybookAndDefensivePlaybookAndSeason(
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ): PlaybookStats? {
        return playbookStatsRepository.findByOffensivePlaybookAndDefensivePlaybookAndSeasonNumber(
            offensivePlaybook,
            defensivePlaybook,
            seasonNumber,
        )
    }

    /**
     * Get all playbook stats for a specific offensive playbook and season
     */
    fun getPlaybookStatsByOffensivePlaybookAndSeason(
        offensivePlaybook: OffensivePlaybook,
        seasonNumber: Int,
    ): List<PlaybookStats> {
        return playbookStatsRepository.findByOffensivePlaybookAndSeasonNumber(offensivePlaybook, seasonNumber)
    }

    /**
     * Get all playbook stats for a specific defensive playbook and season
     */
    fun getPlaybookStatsByDefensivePlaybookAndSeason(
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ): List<PlaybookStats> {
        return playbookStatsRepository.findByDefensivePlaybookAndSeasonNumber(defensivePlaybook, seasonNumber)
    }

    /**
     * Get all playbook stats for a specific offensive playbook
     */
    fun getPlaybookStatsByOffensivePlaybook(offensivePlaybook: OffensivePlaybook): List<PlaybookStats> {
        return playbookStatsRepository.findByOffensivePlaybookOrderBySeasonNumberDesc(offensivePlaybook)
    }

    /**
     * Get all playbook stats for a specific defensive playbook
     */
    fun getPlaybookStatsByDefensivePlaybook(defensivePlaybook: DefensivePlaybook): List<PlaybookStats> {
        return playbookStatsRepository.findByDefensivePlaybookOrderBySeasonNumberDesc(defensivePlaybook)
    }

    /**
     * Get all playbook stats for a specific season
     */
    fun getPlaybookStatsBySeason(seasonNumber: Int): List<PlaybookStats> {
        return playbookStatsRepository.findBySeasonNumberOrderByOffensivePlaybookAscDefensivePlaybookAsc(seasonNumber)
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
                    "Generating playbook stats for $offensivePlaybook/$defensivePlaybook in season $seasonNumber with ${gameStatsList.size} games",
                )
                generatePlaybookStatsForOffensivePlaybookAndDefensivePlaybookAndSeason(offensivePlaybook, defensivePlaybook, seasonNumber)
            }
        }

        Logger.info("Completed generation of all playbook stats")
    }

    /**
     * Generate playbook stats for a specific offensive playbook and season (all defensive playbooks)
     */
    fun generatePlaybookStatsForOffensivePlaybookAndSeason(
        offensivePlaybook: OffensivePlaybook,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of playbook stats for $offensivePlaybook in season $seasonNumber")

        // Get all game stats for this offensive playbook and season
        val gameStatsList =
            gameStatsRepository.findBySeasonOrderByGameIdAsc(seasonNumber)
                .filter { gameStats -> gameStats.offensivePlaybook == offensivePlaybook }

        if (gameStatsList.isEmpty()) {
            Logger.warn("No game stats found for $offensivePlaybook in season $seasonNumber")
            return
        }

        // Group by defensive playbook and generate playbook stats for each defensive playbook
        val groupedByDefensivePlaybook = gameStatsList.groupBy { it.defensivePlaybook }

        for ((defensivePlaybook, defensivePlaybookGameStats) in groupedByDefensivePlaybook) {
            if (defensivePlaybook != null) {
                generatePlaybookStatsForOffensivePlaybookAndDefensivePlaybookAndSeason(offensivePlaybook, defensivePlaybook, seasonNumber)
            }
        }

        Logger.info("Completed generating playbook stats for $offensivePlaybook in season $seasonNumber")
    }

    /**
     * Generate playbook stats for a specific defensive playbook and season (all offensive playbooks)
     */
    fun generatePlaybookStatsForDefensivePlaybookAndSeason(
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ) {
        Logger.info("Starting generation of playbook stats for $defensivePlaybook in season $seasonNumber")

        // Get all game stats for this defensive playbook and season
        val gameStatsList =
            gameStatsRepository.findBySeasonOrderByGameIdAsc(seasonNumber)
                .filter { gameStats -> gameStats.defensivePlaybook == defensivePlaybook }

        if (gameStatsList.isEmpty()) {
            Logger.warn("No game stats found for $defensivePlaybook in season $seasonNumber")
            return
        }

        // Group by offensive playbook and generate playbook stats for each offensive playbook
        val groupedByOffensivePlaybook = gameStatsList.groupBy { it.offensivePlaybook }

        for ((offensivePlaybook, offensivePlaybookGameStats) in groupedByOffensivePlaybook) {
            if (offensivePlaybook != null) {
                generatePlaybookStatsForOffensivePlaybookAndDefensivePlaybookAndSeason(offensivePlaybook, defensivePlaybook, seasonNumber)
            }
        }

        Logger.info("Completed generating playbook stats for $defensivePlaybook in season $seasonNumber")
    }

    /**
     * Generate playbook stats for a specific offensive playbook, defensive playbook, and season
     */
    fun generatePlaybookStatsForOffensivePlaybookAndDefensivePlaybookAndSeason(
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
            extraPointsAttempted = 0, // Not tracked in GameStats
            extraPointsMade = 0, // Not tracked in GameStats
            extraPointPercentage = 0.0, // Not tracked in GameStats
            punts = gameStatsList.sumOf { it.puntsAttempted },
            puntYards = 0, // Not tracked in GameStats
            longestPunt = gameStatsList.maxOfOrNull { it.longestPunt } ?: 0,
            kickoffReturns = 0, // Not tracked in GameStats
            kickoffReturnYards = 0, // Not tracked in GameStats
            kickoffReturnTouchdowns = gameStatsList.sumOf { it.kickReturnTd },
            puntReturns = 0, // Not tracked in GameStats
            puntReturnYards = 0, // Not tracked in GameStats
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

    /**
     * Check if playbook stats exist for a specific offensive playbook, defensive playbook, and season
     */
    fun playbookStatsExistForOffensivePlaybookAndDefensivePlaybookAndSeason(
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ): Boolean {
        return playbookStatsRepository.existsByOffensivePlaybookAndDefensivePlaybookAndSeasonNumber(
            offensivePlaybook,
            defensivePlaybook,
            seasonNumber,
        )
    }

    /**
     * Check if playbook stats exist for a specific offensive playbook and season
     */
    fun playbookStatsExistForOffensivePlaybookAndSeason(
        offensivePlaybook: OffensivePlaybook,
        seasonNumber: Int,
    ): Boolean {
        return playbookStatsRepository.existsByOffensivePlaybookAndSeasonNumber(offensivePlaybook, seasonNumber)
    }

    /**
     * Check if playbook stats exist for a specific defensive playbook and season
     */
    fun playbookStatsExistForDefensivePlaybookAndSeason(
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ): Boolean {
        return playbookStatsRepository.existsByDefensivePlaybookAndSeasonNumber(defensivePlaybook, seasonNumber)
    }

    /**
     * Check if playbook stats exist for a specific offensive playbook
     */
    fun playbookStatsExistForOffensivePlaybook(offensivePlaybook: OffensivePlaybook): Boolean {
        return playbookStatsRepository.countByOffensivePlaybook(offensivePlaybook) > 0
    }

    /**
     * Check if playbook stats exist for a specific defensive playbook
     */
    fun playbookStatsExistForDefensivePlaybook(defensivePlaybook: DefensivePlaybook): Boolean {
        return playbookStatsRepository.countByDefensivePlaybook(defensivePlaybook) > 0
    }

    /**
     * Check if playbook stats exist for a specific season
     */
    fun playbookStatsExistForSeason(seasonNumber: Int): Boolean {
        return playbookStatsRepository.countBySeasonNumber(seasonNumber) > 0
    }
}
