package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

@Component
class StaticSnapFrameRenderer : PlayAnimationFrameRenderer {
    override fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        homeTeam: Team,
        awayTeam: Team,
    ): List<BufferedImage> {
        val centerY = FieldBackgroundPainter.HEIGHT / 2
        val x = FieldCoordinateMapper.toPixelX(endAbs, FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
        return List(STATIC_FRAME_COUNT) {
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also { FieldBackgroundPainter.drawBall(it, x, centerY) }
        }
    }

    companion object {
        private const val STATIC_FRAME_COUNT = 4
    }
}
