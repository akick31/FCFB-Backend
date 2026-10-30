package com.fcfb.arceus.domain.probability

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.util.game.convertClockToSeconds

data class WinProbabilityState(
    val possession: TeamSide,
    val quarter: Int,
    val clock: Int,
    val ballLocation: Int,
    val down: Int,
    val yardsToGo: Int,
    val homeScore: Int,
    val awayScore: Int,
    val playType: PlayType,
) {
    companion object {
        fun afterPlay(game: Game) =
            WinProbabilityState(
                possession = game.possession,
                quarter = game.quarter,
                clock = convertClockToSeconds(game.clock),
                ballLocation = game.ballLocation,
                down = game.down,
                yardsToGo = game.yardsToGo,
                homeScore = game.homeScore,
                awayScore = game.awayScore,
                playType = game.currentPlayType ?: PlayType.NORMAL,
            )

        fun beforeSnap(
            play: Play,
            homeScore: Int,
            awayScore: Int,
        ) = WinProbabilityState(
            possession = play.possession,
            quarter = play.quarter,
            clock = play.clock,
            ballLocation = play.ballLocation,
            down = play.down,
            yardsToGo = play.yardsToGo,
            homeScore = homeScore,
            awayScore = awayScore,
            playType = playTypeOf(play.playCall),
        )

        fun sidePerspective(
            probability: Double,
            side: TeamSide,
        ) = if (side == TeamSide.HOME) probability else 1.0 - probability

        private fun playTypeOf(playCall: PlayCall?) =
            when (playCall) {
                PlayCall.PAT, PlayCall.TWO_POINT -> PlayType.PAT
                PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_ONSIDE, PlayCall.KICKOFF_SQUIB -> PlayType.KICKOFF
                else -> PlayType.NORMAL
            }
    }
}
