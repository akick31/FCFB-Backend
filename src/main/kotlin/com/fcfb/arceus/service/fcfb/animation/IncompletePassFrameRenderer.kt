package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.sin

@Component
class IncompletePassFrameRenderer : PlayAnimationFrameRenderer {
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
            val flightT = (t / FLIGHT_PORTION).coerceAtMost(1f)
            val flightAbs = startAbs + (endAbs - startAbs) * flightT
            val baseX = FieldCoordinateMapper.toPixelX(Math.round(flightAbs), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)

            val y: Int
            val x: Int
            if (t < FLIGHT_PORTION) {
                y = centerY - (PASS_ARC_HEIGHT * sin(Math.PI * flightT)).toInt()
                x = baseX
            } else {
                val bounceT = (t - FLIGHT_PORTION) / (1f - FLIGHT_PORTION)
                val decay = 1f - bounceT
                y = centerY - (BOUNCE_AMPLITUDE * decay * abs(sin(bounceT * BOUNCE_CYCLES * Math.PI))).toInt()
                x = baseX + (JITTER_AMPLITUDE * decay * sin(bounceT * JITTER_CYCLES * 2 * Math.PI)).toInt()
            }
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also {
                FieldBackgroundPainter.drawScrimmageLines(it, startAbs, firstDownAbs)
                PlayerFormationPainter.draw(it, play, startAbs, x, y, t, offensivePlaybook, defensivePlaybook)
                FieldBackgroundPainter.drawBall(it, x, y)
            }
        }
    }

    companion object {
        private const val FLIGHT_PORTION = 0.6f
        private const val PASS_ARC_HEIGHT = 110
        private const val BOUNCE_AMPLITUDE = 40
        private const val BOUNCE_CYCLES = 3.0
        private const val JITTER_AMPLITUDE = 20
        private const val JITTER_CYCLES = 4.0
    }
}
