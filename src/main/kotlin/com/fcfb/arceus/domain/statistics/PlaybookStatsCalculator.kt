package com.fcfb.arceus.domain.statistics

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.model.statistics.PlaybookStats
import com.fcfb.arceus.util.statistics.StatsAverageCalculator
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PlaybookStatsCalculator(
    private val statsAverageCalculator: StatsAverageCalculator,
) {
    fun aggregateGameStatsToPlaybookStats(
        gameStatsList: List<GameStats>,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ): PlaybookStats {
        val totalTeams = gameStatsList.map { it.team }.distinct().size
        val totalGames = gameStatsList.size

        val offensivePlayYards = gameStatsList.sumOf { it.offensivePlayYards }
        val offensivePlayCount = gameStatsList.sumOf { it.offensivePlayCount }

        return PlaybookStats(
            offensivePlaybook = offensivePlaybook,
            defensivePlaybook = defensivePlaybook,
            seasonNumber = seasonNumber,
            totalTeams = totalTeams,
            totalGames = totalGames,
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
            offensivePlayYards = offensivePlayYards,
            offensivePlayCount = offensivePlayCount,
            averageYardsPerPlay =
                statsAverageCalculator.average(
                    offensivePlayYards,
                    offensivePlayCount,
                ),
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
            averageOffensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveDiff }) ?: 0.0,
            averageDefensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveDiff }) ?: 0.0,
            averageOffensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }) ?: 0.0,
            averageDefensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }) ?: 0.0,
            averageDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDiff }) ?: 0.0,
            averageResponseSpeed = calculateAverage(gameStatsList.mapNotNull { it.averageResponseSpeed }) ?: 0.0,
            lastModifiedTs = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT),
        )
    }

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

    private fun calculateAverage(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        return values.average()
    }
}
