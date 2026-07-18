package com.fcfb.arceus.service.fcfb.gamestats

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play

object GameStatsCalculator {
    fun calculatePassAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            when {
                play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS -> false
                play.playCall == PlayCall.SPIKE -> true
                play.playCall == PlayCall.PASS -> true
                else -> false
            }
        }
    }

    fun calculatePassCompletions(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            when {
                play.playCall == PlayCall.PASS && (
                    play.result != Scenario.INCOMPLETE &&
                        play.result != Scenario.LOSS_OF_10_YARDS &&
                        play.result != Scenario.LOSS_OF_7_YARDS &&
                        play.result != Scenario.LOSS_OF_5_YARDS &&
                        play.result != Scenario.LOSS_OF_3_YARDS &&
                        play.result != Scenario.LOSS_OF_2_YARDS &&
                        play.result != Scenario.LOSS_OF_1_YARD &&
                        play.result != Scenario.TURNOVER_PLUS_20_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_15_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_10_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_5_YARDS &&
                        play.result != Scenario.TURNOVER &&
                        play.result != Scenario.TURNOVER_MINUS_5_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_10_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_15_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_20_YARDS &&
                        play.result != Scenario.TURNOVER_TOUCHDOWN &&
                        play.result != Scenario.SAFETY
                ) -> true
                else -> false
            }
        }
    }

    fun calculatePassYards(allPlays: List<Play>): Int {
        return allPlays.sumOf { play ->
            // Don't count sacks towards passing yards
            when {
                play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS -> 0
                play.playCall == PlayCall.PASS -> play.yards
                else -> 0
            }
        }
    }

    fun calculateLongestPass(allPlays: List<Play>): Int {
        return allPlays
            .filter { play ->
                play.playCall == PlayCall.PASS && play.result != Scenario.INCOMPLETE
            }
            .maxOfOrNull { it.yards } ?: 0
    }

    fun calculateSacksAllowed(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PASS && play.actualResult == ActualResult.LOSS
        }
    }

    fun calculateRushAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.RUN
        }
    }

    fun calculateRushSuccesses(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            if (play.playCall != PlayCall.RUN) return@count false

            val yardsToGo = play.yardsToGo
            val yardsGained = play.yards
            val down = play.down

            val isSuccess =
                when (down) {
                    1 -> yardsGained >= (yardsToGo * 0.5)
                    2 -> yardsGained >= (yardsToGo * 0.7)
                    3, 4 -> yardsGained >= yardsToGo
                    else -> false
                }

            isSuccess || play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    fun calculateRushYards(allPlays: List<Play>): Int {
        return allPlays.sumOf { play ->
            when {
                // Count sacks towards rushing yards
                play.playCall == PlayCall.PASS && (
                    play.result == Scenario.LOSS_OF_10_YARDS ||
                        play.result == Scenario.LOSS_OF_7_YARDS ||
                        play.result == Scenario.LOSS_OF_5_YARDS ||
                        play.result == Scenario.LOSS_OF_3_YARDS ||
                        play.result == Scenario.LOSS_OF_2_YARDS ||
                        play.result == Scenario.LOSS_OF_1_YARD
                ) -> play.yards

                play.playCall == PlayCall.RUN && (
                    play.result != Scenario.TURNOVER_PLUS_20_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_15_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_10_YARDS &&
                        play.result != Scenario.TURNOVER_PLUS_5_YARDS &&
                        play.result != Scenario.TURNOVER &&
                        play.result != Scenario.TURNOVER_MINUS_5_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_10_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_15_YARDS &&
                        play.result != Scenario.TURNOVER_MINUS_20_YARDS &&
                        play.result != Scenario.TURNOVER_TOUCHDOWN &&
                        play.result != Scenario.SAFETY
                ) -> play.yards

                else -> 0
            }
        }
    }

    fun calculateLongestRun(allPlays: List<Play>): Int {
        return allPlays
            .filter { play ->
                play.playCall == PlayCall.RUN && play.result != Scenario.INCOMPLETE
            }
            .maxOfOrNull { it.yards } ?: 0
    }

    fun calculatePassSuccesses(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            if (play.playCall != PlayCall.PASS) return@count false

            val yardsToGo = play.yardsToGo
            val yardsGained = play.yards
            val down = play.down

            val isSuccess =
                when (down) {
                    1 -> yardsGained >= (yardsToGo * 0.5)
                    2 -> yardsGained >= (yardsToGo * 0.7)
                    3, 4 -> yardsGained >= yardsToGo
                    else -> false
                }

            isSuccess || play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    fun calculateTotalYards(
        passingYards: Int,
        rushingYards: Int,
    ): Int {
        return passingYards + rushingYards
    }

    fun calculateInterceptionsLost(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PASS && (
                play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
            )
        }
    }

    fun calculateFumblesLost(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.RUN && (
                play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
            )
        }
    }

    fun calculateTurnoversLost(
        interceptionsLost: Int,
        fumblesLost: Int,
    ): Int {
        return interceptionsLost + fumblesLost
    }

    fun calculateTurnoverTouchdownsLost(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
        }
    }

    fun calculateFieldGoalMade(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD
        }
    }

    fun calculateFieldGoalAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.FIELD_GOAL
        }
    }

    fun calculateLongestFieldGoal(allPlays: List<Play>): Int {
        return allPlays
            .filter { play ->
                play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD
            }
            .maxOfOrNull { play ->
                (100 - play.ballLocation) + 17
            } ?: 0
    }

    fun calculateBlockedOpponentFieldGoals(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.FIELD_GOAL && play.actualResult == ActualResult.BLOCKED
        }
    }

    fun calculateFieldGoalTouchdown(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.FIELD_GOAL && play.actualResult == ActualResult.KICK_SIX
        }
    }

    fun calculatePuntsAttempted(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PUNT
        }
    }

    fun calculateLongestPunt(allPlays: List<Play>): Int {
        val puntDistances =
            listOf(
                Scenario.FIVE_YARD_PUNT, Scenario.TEN_YARD_PUNT, Scenario.FIFTEEN_YARD_PUNT, Scenario.TWENTY_YARD_PUNT,
                Scenario.TWENTY_FIVE_YARD_PUNT, Scenario.THIRTY_YARD_PUNT, Scenario.THIRTY_FIVE_YARD_PUNT,
                Scenario.FORTY_YARD_PUNT, Scenario.FORTY_FIVE_YARD_PUNT, Scenario.FIFTY_YARD_PUNT,
                Scenario.FIFTY_FIVE_YARD_PUNT, Scenario.SIXTY_YARD_PUNT, Scenario.SIXTY_FIVE_YARD_PUNT,
                Scenario.SEVENTY_YARD_PUNT,
            )

        return allPlays
            .filter { play -> play.result in puntDistances }
            .maxOfOrNull { play ->
                play.result?.description?.substringBefore(" YARD PUNT")?.toInt() ?: 0
            } ?: 0
    }

    fun calculateAveragePuntLength(allPlays: List<Play>): Double {
        val average =
            allPlays
                .filter { play ->
                    play.playCall == PlayCall.PUNT &&
                        play.result?.description?.contains(" YARD PUNT") ?: false
                }
                .map { play ->
                    play.result?.description?.substringBefore(" YARD PUNT")?.toInt() ?: 0
                }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    fun calculateBlockedOpponentPunt(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PUNT && play.actualResult == ActualResult.BLOCKED
        }
    }

    fun calculatePuntReturnTd(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PUNT && play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN
        }
    }

    fun calculateNumberOfKickoffs(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_NORMAL ||
                play.playCall == PlayCall.KICKOFF_ONSIDE ||
                play.playCall == PlayCall.KICKOFF_SQUIB
        }
    }

    fun calculateOnsideAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_ONSIDE
        }
    }

    fun calculateOnsideSuccess(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_ONSIDE && play.result == Scenario.RECOVERED
        }
    }

    fun calculateNormalKickoffAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_NORMAL
        }
    }

    fun calculateTouchbacks(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_NORMAL && play.result == Scenario.TOUCHBACK
        }
    }

    fun calculateKickReturnTd(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.KICKOFF_NORMAL && play.actualResult == ActualResult.RETURN_TOUCHDOWN
        }
    }

    fun calculateNumberOfDrives(
        allPlays: List<Play>,
        teamSide: TeamSide,
    ): Int {
        var driveCount = 0
        var isDriveInProgress = false

        allPlays.sortedBy { it.playId }.forEach { play ->
            when {
                // If the current play is a kickoff, end the current drive
                (
                    play.playCall == PlayCall.KICKOFF_NORMAL ||
                        play.playCall == PlayCall.KICKOFF_ONSIDE ||
                        play.playCall == PlayCall.KICKOFF_SQUIB
                ) && play.possession == teamSide -> {
                    isDriveInProgress = false
                }

                // Player starts or continues a drive (possession belongs to the player)
                play.possession == teamSide && !isDriveInProgress -> {
                    // Start a new drive
                    driveCount++
                    isDriveInProgress = true
                }

                // If possession changes to another player or a turnover happens
                play.possession != teamSide ||
                    play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN -> {
                    // End the current drive
                    isDriveInProgress = false
                }
            }
        }

        return driveCount
    }

    fun calculateTimeOfPossession(allPlays: List<Play>): Int {
        return allPlays
            .filter { play ->
                play.playCall != PlayCall.KICKOFF_NORMAL &&
                    play.playCall != PlayCall.KICKOFF_ONSIDE &&
                    play.playCall != PlayCall.KICKOFF_SQUIB
            }
            .sumOf { play ->
                if (play.clock - play.playTime + play.runoffTime < 0) {
                    play.clock
                } else {
                    play.playTime + play.runoffTime
                }
            }
    }

    fun calculateTouchdowns(
        allPlays: List<Play>,
        teamSide: TeamSide,
    ): Int {
        val offensiveTouchdowns =
            allPlays.count { play ->
                play.possession == teamSide && (
                    play.actualResult == ActualResult.TOUCHDOWN ||
                        play.actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
                        play.actualResult == ActualResult.PUNT_TEAM_TOUCHDOWN
                )
            }

        val defensiveTouchdowns =
            allPlays.count { play ->
                play.possession != teamSide && (
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN ||
                        play.actualResult == ActualResult.RETURN_TOUCHDOWN ||
                        play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN ||
                        play.actualResult == ActualResult.KICK_SIX
                )
            }

        return offensiveTouchdowns + defensiveTouchdowns
    }

    fun calculateAverageNormalPlayDiff(allPlays: List<Play>): Double {
        val average =
            allPlays
                .filter { play ->
                    play.playCall != PlayCall.KICKOFF_NORMAL &&
                        play.playCall != PlayCall.KICKOFF_ONSIDE &&
                        play.playCall != PlayCall.KICKOFF_SQUIB &&
                        play.playCall != PlayCall.PAT &&
                        play.playCall != PlayCall.TWO_POINT &&
                        play.playCall != PlayCall.KNEEL &&
                        play.playCall != PlayCall.SPIKE
                }
                .mapNotNull { it.difference }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    fun calculateAverageSpecialTeamsDiff(allPlays: List<Play>): Double {
        val average =
            allPlays
                .filter { play ->
                    play.playCall == PlayCall.KICKOFF_NORMAL ||
                        play.playCall == PlayCall.KICKOFF_ONSIDE ||
                        play.playCall == PlayCall.KICKOFF_SQUIB ||
                        play.playCall == PlayCall.FIELD_GOAL ||
                        play.playCall == PlayCall.PUNT
                }
                .mapNotNull { it.difference }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    fun calculateAverageYardsPerPlay(allPlays: List<Play>): Double {
        val average =
            allPlays
                .filter { play ->
                    play.playCall != PlayCall.KICKOFF_NORMAL &&
                        play.playCall != PlayCall.KICKOFF_ONSIDE &&
                        play.playCall != PlayCall.KICKOFF_SQUIB &&
                        play.playCall != PlayCall.PAT &&
                        play.playCall != PlayCall.TWO_POINT
                }
                .map { it.yards }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    fun calculateFirstDowns(allPlays: List<Play>): Int {
        return allPlays.count { play -> play.actualResult == ActualResult.FIRST_DOWN }
    }

    fun calculateThirdDownConversionSuccess(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.down == 3 && play.yards > play.yardsToGo
        }
    }

    fun calculateThirdDownConversionAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.down == 3
        }
    }

    fun calculateFourthDownConversionSuccess(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.down == 4 && play.yards > play.yardsToGo
        }
    }

    fun calculateFourthDownConversionAttempts(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.down == 4 && (play.playCall == PlayCall.RUN || play.playCall == PlayCall.PASS)
        }
    }

    fun calculateLargestLeadForHome(allPlays: List<Play>): Int {
        return allPlays.maxOfOrNull { play ->
            play.homeScore - play.awayScore
        } ?: 0
    }

    fun calculateLargestDeficitForHome(allPlays: List<Play>): Int {
        return allPlays.maxOfOrNull { play ->
            play.awayScore - play.homeScore
        } ?: 0
    }

    fun calculateLargestLeadForAway(allPlays: List<Play>): Int {
        return allPlays.maxOfOrNull { play ->
            play.awayScore - play.homeScore
        } ?: 0
    }

    fun calculateLargestDeficitForAway(allPlays: List<Play>): Int {
        return allPlays.maxOfOrNull { play ->
            play.homeScore - play.awayScore
        } ?: 0
    }

    fun calculatePassTouchdowns(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PASS && play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    fun calculateRushTouchdowns(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.RUN && play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    fun calculateRedZoneAttempts(
        allPlays: List<Play>,
        teamSide: TeamSide,
    ): Int {
        var redZoneAttempts = 0
        var isDriveInProgress = false
        var visitedRedZoneOnDrive = false

        allPlays.sortedBy { it.playId }.forEach { play ->
            when {
                // If the current play is a kickoff, end the current drive
                (
                    play.playCall == PlayCall.KICKOFF_NORMAL ||
                        play.playCall == PlayCall.KICKOFF_ONSIDE ||
                        play.playCall == PlayCall.KICKOFF_SQUIB
                ) && play.possession == teamSide -> {
                    isDriveInProgress = false
                }

                // Player starts or continues a drive (possession belongs to the player)
                play.possession == teamSide && !isDriveInProgress -> {
                    // Start a new drive
                    isDriveInProgress = true
                    visitedRedZoneOnDrive = false
                }

                // If possession changes to another player or a turnover happens
                play.possession != teamSide ||
                    play.actualResult == ActualResult.TURNOVER ||
                    play.actualResult == ActualResult.TURNOVER_TOUCHDOWN -> {
                    // End the current drive
                    isDriveInProgress = false
                }

                // If the current play is in the red zone
                play.ballLocation >= 80 &&
                    !visitedRedZoneOnDrive &&
                    isDriveInProgress &&
                    play.playCall != PlayCall.PAT -> {
                    redZoneAttempts++
                    visitedRedZoneOnDrive = true
                }
            }
        }
        return redZoneAttempts
    }

    fun calculateRedZoneSuccesses(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.ballLocation >= 80 &&
                play.actualResult == ActualResult.TOUCHDOWN
        }
    }

    fun calculateAverageDiff(allPlays: List<Play>): Double {
        val average = allPlays.mapNotNull { it.difference }.average()
        return if (average.isNaN()) 0.0 else average
    }

    fun calculateTurnoverDifferential(
        turnoversLost: Int,
        turnoversForced: Int,
    ): Int {
        return turnoversForced - turnoversLost
    }

    fun calculatePickSixes(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.PASS && play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
        }
    }

    fun calculateFumbleReturnTds(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.playCall == PlayCall.RUN && play.actualResult == ActualResult.TURNOVER_TOUCHDOWN
        }
    }

    fun calculateSafeties(allPlays: List<Play>): Int {
        return allPlays.count { play ->
            play.actualResult == ActualResult.SAFETY
        }
    }

    fun calculateAverageResponseSpeed(
        allPlays: List<Play>,
        teamSide: TeamSide,
    ): Double {
        val average =
            allPlays
                .filter { play ->
                    play.offensiveResponseSpeed != null && play.defensiveResponseSpeed != null
                }
                .map { play ->
                    if (play.possession == teamSide) {
                        play.offensiveResponseSpeed ?: 0L
                    } else {
                        play.defensiveResponseSpeed ?: 0L
                    }
                }
                .average()

        return if (average.isNaN()) 0.0 else average
    }

    fun calculateQuarterScore(
        play: Play,
        currentQuarterScore: Int,
        possession: TeamSide,
    ): Int {
        if (play.actualResult == ActualResult.DELAY_OF_GAME) {
            if (play.result == Scenario.DELAY_OF_GAME_HOME && possession == TeamSide.AWAY) {
                return currentQuarterScore + 8
            }
            if (play.result == Scenario.DELAY_OF_GAME_AWAY && possession == TeamSide.HOME) {
                return currentQuarterScore + 8
            }
            return currentQuarterScore
        }
        if (play.possession == possession) {
            if (play.actualResult == ActualResult.TOUCHDOWN || play.actualResult == ActualResult.KICKING_TEAM_TOUCHDOWN ||
                play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN
            ) {
                return currentQuarterScore + 6
            }
            if (play.playCall == PlayCall.PAT && play.actualResult == ActualResult.GOOD) {
                return currentQuarterScore + 1
            }
            if (play.playCall == PlayCall.TWO_POINT && play.actualResult == ActualResult.GOOD) {
                return currentQuarterScore + 2
            }
            if (play.playCall == PlayCall.FIELD_GOAL && play.result == Scenario.GOOD) {
                return currentQuarterScore + 3
            }
        }
        if (play.possession != possession) {
            if (play.actualResult == ActualResult.TURNOVER_TOUCHDOWN || play.actualResult == ActualResult.RETURN_TOUCHDOWN ||
                play.actualResult == ActualResult.PUNT_RETURN_TOUCHDOWN || play.actualResult == ActualResult.KICK_SIX
            ) {
                return currentQuarterScore + 6
            }
            if (play.playCall == PlayCall.PAT && play.actualResult == ActualResult.DEFENSE_TWO_POINT) {
                return currentQuarterScore + 1
            }
            if (play.playCall == PlayCall.TWO_POINT && play.actualResult == ActualResult.DEFENSE_TWO_POINT) {
                return currentQuarterScore + 2
            }
            if (play.actualResult == ActualResult.SAFETY) {
                return currentQuarterScore + 2
            }
        }
        return currentQuarterScore
    }

    fun calculatePercentage(
        successes: Int,
        attempts: Int,
    ): Double {
        if (attempts == 0) {
            return 0.0
        }
        return (successes.toDouble() / attempts.toDouble()) * 100
    }
}
