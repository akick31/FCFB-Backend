package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.LeagueStats
import com.fcfb.arceus.model.SeasonStats
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object LeagueStatsCalculator {
    fun aggregateSeasonStatsToLeagueStats(
        seasonStatsList: List<SeasonStats>,
        subdivision: Subdivision,
        seasonNumber: Int,
    ): LeagueStats {
        val totalTeams = seasonStatsList.size
        val totalGames = seasonStatsList.sumOf { it.wins + it.losses }

        val offensivePlayYards = seasonStatsList.sumOf { it.offensivePlayYards }
        val offensivePlayCount = seasonStatsList.sumOf { it.offensivePlayCount }

        return LeagueStats(
            subdivision = subdivision,
            seasonNumber = seasonNumber,
            totalTeams = totalTeams,
            totalGames = totalGames,
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
            offensivePlayYards = offensivePlayYards,
            offensivePlayCount = offensivePlayCount,
            averageYardsPerPlay =
                StatsAverageCalculator.average(
                    offensivePlayYards,
                    offensivePlayCount,
                ),
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
            averageOffensiveDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageOffensiveDiff }) ?: 0.0,
            averageDefensiveDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDefensiveDiff }) ?: 0.0,
            averageOffensiveSpecialTeamsDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }) ?: 0.0,
            averageDefensiveSpecialTeamsDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }) ?: 0.0,
            averageDiff = calculateAverage(seasonStatsList.mapNotNull { it.averageDiff }) ?: 0.0,
            averageResponseSpeed = calculateAverage(seasonStatsList.mapNotNull { it.averageResponseSpeed }) ?: 0.0,
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
