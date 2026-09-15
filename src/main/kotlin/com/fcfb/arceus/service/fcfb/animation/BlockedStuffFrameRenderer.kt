package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import kotlin.math.sin

@Component
class BlockedStuffFrameRenderer : PlayAnimationFrameRenderer {
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
            val advance = if (t <= DEFLECTION_POINT) t / DEFLECTION_POINT else 1f
            val abs = startAbs + (endAbs - startAbs) * DEFLECTION_ADVANCE * advance
            val x = FieldCoordinateMapper.toPixelX(Math.round(abs), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
            val deflectionProgress = ((t - DEFLECTION_POINT) / (1f - DEFLECTION_POINT)).coerceIn(0f, 1f)
            val y = centerY - (DEFLECTION_HEIGHT * sin(Math.PI * deflectionProgress)).toInt()
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also {
                FieldBackgroundPainter.drawScrimmageLines(it, startAbs, firstDownAbs)
                PlayerFormationPainter.draw(it, play, startAbs, x, y, t, offensivePlaybook, defensivePlaybook)
                FieldBackgroundPainter.drawBall(it, x, y)
            }
        }
    }

    companion object {
        private const val DEFLECTION_POINT = 0.3f
        private const val DEFLECTION_ADVANCE = 0.4f
        private const val DEFLECTION_HEIGHT = 50
    }
}
