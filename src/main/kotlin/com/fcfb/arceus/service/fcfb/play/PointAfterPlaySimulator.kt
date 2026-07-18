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
 * Simulates a point-after (PAT / two-point) play.
 */
@Component
class PointAfterPlaySimulator(
    private val gameService: GameService,
    private val rangesService: RangesService,
    private val playSimulationUtils: PlaySimulationUtils,
) {
    fun runPointAfterPlay(
        gamePlay: Play,
        game: Game,
        playCall: PlayCall,
        offensiveNumber: Int?,
        decryptedDefensiveNumber: String,
    ): Play {
        if (game.currentPlayType != PlayType.PAT) {
            throw InvalidPlayTypeException()
        }

        if (offensiveNumber == null) {
            throw NumberNotFoundException()
        }

        val difference = gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
        val possession = gamePlay.possession
        val resultInformation = rangesService.getNonNormalResult(playCall, difference)
        val result = resultInformation.result ?: throw ResultNotFoundException()
        var homeScore = game.homeScore
        var awayScore = game.awayScore
        val ballLocation = 35
        val down = 1
        val yardsToGo = 10
        val actualResult =
            when (result) {
                Scenario.GOOD -> ActualResult.GOOD
                Scenario.NO_GOOD -> ActualResult.NO_GOOD
                Scenario.SUCCESS -> ActualResult.SUCCESS
                Scenario.FAILED -> ActualResult.FAILED
                Scenario.DEFENSE_TWO_POINT -> ActualResult.DEFENSE_TWO_POINT
                else -> throw InvalidScenarioException()
            }
        when (actualResult) {
            ActualResult.GOOD -> {
                if (possession == TeamSide.HOME) {
                    homeScore += 1
                } else {
                    awayScore += 1
                }
            }
            ActualResult.NO_GOOD -> {}
            ActualResult.SUCCESS ->
                if (possession == TeamSide.HOME) {
                    homeScore += 2
                } else {
                    awayScore += 2
                }
            ActualResult.FAILED -> {}
            ActualResult.DEFENSE_TWO_POINT -> {
                if (possession == TeamSide.HOME) {
                    awayScore += 2
                } else {
                    homeScore += 2
                }
            }
            else -> {
                throw InvalidActualResultException()
            }
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
            0,
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
