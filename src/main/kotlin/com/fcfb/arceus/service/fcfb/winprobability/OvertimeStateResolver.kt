package com.fcfb.arceus.service.fcfb.winprobability

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.winprobability.OvertimeState

object OvertimeStateResolver {
    private const val REGULATION_QUARTERS = 4

    fun fromLiveGame(
        game: Game,
        play: Play,
    ) = OvertimeState(
        offense = game.possession,
        period = game.quarter - REGULATION_QUARTERS,
        offenseHasFirstPossession = game.overtimeHalf == 1,
        offenseMargin = marginFor(game.possession, play),
        conversionPending = game.currentPlayType == PlayType.PAT,
        down = game.down,
        distance = game.yardsToGo,
        yardsToGoal = 100 - game.ballLocation,
    )

    fun fromFollowingPlay(
        play: Play,
        followingPlay: Play,
        periodFirstPossession: TeamSide,
    ) = OvertimeState(
        offense = followingPlay.possession,
        period = followingPlay.quarter - REGULATION_QUARTERS,
        offenseHasFirstPossession = followingPlay.possession == periodFirstPossession,
        offenseMargin = marginFor(followingPlay.possession, play),
        conversionPending = followingPlay.playCall == PlayCall.PAT || followingPlay.playCall == PlayCall.TWO_POINT,
        down = followingPlay.down,
        distance = followingPlay.yardsToGo,
        yardsToGoal = 100 - followingPlay.ballLocation,
    )

    private fun marginFor(
        side: TeamSide,
        play: Play,
    ) = if (side == TeamSide.HOME) play.homeScore - play.awayScore else play.awayScore - play.homeScore
}
