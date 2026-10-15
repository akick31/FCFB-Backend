package com.fcfb.arceus.service.fcfb.probability

import com.fcfb.arceus.domain.elo.EloCalculator
import com.fcfb.arceus.domain.probability.WinProbabilityCalculator
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.util.ml.XGBoostPredictor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WinProbabilityService(
    private val xgboostPredictor: XGBoostPredictor,
    private val playRepository: PlayRepository,
) {
    private val eloCalculator = EloCalculator()
    private val winProbabilityCalculator = WinProbabilityCalculator(xgboostPredictor)
    private val logger = LoggerFactory.getLogger(WinProbabilityService::class.java)

    fun calculateWinProbability(
        game: Game,
        play: Play,
        homeElo: Double,
        awayElo: Double,
    ): Double {
        try {
            val scoreDiff = play.homeScore - play.awayScore
            val timeRemaining = winProbabilityCalculator.calculateTimeRemaining(play.quarter, play.clock).toInt()
            val down = play.down
            val yardsToGo = play.yardsToGo
            val ballLocation = 100 - play.ballLocation

            val secondsLeftGame = timeRemaining
            val secondsLeftHalf = winProbabilityCalculator.calculateSecondsLeftInHalf(play.quarter, play.clock)
            val half = if (play.quarter <= 2) 1 else 2
            val hadFirstPossession = winProbabilityCalculator.resolveFirstPossession(game, play.possession)
            val eloDiffTime =
                if (play.possession == TeamSide.HOME) {
                    winProbabilityCalculator.calculateEloDiffTime(homeElo, awayElo, secondsLeftGame)
                } else {
                    winProbabilityCalculator.calculateEloDiffTime(awayElo, homeElo, secondsLeftGame)
                }

            val margin =
                if (play.possession == TeamSide.HOME) {
                    scoreDiff
                } else {
                    -scoreDiff
                }

            val features =
                xgboostPredictor.createFeatureArray(
                    down = down,
                    distance = yardsToGo,
                    position = ballLocation,
                    margin = margin,
                    secondsLeftGame = secondsLeftGame,
                    secondsLeftHalf = secondsLeftHalf,
                    half = half,
                    hadFirstPossession = hadFirstPossession,
                    eloDiffTime = eloDiffTime,
                )

            val winProbability =
                when (play.playCall) {
                    PlayCall.PAT ->
                        winProbabilityCalculator.calculatePatWinProbability(
                            margin,
                            timeRemaining,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    PlayCall.TWO_POINT ->
                        winProbabilityCalculator.calculateTwoPointWinProbability(
                            margin,
                            timeRemaining,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    PlayCall.KICKOFF_NORMAL ->
                        winProbabilityCalculator.calculateKickoffWinProbability(
                            margin,
                            timeRemaining,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    PlayCall.KICKOFF_ONSIDE ->
                        winProbabilityCalculator.calculateKickoffOnsideWinProbability(
                            margin,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    PlayCall.KICKOFF_SQUIB ->
                        winProbabilityCalculator.calculateKickoffSquibWinProbability(
                            margin,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    else -> xgboostPredictor.predict(features)
                }

            val winProbabilityAdded = calculateWinProbabilityAdded(game, play, winProbability)

            play.winProbability = winProbability
            play.winProbabilityAdded = winProbabilityAdded

            return winProbability
        } catch (e: Exception) {
            logger.error("Error calculating win probability: ${e.message}", e)
            val defaultProbability = 0.5
            play.winProbability = defaultProbability
            play.winProbabilityAdded = 0.0
            return defaultProbability
        }
    }

    private fun calculateWinProbabilityAdded(
        game: Game,
        play: Play,
        currentWinProbability: Double,
    ): Double {
        val previousWinProbability = game.winProbability ?: 0.5

        val previousPlay =
            try {
                val allPlays = playRepository.getAllPlaysByGameId(game.gameId)
                allPlays.find { it.playNumber == play.playNumber - 1 }
            } catch (e: Exception) {
                logger.error("Error looking up previous play for game ${game.gameId}: ${e.message}", e)
                null
            }

        return winProbabilityCalculator.calculateWinProbabilityAdded(
            currentWinProbability = currentWinProbability,
            previousWinProbability = previousWinProbability,
            currentPossession = play.possession,
            previousPlayWinProbability = previousPlay?.winProbability,
            previousPlayPossession = previousPlay?.possession,
        )
    }

    fun updateEloRatings(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ) {
        val (newHomeElo, newAwayElo) =
            eloCalculator.updatedRatings(homeTeam.currentElo, awayTeam.currentElo, game.homeScore > game.awayScore)
        logger.info(
            "Game ${game.gameId} ELO: ${game.homeTeam} ${homeTeam.currentElo} -> $newHomeElo, " +
                "${game.awayTeam} ${awayTeam.currentElo} -> $newAwayElo",
        )
        homeTeam.currentElo = newHomeElo
        awayTeam.currentElo = newAwayElo
    }
}
