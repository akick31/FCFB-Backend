package com.fcfb.arceus.service.fcfb.play

import com.fcfb.arceus.domain.play.PlayResolutions
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.game.InvalidScenarioException
import com.fcfb.arceus.exception.play.InvalidActualResultException
import com.fcfb.arceus.exception.play.InvalidPlayTypeException
import com.fcfb.arceus.exception.play.NumberNotFoundException
import com.fcfb.arceus.exception.play.ResultNotFoundException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.service.fcfb.game.GameService
import org.springframework.stereotype.Component

@Component
class PuntPlayProcessorService(
    private val gameService: GameService,
    private val rangesService: RangesService,
    private val playUpdateService: PlayUpdateService,
) {
    private val playResolutions = PlayResolutions()

    fun runPuntPlay(
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
        var ballLocation = game.ballLocation
        val (offensivePlaybook, _) = playResolutions.resolvePlaybooks(game, possession)
        val (timeoutUsed, homeTimeoutCalled, awayTimeoutCalled) =
            playResolutions.resolveTimeoutAttribution(game, gamePlay, offensiveTimeoutCalled)

        val resultInformation = rangesService.getPuntResult(playCall, ballLocation, difference)
        var result = resultInformation.result ?: throw ResultNotFoundException()
        val playTime = resultInformation.playTime

        val clockStopped = game.clockStopped
        val runoffTime =
            playResolutions.resolveRunoffTime(
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
        var down = game.down
        var yardsToGo = game.yardsToGo
        val actualResult: ActualResult
        when (result) {
            Scenario.PUNT_RETURN_TOUCHDOWN -> {
                actualResult = ActualResult.PUNT_RETURN_TOUCHDOWN
                ballLocation = 97
            }
            Scenario.BLOCKED_PUNT -> {
                actualResult = ActualResult.BLOCKED
                ballLocation = 100 - ballLocation
            }
            Scenario.TOUCHBACK -> {
                actualResult = ActualResult.PUNT
                ballLocation = 20
            }
            Scenario.FIVE_YARD_PUNT, Scenario.TEN_YARD_PUNT, Scenario.FIFTEEN_YARD_PUNT, Scenario.TWENTY_YARD_PUNT,
            Scenario.TWENTY_FIVE_YARD_PUNT, Scenario.THIRTY_YARD_PUNT, Scenario.THIRTY_FIVE_YARD_PUNT,
            Scenario.FORTY_YARD_PUNT, Scenario.FORTY_FIVE_YARD_PUNT, Scenario.FIFTY_YARD_PUNT, Scenario.FIFTY_FIVE_YARD_PUNT,
            Scenario.SIXTY_YARD_PUNT, Scenario.SIXTY_FIVE_YARD_PUNT, Scenario.SEVENTY_YARD_PUNT,
            -> {
                actualResult = ActualResult.PUNT
                ballLocation = 100 - (ballLocation + result.description.substringBefore(" YARD PUNT").toInt())
            }
            Scenario.FUMBLE -> {
                actualResult = ActualResult.MUFFED_PUNT
                ballLocation += 40
                if (ballLocation >= 100) {
                    ballLocation = 99
                }
            }
            Scenario.TOUCHDOWN -> {
                actualResult = ActualResult.PUNT_TEAM_TOUCHDOWN
                ballLocation = 97
            }
            Scenario.END_OF_HALF -> {
                actualResult = ActualResult.END_OF_HALF
            }
            else -> throw InvalidScenarioException()
        }
        when (actualResult) {
            ActualResult.PUNT_RETURN_TOUCHDOWN -> {
                if (possession == TeamSide.HOME) {
                    possession = TeamSide.AWAY
                    awayScore += 6
                } else {
                    possession = TeamSide.HOME
                    homeScore += 6
                }
                down = 1
                yardsToGo = 10
            }
            ActualResult.BLOCKED -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
                down = 1
                yardsToGo = 10
            }
            ActualResult.PUNT -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
                down = 1
                yardsToGo = 10
            }
            ActualResult.MUFFED_PUNT -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.HOME
                    } else {
                        TeamSide.AWAY
                    }
                down = 1
                yardsToGo = 10
            }
            ActualResult.PUNT_TEAM_TOUCHDOWN -> {
                if (possession == TeamSide.HOME) {
                    homeScore += 6
                    possession = TeamSide.HOME
                } else {
                    awayScore += 6
                    possession = TeamSide.AWAY
                }
            }
            ActualResult.END_OF_HALF -> {}
            else -> throw InvalidActualResultException()
        }

        return playUpdateService.updatePlayValues(
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
