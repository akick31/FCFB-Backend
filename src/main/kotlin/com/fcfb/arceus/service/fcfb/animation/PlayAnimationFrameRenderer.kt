package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import java.awt.image.BufferedImage

interface PlayAnimationFrameRenderer {
    fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        homeTeam: Team,
        awayTeam: Team,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ): List<BufferedImage>
}

internal const val LEAD_IN_FRAME_COUNT = 8
internal const val MOTION_FRAME_COUNT = 26
internal const val HOLD_FRAME_COUNT = 4

internal fun animationTimeline(): List<Float> =
    List(LEAD_IN_FRAME_COUNT) { 0f } +
        (0 until MOTION_FRAME_COUNT).map { it / (MOTION_FRAME_COUNT - 1).toFloat() } +
        List(HOLD_FRAME_COUNT) { 1f }

internal fun firstDownAbsFor(
    play: Play,
    startAbs: Int,
): Int = if (play.possession == TeamSide.HOME) startAbs + play.yardsToGo else startAbs - play.yardsToGo

private val NO_SCRIMMAGE_LINE_PLAY_CALLS =
    setOf(PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_SQUIB, PlayCall.KICKOFF_ONSIDE, PlayCall.TWO_POINT)

internal fun shouldDrawScrimmageLines(playCall: PlayCall?): Boolean = playCall !in NO_SCRIMMAGE_LINE_PLAY_CALLS
