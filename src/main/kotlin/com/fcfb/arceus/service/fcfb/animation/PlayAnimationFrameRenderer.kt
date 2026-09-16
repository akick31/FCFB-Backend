package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import java.awt.image.BufferedImage

interface PlayAnimationFrameRenderer {
    fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        theme: FieldTheme,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ): List<BufferedImage>
}

internal const val LEAD_IN_FRAME_COUNT = 8
internal const val MOTION_FRAME_COUNT = 52
internal const val HOLD_FRAME_COUNT = 4

/** Long plays get extra motion frames so the ball carrier covers the extra ground at the same speed, not faster. */
internal fun animationTimeline(extraMotionFrames: Int = 0): List<Float> {
    val motionFrames = MOTION_FRAME_COUNT + extraMotionFrames
    return List(LEAD_IN_FRAME_COUNT) { 0f } +
        (0 until motionFrames).map { it / (motionFrames - 1).toFloat() } +
        List(HOLD_FRAME_COUNT) { 1f }
}

internal fun firstDownAbsFor(
    play: Play,
    startAbs: Int,
): Int = if (play.possession == TeamSide.HOME) startAbs + play.yardsToGo else startAbs - play.yardsToGo

private val NO_SCRIMMAGE_LINE_PLAY_CALLS =
    setOf(PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_SQUIB, PlayCall.KICKOFF_ONSIDE, PlayCall.TWO_POINT)

internal fun shouldDrawScrimmageLines(playCall: PlayCall?): Boolean = playCall !in NO_SCRIMMAGE_LINE_PLAY_CALLS
