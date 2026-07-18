package com.fcfb.arceus.service.fcfb.play

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.fcfb.RangesService
import com.fcfb.arceus.util.InvalidActualResultException
import com.fcfb.arceus.util.InvalidPlayTypeException
import com.fcfb.arceus.util.InvalidScenarioException
import com.fcfb.arceus.util.NumberNotFoundException
import com.fcfb.arceus.util.ResultNotFoundException
import org.springframework.stereotype.Component

/**
 * Processes a field goal play.
 */
@Component
class FieldGoalPlayProcessor(
    private val gameService: GameService,
    private val rangesService: RangesService,
    private val playProcessingUtils: PlayProcessingUtils,
) {
    fun runFieldGoalPlay(
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

        if (offensiveNumber == null) {
            throw NumberNotFoundException()
        }

        val difference = gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
        var possession = gamePlay.possession
        var ballLocation = 100 - game.ballLocation
        val (offensivePlaybook, _) = playProcessingUtils.getPlaybooks(game, possession)
        val (timeoutUsed, homeTimeoutCalled, awayTimeoutCalled) =
            playProcessingUtils.getTimeoutUsage(game, gamePlay, offensiveTimeoutCalled)

        val resultInformation = rangesService.getFieldGoalResult(playCall, ballLocation + 17, difference)
        var result = resultInformation.result ?: throw ResultNotFoundException()
        val playTime = resultInformation.playTime
        ballLocation = game.ballLocation

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
        val down = 1
        val yardsToGo = 10
        val actualResult: ActualResult
        when (result) {
            Scenario.KICK_SIX -> {
                actualResult = ActualResult.KICK_SIX
                ballLocation = 97
            }
            Scenario.BLOCKED_FIELD_GOAL -> {
                actualResult = ActualResult.BLOCKED
                ballLocation = 100 - ballLocation
            }
            Scenario.NO_GOOD -> {
                actualResult = ActualResult.NO_GOOD
                ballLocation = 100 - ballLocation
            }
            Scenario.GOOD -> {
                actualResult = ActualResult.GOOD
                ballLocation = 35
            }
            Scenario.END_OF_HALF -> {
                actualResult = ActualResult.END_OF_HALF
            }
            else -> throw InvalidScenarioException()
        }
        when (actualResult) {
            ActualResult.KICK_SIX -> {
                if (possession == TeamSide.HOME) {
                    possession = TeamSide.AWAY
                    awayScore += 6
                } else {
                    possession = TeamSide.HOME
                    homeScore += 6
                }
            }
            ActualResult.BLOCKED -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
            }
            ActualResult.NO_GOOD -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
            }
            ActualResult.GOOD -> {
                if (possession == TeamSide.HOME) {
                    homeScore += 3
                } else {
                    awayScore += 3
                }
            }
            ActualResult.END_OF_HALF -> {}
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
            0,
            timeoutUsed,
            homeTimeoutCalled,
            awayTimeoutCalled,
        )
    }
}
