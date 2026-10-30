package com.fcfb.arceus.domain.probability

import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.ml.XGBoostPredictor

class WinProbabilityCalculator(private val xgboostPredictor: XGBoostPredictor) {
    private val logger = Logger()

    fun calculateTimeRemaining(
        quarter: Int,
        clock: Int,
    ): Double {
        val timeRemaining =
            when (quarter) {
                1 -> 1680 - (420 - clock)
                2 -> 1260 - (420 - clock)
                3 -> 840 - (420 - clock)
                4 -> 420 - (420 - clock)
                else -> 0.0
            }
        return maxOf(0.0, timeRemaining.toDouble())
    }

    fun calculateSecondsLeftInHalf(
        quarter: Int,
        clock: Int,
    ): Int {
        val secondsLeftHalf =
            when (quarter) {
                1 -> 840 - (420 - clock)
                2 -> 420 - (420 - clock)
                3 -> 840 - (420 - clock)
                4 -> 420 - (420 - clock)
                else -> 0
            }
        return maxOf(0, secondsLeftHalf)
    }

    fun calculateEloDiffTime(
        offenseElo: Double,
        defenseElo: Double,
        secondsLeftGame: Int,
    ): Double = (offenseElo - defenseElo) * Math.exp(-2.0 * (1.0 - (secondsLeftGame.toDouble() / 1680.0)))

    fun resolveFirstPossession(
        game: Game,
        currentPossession: TeamSide,
    ): Int {
        val coinTossWinner = game.coinTossWinner
        val coinTossChoice = game.coinTossChoice

        if (coinTossWinner == null || coinTossChoice == null) {
            logger.warn("Missing coin toss data for game ${game.gameId}. Using default: HOME had first possession")
            return if (currentPossession == TeamSide.HOME) 1 else 0
        }

        val whoHadFirstPossession =
            when {
                coinTossWinner == TeamSide.AWAY && coinTossChoice == CoinTossChoice.DEFER -> TeamSide.HOME
                coinTossWinner == TeamSide.HOME && coinTossChoice == CoinTossChoice.DEFER -> TeamSide.AWAY
                coinTossWinner == TeamSide.AWAY && coinTossChoice == CoinTossChoice.RECEIVE -> TeamSide.AWAY
                coinTossWinner == TeamSide.HOME && coinTossChoice == CoinTossChoice.RECEIVE -> TeamSide.HOME
                else -> {
                    logger.warn("Unexpected coin toss combination for game ${game.gameId}: winner=$coinTossWinner, choice=$coinTossChoice")
                    TeamSide.HOME
                }
            }

        return if (currentPossession == whoHadFirstPossession) 1 else 0
    }

    fun calculatePatWinProbability(
        margin: Int,
        timeRemaining: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double {
        val probIfSuccess =
            calculateWinProbabilityForScenario(
                75,
                -(margin + 1),
                timeRemaining,
                secondsLeftHalf,
                half,
                hadFirstPossession,
                -eloDiffTime,
            )
        val probIfFail =
            calculateWinProbabilityForScenario(
                75,
                -margin,
                timeRemaining,
                secondsLeftHalf,
                half,
                1 - hadFirstPossession,
                -eloDiffTime,
            )
        val probIfReturn =
            calculateWinProbabilityForScenario(
                75,
                -(margin - 2),
                timeRemaining,
                secondsLeftHalf,
                half,
                1 - hadFirstPossession,
                -eloDiffTime,
            )

        return 1.0 - (
            ((721.0 / 751.0) * probIfSuccess) +
                ((27.0 / 751.0) * probIfFail) +
                ((3.0 / 751.0) * probIfReturn)
        )
    }

    fun calculateKickoffWinProbability(
        margin: Int,
        timeRemaining: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double =
        1.0 -
            calculateWinProbabilityForScenario(
                75, -margin, timeRemaining, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
            )

    private fun calculateWinProbabilityForScenario(
        position: Int,
        margin: Int,
        secondsLeftGame: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double {
        val features =
            xgboostPredictor.createFeatureArray(
                down = 1,
                distance = 10,
                position = position,
                margin = margin,
                secondsLeftGame = secondsLeftGame,
                secondsLeftHalf = secondsLeftHalf,
                half = half,
                hadFirstPossession = hadFirstPossession,
                eloDiffTime = eloDiffTime,
            )
        return xgboostPredictor.predict(features)
    }
}
