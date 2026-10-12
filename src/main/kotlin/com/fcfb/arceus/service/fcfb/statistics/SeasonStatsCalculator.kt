package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.SeasonStats
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object SeasonStatsCalculator {
    fun aggregateGameStatsToSeasonStats(
        gameStatsList: List<GameStats>,
        team: String,
        seasonNumber: Int,
        gameStatsByGameId: Map<Int, List<GameStats>>,
        conference: String?,
    ): SeasonStats {
        val firstGameStats = gameStatsList.first()

        var wins = 0
        var losses = 0

        for (teamGameStats in gameStatsList) {
            val gameId = teamGameStats.gameId
            val teamScore = teamGameStats.score

            val opponentGameStats =
                gameStatsByGameId[gameId]
                    ?.firstOrNull { it.team != team }

            if (opponentGameStats != null) {
                val opponentScore = opponentGameStats.score
                if (teamScore > opponentScore) {
                    wins++
                } else if (teamScore < opponentScore) {
                    losses++
                }
            }
        }

        return SeasonStats(
            team = team,
            seasonNumber = seasonNumber,
            wins = wins,
            losses = losses,
            subdivision = firstGameStats.subdivision,
            conference = conference,
            offensivePlaybook = firstGameStats.offensivePlaybook,
            defensivePlaybook = firstGameStats.defensivePlaybook,
            passAttempts = gameStatsList.sumOf { it.passAttempts },
            passCompletions = gameStatsList.sumOf { it.passCompletions },
            passCompletionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.passCompletions },
                    gameStatsList.sumOf { it.passAttempts },
                ),
            passYards = gameStatsList.sumOf { it.passYards },
            longestPass = gameStatsList.maxOfOrNull { it.longestPass } ?: 0,
            passTouchdowns = gameStatsList.sumOf { it.passTouchdowns },
            passSuccesses = gameStatsList.sumOf { it.passSuccesses },
            passSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.passSuccesses },
                    gameStatsList.sumOf { it.passAttempts },
                ),
            rushAttempts = gameStatsList.sumOf { it.rushAttempts },
            rushSuccesses = gameStatsList.sumOf { it.rushSuccesses },
            rushSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.rushSuccesses },
                    gameStatsList.sumOf { it.rushAttempts },
                ),
            rushYards = gameStatsList.sumOf { it.rushYards },
            longestRun = gameStatsList.maxOfOrNull { it.longestRun } ?: 0,
            rushTouchdowns = gameStatsList.sumOf { it.rushTouchdowns },
            totalYards = gameStatsList.sumOf { it.totalYards },
            firstDowns = gameStatsList.sumOf { it.firstDowns },
            sacksAllowed = gameStatsList.sumOf { it.sacksAllowed },
            sacksForced = gameStatsList.sumOf { it.sacksForced },
            interceptionsLost = gameStatsList.sumOf { it.interceptionsLost },
            interceptionsForced = gameStatsList.sumOf { it.interceptionsForced },
            fumblesLost = gameStatsList.sumOf { it.fumblesLost },
            fumblesForced = gameStatsList.sumOf { it.fumblesForced },
            turnoversLost = gameStatsList.sumOf { it.turnoversLost },
            turnoversForced = gameStatsList.sumOf { it.turnoversForced },
            turnoverDifferential = gameStatsList.sumOf { it.turnoverDifferential },
            turnoverTouchdownsLost = gameStatsList.sumOf { it.turnoverTouchdownsLost },
            turnoverTouchdownsForced = gameStatsList.sumOf { it.turnoverTouchdownsForced },
            pickSixesThrown = gameStatsList.sumOf { it.pickSixesThrown },
            pickSixesForced = gameStatsList.sumOf { it.pickSixesForced },
            fumbleReturnTdsCommitted = gameStatsList.sumOf { it.fumbleReturnTdsCommitted },
            fumbleReturnTdsForced = gameStatsList.sumOf { it.fumbleReturnTdsForced },
            fieldGoalMade = gameStatsList.sumOf { it.fieldGoalMade },
            fieldGoalAttempts = gameStatsList.sumOf { it.fieldGoalAttempts },
            fieldGoalPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.fieldGoalMade },
                    gameStatsList.sumOf { it.fieldGoalAttempts },
                ),
            longestFieldGoal = gameStatsList.maxOfOrNull { it.longestFieldGoal } ?: 0,
            blockedOpponentFieldGoals = gameStatsList.sumOf { it.blockedOpponentFieldGoals },
            fieldGoalTouchdown = gameStatsList.sumOf { it.fieldGoalTouchdown },
            puntsAttempted = gameStatsList.sumOf { it.puntsAttempted },
            longestPunt = gameStatsList.maxOfOrNull { it.longestPunt } ?: 0,
            blockedOpponentPunt = gameStatsList.sumOf { it.blockedOpponentPunt },
            puntReturnTd = gameStatsList.sumOf { it.puntReturnTd },
            puntReturnTdPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.puntReturnTd },
                    gameStatsList.sumOf { it.puntsAttempted },
                ),
            numberOfKickoffs = gameStatsList.sumOf { it.numberOfKickoffs },
            onsideAttempts = gameStatsList.sumOf { it.onsideAttempts },
            onsideSuccess = gameStatsList.sumOf { it.onsideSuccess },
            onsideSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.onsideSuccess },
                    gameStatsList.sumOf { it.onsideAttempts },
                ),
            normalKickoffAttempts = gameStatsList.sumOf { it.normalKickoffAttempts },
            touchbacks = gameStatsList.sumOf { it.touchbacks },
            touchbackPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.touchbacks },
                    gameStatsList.sumOf { it.normalKickoffAttempts },
                ),
            kickReturnTd = gameStatsList.sumOf { it.kickReturnTd },
            kickReturnTdPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.kickReturnTd },
                    gameStatsList.sumOf { it.numberOfKickoffs },
                ),
            numberOfDrives = gameStatsList.sumOf { it.numberOfDrives },
            timeOfPossession = gameStatsList.sumOf { it.timeOfPossession },
            touchdowns = gameStatsList.sumOf { it.touchdowns },
            thirdDownConversionSuccess = gameStatsList.sumOf { it.thirdDownConversionSuccess },
            thirdDownConversionAttempts = gameStatsList.sumOf { it.thirdDownConversionAttempts },
            thirdDownConversionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.thirdDownConversionSuccess },
                    gameStatsList.sumOf { it.thirdDownConversionAttempts },
                ),
            fourthDownConversionSuccess = gameStatsList.sumOf { it.fourthDownConversionSuccess },
            fourthDownConversionAttempts = gameStatsList.sumOf { it.fourthDownConversionAttempts },
            fourthDownConversionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.fourthDownConversionSuccess },
                    gameStatsList.sumOf { it.fourthDownConversionAttempts },
                ),
            largestLead = gameStatsList.maxOfOrNull { it.largestLead } ?: 0,
            largestDeficit = gameStatsList.maxOfOrNull { it.largestDeficit } ?: 0,
            redZoneAttempts = gameStatsList.sumOf { it.redZoneAttempts },
            redZoneSuccesses = gameStatsList.sumOf { it.redZoneSuccesses },
            redZoneSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.redZoneSuccesses },
                    gameStatsList.sumOf { it.redZoneAttempts },
                ),
            redZonePercentage =
                calculatePercentage(
                    gameStatsList.sumOf { it.redZoneAttempts },
                    gameStatsList.sumOf { it.numberOfDrives },
                ),
            safetiesForced = gameStatsList.sumOf { it.safetiesForced },
            safetiesCommitted = gameStatsList.sumOf { it.safetiesCommitted },
            averageOffensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveDiff }),
            averageDefensiveDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveDiff }),
            averageOffensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageOffensiveSpecialTeamsDiff }),
            averageDefensiveSpecialTeamsDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDefensiveSpecialTeamsDiff }),
            averageDiff = calculateAverage(gameStatsList.mapNotNull { it.averageDiff }),
            averageResponseSpeed = calculateAverage(gameStatsList.mapNotNull { it.averageResponseSpeed }),
            opponentPassAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passAttempts ?: 0
                },
            opponentPassCompletions =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passCompletions ?: 0
                },
            opponentPassCompletionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passCompletions ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passAttempts ?: 0 },
                ),
            opponentPassYards =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passYards ?: 0
                },
            opponentLongestPass =
                gameStatsList.mapNotNull {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.longestPass
                }.maxOrNull() ?: 0,
            opponentPassTouchdowns =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passTouchdowns ?: 0
                },
            opponentPassSuccesses =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passSuccesses ?: 0
                },
            opponentPassSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passSuccesses ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.passAttempts ?: 0 },
                ),
            opponentRushAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushAttempts ?: 0
                },
            opponentRushSuccesses =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushSuccesses ?: 0
                },
            opponentRushSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushSuccesses ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushAttempts ?: 0 },
                ),
            opponentRushYards =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushYards ?: 0
                },
            opponentLongestRun =
                gameStatsList.mapNotNull {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.longestRun
                }.maxOrNull() ?: 0,
            opponentRushTouchdowns =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.rushTouchdowns ?: 0
                },
            opponentTotalYards =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.totalYards ?: 0
                },
            opponentFirstDowns =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.firstDowns ?: 0
                },
            opponentFieldGoalMade =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalMade ?: 0
                },
            opponentFieldGoalAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalAttempts ?: 0
                },
            opponentFieldGoalPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalMade ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalAttempts ?: 0 },
                ),
            opponentLongestFieldGoal =
                gameStatsList.mapNotNull {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.longestFieldGoal
                }.maxOrNull() ?: 0,
            opponentFieldGoalTouchdown =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fieldGoalTouchdown ?: 0
                },
            opponentPuntsAttempted =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.puntsAttempted ?: 0
                },
            opponentLongestPunt =
                gameStatsList.mapNotNull {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.longestPunt
                }.maxOrNull() ?: 0,
            opponentPuntReturnTd =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.puntReturnTd ?: 0
                },
            opponentPuntReturnTdPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.puntReturnTd ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.puntsAttempted ?: 0 },
                ),
            opponentNumberOfKickoffs =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.numberOfKickoffs ?: 0
                },
            opponentOnsideAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.onsideAttempts ?: 0
                },
            opponentOnsideSuccess =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.onsideSuccess ?: 0
                },
            opponentOnsideSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.onsideSuccess ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.onsideAttempts ?: 0 },
                ),
            opponentNormalKickoffAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.normalKickoffAttempts ?: 0
                },
            opponentTouchbacks =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.touchbacks ?: 0
                },
            opponentTouchbackPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.touchbacks ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.normalKickoffAttempts ?: 0 },
                ),
            opponentKickReturnTd =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.kickReturnTd ?: 0
                },
            opponentKickReturnTdPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.kickReturnTd ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.numberOfKickoffs ?: 0 },
                ),
            opponentNumberOfDrives =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.numberOfDrives ?: 0
                },
            opponentTimeOfPossession =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.timeOfPossession ?: 0
                },
            opponentTouchdowns =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.touchdowns ?: 0
                },
            opponentThirdDownConversionSuccess =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.thirdDownConversionSuccess ?: 0
                },
            opponentThirdDownConversionAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.thirdDownConversionAttempts ?: 0
                },
            opponentThirdDownConversionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.thirdDownConversionSuccess ?: 0
                    },
                    gameStatsList.sumOf {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.thirdDownConversionAttempts ?: 0
                    },
                ),
            opponentFourthDownConversionSuccess =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fourthDownConversionSuccess ?: 0
                },
            opponentFourthDownConversionAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fourthDownConversionAttempts ?: 0
                },
            opponentFourthDownConversionPercentage =
                calculatePercentage(
                    gameStatsList.sumOf {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fourthDownConversionSuccess ?: 0
                    },
                    gameStatsList.sumOf {
                        getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.fourthDownConversionAttempts ?: 0
                    },
                ),
            opponentRedZoneAttempts =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneAttempts ?: 0
                },
            opponentRedZoneSuccesses =
                gameStatsList.sumOf {
                    getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneSuccesses ?: 0
                },
            opponentRedZoneSuccessPercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneSuccesses ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneAttempts ?: 0 },
                ),
            opponentRedZonePercentage =
                calculatePercentage(
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.redZoneAttempts ?: 0 },
                    gameStatsList.sumOf { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team)?.numberOfDrives ?: 0 },
                ),
            lastModifiedTs =
                ZonedDateTime.now(ZoneId.of("America/New_York"))
                    .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
        ).also { stats ->
            updateOffensiveAverages(stats, gameStatsList)
            updateOpponentAverages(stats, gameStatsList.mapNotNull { getOpponentGameStatsByGameId(it.gameId, gameStatsByGameId, team) })
        }
    }

    private fun calculateAverage(values: List<Double>): Double? {
        return if (values.isNotEmpty()) {
            values.average()
        } else {
            null
        }
    }

    private fun updateOffensiveAverages(
        stats: SeasonStats,
        games: List<GameStats>,
    ) {
        stats.offensivePlayYards = games.sumOf { it.offensivePlayYards }
        stats.offensivePlayCount = games.sumOf { it.offensivePlayCount }
        stats.averageYardsPerPlay = StatsAverageCalculator.average(stats.offensivePlayYards, stats.offensivePlayCount)
        stats.puntYards = games.sumOf { it.puntYards }
        stats.puntCount = games.sumOf { it.puntCount }
        stats.averagePuntLength = StatsAverageCalculator.average(stats.puntYards, stats.puntCount)
    }

    private fun updateOpponentAverages(
        stats: SeasonStats,
        opponents: List<GameStats>,
    ) {
        stats.opponentOffensivePlayYards = opponents.sumOf { it.offensivePlayYards }
        stats.opponentOffensivePlayCount = opponents.sumOf { it.offensivePlayCount }
        stats.opponentAverageYardsPerPlay =
            StatsAverageCalculator.average(stats.opponentOffensivePlayYards, stats.opponentOffensivePlayCount)
        stats.opponentPuntYards = opponents.sumOf { it.puntYards }
        stats.opponentPuntCount = opponents.sumOf { it.puntCount }
        stats.opponentAveragePuntLength = StatsAverageCalculator.average(stats.opponentPuntYards, stats.opponentPuntCount)
    }

    private fun getOpponentGameStatsByGameId(
        gameId: Int,
        gameStatsByGameId: Map<Int, List<GameStats>>,
        team: String,
    ): GameStats? {
        return gameStatsByGameId[gameId]
            ?.firstOrNull { it.team != team }
    }

    private fun calculatePercentage(
        successes: Int,
        attempts: Int,
    ): Double? {
        if (attempts == 0) return null
        return (successes.toDouble() / attempts.toDouble()) * 100.0
    }
}
