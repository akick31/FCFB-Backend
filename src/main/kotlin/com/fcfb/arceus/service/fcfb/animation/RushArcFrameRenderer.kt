package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage

@Component
class RushArcFrameRenderer : PlayAnimationFrameRenderer {
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
        val firstDownAbs = firstDownAbsFor(play, startAbs)
        return animationTimeline().map { t ->
            val abs = startAbs + (endAbs - startAbs) * t
            val x = FieldCoordinateMapper.toPixelX(Math.round(abs), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also {
                if (shouldDrawScrimmageLines(play.playCall)) {
                    FieldBackgroundPainter.drawScrimmageLines(it, startAbs, firstDownAbs)
                }
                PlayerFormationPainter.draw(it, play, startAbs, x, centerY, t, offensivePlaybook, defensivePlaybook)
                FieldBackgroundPainter.drawBall(it, x, centerY)
            }
        }
    }
}
