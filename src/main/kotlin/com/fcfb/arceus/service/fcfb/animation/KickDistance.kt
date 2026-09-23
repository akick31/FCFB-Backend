package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.model.Play

object KickDistance {
    const val RETURNABLE_SHORT_KICK_YARDS = 55f

    private const val PAT_LINE_OF_SCRIMMAGE_YARDS = 3f

    fun attemptYards(play: Play): Float =
        lineOfScrimmageYards(play) + GoalPostScenePainter.HOLD_DEPTH_YARDS + GoalPostScenePainter.END_ZONE_DEPTH_YARDS

    fun fallsShort(play: Play): Boolean = attemptYards(play) >= RETURNABLE_SHORT_KICK_YARDS

    private fun lineOfScrimmageYards(play: Play): Float =
        if (play.playCall == PlayCall.PAT) PAT_LINE_OF_SCRIMMAGE_YARDS else (100 - play.ballLocation).toFloat().coerceAtLeast(1f)
}
