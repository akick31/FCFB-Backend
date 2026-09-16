package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.choreography.SCORE_AT
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
private const val MIN_MOTION_FRAMES = 16

/**
 * Long plays get extra motion frames so the ball carrier covers the extra ground at the same speed, not faster.
 * [celebrationFrames] spends extra frames on the tail after a score, where the play itself has already finished.
 */
internal fun animationTimeline(
    extraMotionFrames: Int = 0,
    celebrationFrames: Int = 0,
    endsAt: Float = 1f,
): List<Float> {
    val motionEnd = if (celebrationFrames > 0) SCORE_AT else endsAt
    val fullFrames = MOTION_FRAME_COUNT + extraMotionFrames
    val motionFrames = maxOf(MIN_MOTION_FRAMES, (fullFrames * motionEnd).toInt())
    val motion = (0 until motionFrames).map { motionEnd * it / (motionFrames - 1).toFloat() }
    val celebration = (1..celebrationFrames).map { SCORE_AT + (1f - SCORE_AT) * it / celebrationFrames }
    val last = if (celebrationFrames > 0) 1f else motionEnd
    return List(LEAD_IN_FRAME_COUNT) { 0f } + motion + celebration + List(HOLD_FRAME_COUNT) { last }
}

internal fun firstDownAbsFor(
    play: Play,
    startAbs: Int,
): Int = if (play.possession == TeamSide.HOME) startAbs + play.yardsToGo else startAbs - play.yardsToGo

private val NO_SCRIMMAGE_LINE_PLAY_CALLS =
    setOf(PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_SQUIB, PlayCall.KICKOFF_ONSIDE, PlayCall.TWO_POINT)

internal fun shouldDrawScrimmageLines(playCall: PlayCall?): Boolean = playCall !in NO_SCRIMMAGE_LINE_PLAY_CALLS
