package com.fcfb.arceus.service.fcfb.play

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.fcfb.GameStatsService
import com.fcfb.arceus.service.fcfb.ScorebugService
import org.springframework.stereotype.Component

/**
 * Shared helper functions used across multiple play processors (runoff time calculation,
 * playbook lookup, timeout usage resolution, and persisting the finished play).
 */
@Component
class PlayProcessingUtils(
    private val playRepository: PlayRepository,
    private val gameService: GameService,
    private val gameStatsService: GameStatsService,
    private val scorebugService: ScorebugService,
) {
    fun getRunoffTime(
        game: Game,
        clockStopped: Boolean?,
        clock: Int,
        timeoutUsed: Boolean,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensivePlaybook: OffensivePlaybook,
    ): Int {
        return if (!clockStopped!! && !timeoutUsed) {
            when {
                playCall == PlayCall.SPIKE -> 3
                playCall == PlayCall.KNEEL -> 40
                runoffType == RunoffType.HURRY -> 7
                runoffType == RunoffType.FINAL ->
                    if (clock <= 7) {
                        clock
                    } else if (clock > 30) {
                        30
                    } else {
                        clock - 1
                    }
                runoffType == RunoffType.NORMAL ->
                    when (offensivePlaybook) {
                        OffensivePlaybook.PRO -> 15
                        OffensivePlaybook.AIR_RAID -> 10
                        OffensivePlaybook.FLEXBONE -> 20
                        OffensivePlaybook.SPREAD -> 13
                        OffensivePlaybook.WEST_COAST -> 17
                    }
                runoffType == RunoffType.CHEW -> 30
                game.gameMode == GameMode.CHEW -> 30
                offensivePlaybook == OffensivePlaybook.PRO -> 15
                offensivePlaybook == OffensivePlaybook.AIR_RAID -> 10
                offensivePlaybook == OffensivePlaybook.FLEXBONE -> 20
                offensivePlaybook == OffensivePlaybook.SPREAD -> 13
                offensivePlaybook == OffensivePlaybook.WEST_COAST -> 17
                else -> 0
            }
        } else {
            when {
                playCall == PlayCall.SPIKE -> 1
                else -> 0
            }
        }
    }

    fun getPlaybooks(
        game: Game,
        possession: TeamSide,
    ): Pair<OffensivePlaybook, DefensivePlaybook> {
        return if (possession == TeamSide.HOME) {
            game.homeOffensivePlaybook to game.awayDefensivePlaybook
        } else {
            game.awayOffensivePlaybook to game.homeDefensivePlaybook
        }
    }

    private fun handleEndOfQuarterNormalScenarios(
        game: Game,
        gamePlay: Play,
        actualResult: ActualResult,
        initialPossession: TeamSide,
        initialClock: Int,
        initialQuarter: Int,
        homeScore: Int,
        awayScore: Int,
        playTime: Int,
    ): Triple<TeamSide, Int, Int> {
        var possession = initialPossession
        var clock = initialClock
        var quarter = initialQuarter

        // If quarter is over but game is not over
        if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && quarter < 4) {
            quarter += 1
            clock = 420 - playTime
            if (quarter == 3) {
                possession = gameService.handleHalfTimePossessionChange(game)
                clock = 420
            }
        } else if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && gamePlay.quarter == 4) {
            // Check if game is over or needs to go to OT
            quarter =
                if (homeScore > awayScore || awayScore > homeScore) {
                    0
                } else {
                    5
                }
            clock = 0
        } else if ((clock - playTime) <= 0 && gameService.isTouchdownPlay(actualResult)) {
            clock = 0
            if (quarter == 4 &&
                ((homeScore - awayScore) >= 2 || (awayScore - homeScore) >= 2)
            ) {
                quarter = 0
            }
        } else if (clock > 0) {
            clock = initialClock - playTime
            if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && quarter < 4) {
                quarter += 1
                clock = 420
                if (quarter == 3) {
                    possession = gameService.handleHalfTimePossessionChange(game)
                }
            } else if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && gamePlay.quarter == 4) {
                // Check if game is over or needs to go to OT
                quarter =
                    if (homeScore > awayScore || awayScore > homeScore) {
                        0
                    } else {
                        5
                    }
                clock = 0
            } else if (clock <= 0 && gameService.isTouchdownPlay(actualResult)) {
                clock = 0
                if (quarter == 4 &&
                    ((homeScore - awayScore) >= 2 || (awayScore - homeScore) >= 2)
                ) {
                    quarter = 0
                }
            }
        }
        return Triple(possession, clock, quarter)
    }

    private fun handleEndOfQuarterPATScenarios(
        game: Game,
        gamePlay: Play,
        initialPossession: TeamSide,
        initialClock: Int,
        initialQuarter: Int,
        homeScore: Int,
        awayScore: Int,
    ): Triple<TeamSide, Int, Int> {
        var possession = initialPossession
        var clock = initialClock
        var quarter = initialQuarter

        // If quarter is over but game is not over
        if (clock <= 0 &&
            quarter < 4
        ) {
            quarter += 1
            clock = 420
            if (quarter == 3) {
                possession = gameService.handleHalfTimePossessionChange(game)
            }
        } else if (
            clock <= 0 &&
            gamePlay.quarter == 4
        ) {
            // Check if game is over or needs to go to OT
            quarter =
                if (homeScore > awayScore || awayScore > homeScore) {
                    0
                } else {
                    5
                }
            clock = 0
        }
        return Triple(possession, clock, quarter)
    }

    fun getTimeoutUsage(
        game: Game,
        gamePlay: Play,
        offensiveTimeoutCalled: Boolean,
    ): Triple<Boolean, Boolean, Boolean> {
        val clockStopped = game.clockStopped
        val defensiveTimeoutCalled = gamePlay.timeoutUsed
        var timeoutUsed = false
        var homeTimeoutCalled = false
        var awayTimeoutCalled = false
        if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME && game.awayTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY && game.homeTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME && game.homeTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY && game.awayTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            awayTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME) {
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY) {
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME) {
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY) {
            homeTimeoutCalled = true
        }
        return Triple(timeoutUsed, homeTimeoutCalled, awayTimeoutCalled)
    }

    fun updatePlayValues(
        game: Game,
        gamePlay: Play,
        playCall: PlayCall,
        result: Scenario,
        actualResult: ActualResult,
        initialPossession: TeamSide,
        homeScore: Int,
        awayScore: Int,
        runoffTime: Int,
        playTime: Int,
        ballLocation: Int,
        down: Int,
        yardsToGo: Int,
        decryptedDefensiveNumber: String,
        offensiveNumber: Int?,
        difference: Int?,
        yards: Int,
        timeoutUsed: Boolean,
        homeTimeoutCalled: Boolean,
        awayTimeoutcalled: Boolean,
    ): Play {
        var clock = gamePlay.clock.minus(runoffTime)
        var quarter = gamePlay.quarter
        var possession = initialPossession
        var finalPlayTime = playTime

        if (playCall != PlayCall.PAT && playCall != PlayCall.TWO_POINT) {
            val (updatedPossession, updatedClock, updatedQuarter) =
                handleEndOfQuarterNormalScenarios(
                    game,
                    gamePlay,
                    actualResult,
                    possession,
                    clock,
                    quarter,
                    homeScore,
                    awayScore,
                    playTime,
                )
            if (updatedQuarter != quarter) {
                finalPlayTime = clock
            }
            possession = updatedPossession
            clock = updatedClock
            quarter = updatedQuarter
        } else {
            // Handle end of quarter PAT scenarios
            val (updatedPossession, updatedClock, updatedQuarter) =
                handleEndOfQuarterPATScenarios(
                    game,
                    gamePlay,
                    possession,
                    clock,
                    quarter,
                    homeScore,
                    awayScore,
                )
            possession = updatedPossession
            clock = updatedClock
            quarter = updatedQuarter
        }

        // Update gamePlay values
        if (playCall == PlayCall.SPIKE || playCall == PlayCall.KNEEL) {
            gamePlay.defensiveNumber = null
            gamePlay.offensiveNumber = null
        } else {
            gamePlay.defensiveNumber = decryptedDefensiveNumber
            gamePlay.offensiveNumber = offensiveNumber?.toString()
        }
        gamePlay.playCall = playCall
        gamePlay.result = result
        gamePlay.actualResult = actualResult
        gamePlay.yards = yards
        gamePlay.playTime = finalPlayTime
        gamePlay.runoffTime = runoffTime
        gamePlay.difference = difference
        gamePlay.timeoutUsed = timeoutUsed
        gamePlay.homeScore = homeScore
        gamePlay.awayScore = awayScore
        gamePlay.playFinished = true

        if (initialPossession == TeamSide.HOME) {
            gamePlay.offensiveTimeoutCalled = homeTimeoutCalled
            gamePlay.defensiveTimeoutCalled = awayTimeoutcalled
        } else {
            gamePlay.offensiveTimeoutCalled = awayTimeoutcalled
            gamePlay.defensiveTimeoutCalled = homeTimeoutCalled
        }

        // Save the play and then update the game and stats
        playRepository.save(gamePlay)

        val game =
            gameService.updateGameInformation(
                game,
                gamePlay,
                homeScore,
                awayScore,
                possession,
                quarter,
                clock,
                ballLocation,
                down,
                yardsToGo,
                homeTimeoutCalled,
                awayTimeoutcalled,
                timeoutUsed,
            )

        scorebugService.generateScorebug(game)

        val allPlays = playRepository.getAllPlaysByGameId(game.gameId)
        gameStatsService.updateGameStats(
            game,
            allPlays,
        )

        return gamePlay
    }
}
