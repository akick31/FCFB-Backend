package com.fcfb.arceus.service.fcfb.play

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Ranges
import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.fcfb.RangesService
import com.fcfb.arceus.util.InvalidActualResultException
import com.fcfb.arceus.util.InvalidPlayTypeException
import com.fcfb.arceus.util.NumberNotFoundException
import com.fcfb.arceus.util.ResultNotFoundException
import org.springframework.stereotype.Component

/**
 * Processes a normal (run/pass/spike/kneel) play.
 */
@Component
class NormalPlayProcessor(
    private val gameService: GameService,
    private val rangesService: RangesService,
    private val playProcessingUtils: PlayProcessingUtils,
) {
    fun runNormalPlay(
        gamePlay: Play,
        game: Game,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensiveTimeoutCalled: Boolean,
        offensiveNumber: Int?,
        decryptedDefensiveNumber: String,
    ): Play {
        if (game.currentPlayType != PlayType.NORMAL) {
            throw InvalidPlayTypeException()
        }

        val difference =
            if (offensiveNumber != null) {
                gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
            } else {
                null
            }
        var possession = gamePlay.possession
        val (offensivePlaybook, defensivePlaybook) = playProcessingUtils.getPlaybooks(game, possession)
        val (timeoutUsed, homeTimeoutCalled, awayTimeoutCalled) =
            playProcessingUtils.getTimeoutUsage(game, gamePlay, offensiveTimeoutCalled)

        val resultInformation =
            if (difference != null) {
                rangesService.getNormalResult(playCall, offensivePlaybook, defensivePlaybook, difference)
            } else {
                when (playCall) {
                    PlayCall.SPIKE -> {
                        Ranges(
                            PlayType.NORMAL.description,
                            offensivePlaybook.description,
                            defensivePlaybook.description,
                            0,
                            0,
                            0,
                            Scenario.SPIKE,
                            0,
                            0,
                            0,
                        )
                    }

                    PlayCall.KNEEL -> {
                        Ranges(
                            PlayType.NORMAL.description,
                            offensivePlaybook.description,
                            defensivePlaybook.description,
                            0,
                            0,
                            0,
                            Scenario.KNEEL,
                            1,
                            0,
                            0,
                        )
                    }

                    else -> throw NumberNotFoundException()
                }
            }
        var result = resultInformation.result ?: throw ResultNotFoundException()
        var playTime = resultInformation.playTime

        // Determine runoff time between plays
        val clockStopped = game.clockStopped
        val runoffTime =
            playProcessingUtils.getRunoffTime(
                game,
                clockStopped,
                gamePlay.clock,
                timeoutUsed,
                playCall,
                runoffType,
                offensivePlaybook,
            )
        if (gamePlay.clock - runoffTime < 0 && (game.quarter == 2 || game.quarter == 4)) {
            result = Scenario.END_OF_HALF
        }

        var homeScore = game.homeScore
        var awayScore = game.awayScore
        var ballLocation = game.ballLocation
        var down = game.down
        var yardsToGo = game.yardsToGo
        var yards = 0
        var actualResult: ActualResult
        when (result) {
            Scenario.TURNOVER_TOUCHDOWN -> actualResult = ActualResult.TURNOVER_TOUCHDOWN
            Scenario.TURNOVER_PLUS_20_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation + 20
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TURNOVER_TOUCHDOWN
                }
            }
            Scenario.TURNOVER_PLUS_15_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation + 15
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TURNOVER_TOUCHDOWN
                }
            }
            Scenario.TURNOVER_PLUS_10_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation + 10
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TURNOVER_TOUCHDOWN
                }
            }
            Scenario.TURNOVER_PLUS_5_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation + 5
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TURNOVER_TOUCHDOWN
                }
            }
            Scenario.TURNOVER -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation
            }
            Scenario.TURNOVER_MINUS_5_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation - 5
                if (ballLocation <= 0) {
                    ballLocation = 20
                }
            }
            Scenario.TURNOVER_MINUS_10_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation - 10
                if (ballLocation <= 0) {
                    ballLocation = 20
                }
            }
            Scenario.TURNOVER_MINUS_15_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation - 15
                if (ballLocation <= 0) {
                    ballLocation = 20
                }
            }
            Scenario.TURNOVER_MINUS_20_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation - 20
                if (ballLocation <= 0) {
                    ballLocation = 20
                }
            }
            Scenario.NO_GAIN, Scenario.INCOMPLETE -> {
                actualResult = ActualResult.NO_GAIN
                down += 1
                if (down > 4) {
                    actualResult = ActualResult.TURNOVER_ON_DOWNS
                    ballLocation = 100 - ballLocation
                }
            }
            Scenario.TOUCHDOWN -> {
                yards = 100 - ballLocation
                actualResult = ActualResult.TOUCHDOWN
                playTime = rangesService.getPlayTime(playCall, yards)
            }
            Scenario.SPIKE -> {
                actualResult = ActualResult.SPIKE
                down += 1
                if (down > 4) {
                    actualResult = ActualResult.TURNOVER_ON_DOWNS
                    ballLocation = 100 - ballLocation
                }
            }
            Scenario.KNEEL -> {
                yards = -2
                actualResult = ActualResult.KNEEL
                ballLocation -= 2
                if (ballLocation <= 0) {
                    ballLocation = 1
                }
                yardsToGo -= yards
                down += 1
                if (down > 4) {
                    actualResult = ActualResult.TURNOVER_ON_DOWNS
                    ballLocation = 100 - ballLocation
                }
            }
            Scenario.END_OF_HALF -> {
                actualResult = ActualResult.END_OF_HALF
            }
            else -> {
                yards = result.description.toInt()
                val originalBallLocation = ballLocation
                ballLocation += yards
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TOUCHDOWN
                    yards = 100 - originalBallLocation
                    playTime = rangesService.getPlayTime(playCall, yards)
                } else if (ballLocation <= 0) {
                    actualResult = ActualResult.SAFETY
                    yards = 0 - originalBallLocation
                } else if (yards >= yardsToGo) {
                    down = 1
                    yardsToGo = 10
                    actualResult = ActualResult.FIRST_DOWN
                } else {
                    down += 1
                    if (down > 4) {
                        actualResult = ActualResult.TURNOVER_ON_DOWNS
                        ballLocation = 100 - ballLocation
                    } else {
                        yardsToGo -= yards
                        actualResult =
                            if (yards > 0) {
                                ActualResult.GAIN
                            } else if (yards == 0) {
                                ActualResult.NO_GAIN
                            } else {
                                ActualResult.LOSS
                            }
                    }
                }
            }
        }
        when (actualResult) {
            ActualResult.TURNOVER_ON_DOWNS, ActualResult.TURNOVER -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
                down = 1
                yardsToGo = 10
            }
            ActualResult.SAFETY -> {
                ballLocation = 20
                if (possession == TeamSide.HOME) {
                    awayScore += 2
                } else {
                    homeScore += 2
                }
            }
            ActualResult.TOUCHDOWN -> {
                ballLocation = 97
                if (possession == TeamSide.HOME) {
                    homeScore += 6
                } else {
                    awayScore += 6
                }
            }
            ActualResult.TURNOVER_TOUCHDOWN -> {
                ballLocation = 97
                if (possession == TeamSide.HOME) {
                    awayScore += 6
                    possession = TeamSide.AWAY
                } else {
                    homeScore += 6
                    possession = TeamSide.HOME
                }
            }
            ActualResult.END_OF_HALF -> {}
            ActualResult.FIRST_DOWN -> {}
            ActualResult.GAIN -> {}
            ActualResult.NO_GAIN -> {}
            ActualResult.SPIKE -> {}
            ActualResult.KNEEL -> {}
            ActualResult.LOSS -> {}
            else -> throw InvalidActualResultException()
        }

        return playProcessingUtils.updatePlayValues(
            game,
            gamePlay,
            playCall,
            result,
            actualResult,
            possession,
            homeScore,
            awayScore,
            runoffTime,
            playTime,
            ballLocation,
            down,
            yardsToGo,
            decryptedDefensiveNumber,
            offensiveNumber,
            difference,
            yards,
            timeoutUsed,
            homeTimeoutCalled,
            awayTimeoutCalled,
        )
    }
}
