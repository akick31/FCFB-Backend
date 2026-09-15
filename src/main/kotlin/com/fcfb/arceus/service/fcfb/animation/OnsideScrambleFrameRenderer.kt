package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import kotlin.math.sin

@Component
class OnsideScrambleFrameRenderer : PlayAnimationFrameRenderer {
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
        val direction = if (endAbs >= startAbs) 1 else -1
        val kickTargetAbs = startAbs + direction * KICK_DISTANCE

        return animationTimeline().map { t ->
            val (abs, y) =
                if (t <= KICK_PORTION) {
                    val progress = t / KICK_PORTION
                    val a = startAbs + (kickTargetAbs - startAbs) * progress
                    a to centerY + (KICK_DROP * progress).toInt()
                } else if (t <= BOUNCE_END_PORTION) {
                    val bounceT = (t - KICK_PORTION) / (BOUNCE_END_PORTION - KICK_PORTION)
                    val jitter = (JITTER_AMPLITUDE * (1f - bounceT) * sin(bounceT * JITTER_CYCLES * 2 * Math.PI))
                    val bounce = (BOUNCE_AMPLITUDE * (1f - bounceT) * sin(bounceT * BOUNCE_CYCLES * 2 * Math.PI)).toInt()
                    (kickTargetAbs + jitter).toFloat() to (centerY + KICK_DROP + bounce)
                } else {
                    val runT = (t - BOUNCE_END_PORTION) / (1f - BOUNCE_END_PORTION)
                    val a = kickTargetAbs + (endAbs - kickTargetAbs) * runT
                    val celebrateHop = (CELEBRATE_HOP * sin(runT * CELEBRATE_CYCLES * 2 * Math.PI)).toInt()
                    a to (centerY + celebrateHop)
                }
            val x = FieldCoordinateMapper.toPixelX(Math.round(abs), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also {
                if (shouldDrawScrimmageLines(play.playCall)) {
                    FieldBackgroundPainter.drawScrimmageLines(it, startAbs, firstDownAbs)
                }
                PlayerFormationPainter.drawKickoff(it, play, startAbs, x, y, t, onside = true)
                FieldBackgroundPainter.drawSpiralingBall(it, x, y, t * SPIN_CYCLES)
            }
        }
    }

    companion object {
        private const val KICK_DISTANCE = 10
        private const val KICK_PORTION = 0.25f
        private const val BOUNCE_END_PORTION = 0.55f
        private const val KICK_DROP = 40
        private const val JITTER_AMPLITUDE = 12f
        private const val JITTER_CYCLES = 5.0
        private const val BOUNCE_AMPLITUDE = 36
        private const val BOUNCE_CYCLES = 6.0
        private const val CELEBRATE_HOP = 15
        private const val CELEBRATE_CYCLES = 3.0
        private const val SPIN_CYCLES = 6f
    }
}
