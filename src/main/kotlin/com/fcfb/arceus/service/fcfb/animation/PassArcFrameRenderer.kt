package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import kotlin.math.sin

@Component
class PassArcFrameRenderer : PlayAnimationFrameRenderer {
    override fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        homeTeam: Team,
        awayTeam: Team,
    ): List<BufferedImage> {
        val centerY = FieldBackgroundPainter.HEIGHT / 2
        return animationTimeline().map { t ->
            val abs = startAbs + (endAbs - startAbs) * t
            val x = FieldCoordinateMapper.toPixelX(Math.round(abs), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
            val y = centerY - (PASS_ARC_HEIGHT * sin(Math.PI * t)).toInt()
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also { FieldBackgroundPainter.drawBall(it, x, y) }
        }
    }

    companion object {
        private const val PASS_ARC_HEIGHT = 55
    }
}
