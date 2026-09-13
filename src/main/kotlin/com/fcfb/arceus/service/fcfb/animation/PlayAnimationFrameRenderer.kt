package com.fcfb.arceus.service.fcfb.animation

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
    ): List<BufferedImage>
}

internal const val MOTION_FRAME_COUNT = 10
internal const val HOLD_FRAME_COUNT = 3

internal fun animationTimeline(): List<Float> =
    (0 until MOTION_FRAME_COUNT).map { it / (MOTION_FRAME_COUNT - 1).toFloat() } + List(HOLD_FRAME_COUNT) { 1f }
