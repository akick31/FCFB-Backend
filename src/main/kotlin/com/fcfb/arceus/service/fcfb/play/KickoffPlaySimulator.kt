package com.fcfb.arceus.service.fcfb.play

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
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
 * Simulates a kickoff play.
 */
@Component
class KickoffPlaySimulator(
    private val gameService: GameService,
    private val rangesService: RangesService,
    private val playSimulationUtils: PlaySimulationUtils,
) {
    fun runKickoffPlay(
        gamePlay: Play,
        game: Game,
        playCall: PlayCall,
        offensiveNumber: Int?,
        decryptedDefensiveNumber: String,
    ): Play {
        if (game.currentPlayType != PlayType.KICKOFF) {
            throw InvalidPlayTypeException()
        }

        if (offensiveNumber == null) {
            throw NumberNotFoundException()
        }

        val difference = gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
        var possession = gamePlay.possession
        val resultInformation = rangesService.getNonNormalResult(playCall, difference)
        val result = resultInformation.result ?: throw ResultNotFoundException()
        var homeScore = game.homeScore
        var awayScore = game.awayScore
        val ballLocation: Int
        val down = 1
        val yardsToGo = 10
        val playTime = resultInformation.playTime
        val actualResult: ActualResult?
        when (result) {
            Scenario.TOUCHDOWN -> {
                actualResult = ActualResult.KICKING_TEAM_TOUCHDOWN
                ballLocation = 97
            }
            Scenario.FUMBLE -> {
                actualResult = ActualResult.MUFFED_KICK
                ballLocation = 75
            }
            Scenario.FIVE_YARD_RETURN, Scenario.TEN_YARD_RETURN, Scenario.TWENTY_YARD_RETURN, Scenario.THIRTY_YARD_RETURN,
            Scenario.THIRTY_FIVE_YARD_RETURN, Scenario.FORTY_YARD_RETURN, Scenario.FORTY_FIVE_YARD_RETURN,
            Scenario.FIFTY_YARD_RETURN, Scenario.SIXTY_FIVE_YARD_RETURN,
            -> {
                actualResult = ActualResult.KICKOFF
                ballLocation = result.description.substringBefore(" YARD RETURN").toInt()
            }
            Scenario.TOUCHBACK -> {
                actualResult = ActualResult.KICKOFF
                ballLocation = 25
            }
            Scenario.RETURN_TOUCHDOWN -> {
                actualResult = ActualResult.RETURN_TOUCHDOWN
                ballLocation = 97
            }
            Scenario.RECOVERED -> {
                actualResult = ActualResult.SUCCESSFUL_ONSIDE
                ballLocation = 45
            }
            Scenario.FAILED_ONSIDE -> {
                actualResult = ActualResult.FAILED_ONSIDE
                ballLocation = 55
            }
            else -> throw InvalidScenarioException()
        }
        when (actualResult) {
            ActualResult.KICKING_TEAM_TOUCHDOWN ->
                if (possession == TeamSide.HOME) {
                    homeScore += 6
                } else {
                    awayScore += 6
                }
            ActualResult.KICKOFF, ActualResult.FAILED_ONSIDE ->
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
            ActualResult.RETURN_TOUCHDOWN ->
                if (possession == TeamSide.HOME) {
                    possession = TeamSide.AWAY
                    awayScore += 6
                } else {
                    possession = TeamSide.HOME
                    homeScore += 6
                }
            ActualResult.SUCCESSFUL_ONSIDE, ActualResult.MUFFED_KICK ->
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.HOME
                    } else {
                        TeamSide.AWAY
                    }
            else -> throw InvalidActualResultException()
        }

        return playSimulationUtils.updatePlayValues(
            game,
            gamePlay,
            playCall,
            result,
            actualResult,
            possession,
            homeScore,
            awayScore,
            0,
            playTime,
            ballLocation,
            down,
            yardsToGo,
            decryptedDefensiveNumber,
            offensiveNumber,
            difference,
            0,
            false,
            false,
            false,
        )
    }
}
