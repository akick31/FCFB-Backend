package com.fcfb.arceus.service.fcfb.probability

import com.fcfb.arceus.domain.elo.EloCalculator
import com.fcfb.arceus.domain.probability.WinProbabilityCalculator
import com.fcfb.arceus.domain.probability.WinProbabilityState
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.util.ml.XGBoostPredictor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WinProbabilityService(
    private val xgboostPredictor: XGBoostPredictor,
) {
    private val eloCalculator = EloCalculator()
    private val winProbabilityCalculator = WinProbabilityCalculator(xgboostPredictor)
    private val logger = LoggerFactory.getLogger(WinProbabilityService::class.java)

    fun homeWinProbability(
        game: Game,
        state: WinProbabilityState,
        homeElo: Double,
        awayElo: Double,
    ): Double = WinProbabilityState.sidePerspective(offenseWinProbability(game, state, homeElo, awayElo), state.possession)

    fun recordPlayResult(
        game: Game,
        play: Play,
        previousHomeWinProbability: Double,
        homeElo: Double,
        awayElo: Double,
    ) {
        val homeAfter =
            if (game.gameStatus == GameStatus.FINAL) {
                finalHomeWinProbability(game)
            } else {
                homeWinProbability(game, WinProbabilityState.afterPlay(game), homeElo, awayElo)
            }
        applyToPlay(play, homeAfter, previousHomeWinProbability)
        game.winProbability = WinProbabilityState.sidePerspective(homeAfter, game.possession)
    }

    fun applyToPlay(
        play: Play,
        homeAfter: Double,
        previousHomeWinProbability: Double,
    ) {
        play.winProbability = WinProbabilityState.sidePerspective(homeAfter, play.possession)
        play.winProbabilityAdded =
            WinProbabilityState.sidePerspective(homeAfter, play.possession) -
            WinProbabilityState.sidePerspective(previousHomeWinProbability, play.possession)
    }

    fun finalHomeWinProbability(game: Game) = if (game.homeScore > game.awayScore) 1.0 else 0.0

    private fun offenseWinProbability(
        game: Game,
        state: WinProbabilityState,
        homeElo: Double,
        awayElo: Double,
    ): Double =
        try {
            predict(game, state, homeElo, awayElo)
        } catch (e: Exception) {
            logger.error("Error calculating win probability for game ${game.gameId}: ${e.message}", e)
            DEFAULT_WIN_PROBABILITY
        }

    private fun predict(
        game: Game,
        state: WinProbabilityState,
        homeElo: Double,
        awayElo: Double,
    ): Double {
        val homeOffense = state.possession == TeamSide.HOME
        val timeRemaining = winProbabilityCalculator.calculateTimeRemaining(state.quarter, state.clock).toInt()
        val secondsLeftHalf = winProbabilityCalculator.calculateSecondsLeftInHalf(state.quarter, state.clock)
        val half = if (state.quarter <= 2) 1 else 2
        val hadFirstPossession = winProbabilityCalculator.resolveFirstPossession(game, state.possession)
        val eloDiffTime =
            if (homeOffense) {
                winProbabilityCalculator.calculateEloDiffTime(homeElo, awayElo, timeRemaining)
            } else {
                winProbabilityCalculator.calculateEloDiffTime(awayElo, homeElo, timeRemaining)
            }
        val margin = if (homeOffense) state.homeScore - state.awayScore else state.awayScore - state.homeScore
        return when (state.playType) {
            PlayType.PAT ->
                winProbabilityCalculator.calculatePatWinProbability(
                    margin,
                    timeRemaining,
                    secondsLeftHalf,
                    half,
                    hadFirstPossession,
                    eloDiffTime,
                )
            PlayType.KICKOFF ->
                winProbabilityCalculator.calculateKickoffWinProbability(
                    margin,
                    timeRemaining,
                    secondsLeftHalf,
                    half,
                    hadFirstPossession,
                    eloDiffTime,
                )
            PlayType.NORMAL ->
                xgboostPredictor.predict(
                    xgboostPredictor.createFeatureArray(
                        down = state.down,
                        distance = state.yardsToGo,
                        position = 100 - state.ballLocation,
                        margin = margin,
                        secondsLeftGame = timeRemaining,
                        secondsLeftHalf = secondsLeftHalf,
                        half = half,
                        hadFirstPossession = hadFirstPossession,
                        eloDiffTime = eloDiffTime,
                    ),
                )
        }
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

    companion object {
        private const val DEFAULT_WIN_PROBABILITY = 0.5
    }
}
