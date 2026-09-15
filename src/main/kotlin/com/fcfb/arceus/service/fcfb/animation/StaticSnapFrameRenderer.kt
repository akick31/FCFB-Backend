package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
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
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ): List<BufferedImage> {
        val centerY = FieldBackgroundPainter.HEIGHT / 2
        val x = FieldCoordinateMapper.toPixelX(endAbs, FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
        val firstDownAbs = firstDownAbsFor(play, startAbs)
        return List(STATIC_FRAME_COUNT) {
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also {
                FieldBackgroundPainter.drawScrimmageLines(it, startAbs, firstDownAbs)
                PlayerFormationPainter.draw(it, play, startAbs, x, centerY, 1f, offensivePlaybook, defensivePlaybook)
                FieldBackgroundPainter.drawBall(it, x, centerY)
            }
        }
    }

    companion object {
        private const val STATIC_FRAME_COUNT = 4
    }
}
