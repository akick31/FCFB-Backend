package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import kotlin.math.sin

@Component
class KickoffReturnFrameRenderer : PlayAnimationFrameRenderer {
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
        val catchAbs = if (play.possession == TeamSide.HOME) 100 - CATCH_DEPTH else CATCH_DEPTH
        val touchback = play.result == Scenario.TOUCHBACK
        val touchbackTarget = if (play.possession == TeamSide.HOME) 100 + END_ZONE_CENTER else -END_ZONE_CENTER
        val kickTargetAbs = if (touchback) touchbackTarget else catchAbs

        return animationTimeline().map { t ->
            val (abs, y, spinning) =
                if (t <= PHASE_SPLIT) {
                    val progress = t / PHASE_SPLIT
                    val flightAbs = startAbs + (kickTargetAbs - startAbs) * progress
                    val flightY = centerY - (KICK_ARC_HEIGHT * sin(Math.PI * progress)).toInt()
                    Triple(flightAbs, flightY, true)
                } else if (touchback) {
                    val bounceProgress = (t - PHASE_SPLIT) / (1f - PHASE_SPLIT)
                    val bounce = (BOUNCE_AMPLITUDE * (1f - bounceProgress) * sin(bounceProgress * BOUNCE_CYCLES * 2 * Math.PI)).toInt()
                    Triple(touchbackTarget.toFloat(), centerY + bounce, false)
                } else {
                    val progress = (t - PHASE_SPLIT) / (1f - PHASE_SPLIT)
                    val runAbs = catchAbs + (endAbs - catchAbs) * progress
                    Triple(runAbs, centerY, false)
                }
            val x = FieldCoordinateMapper.toPixelX(Math.round(abs), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also {
                PlayerFormationPainter.drawKickoff(it, play, startAbs, x, y, t, onside = false)
                if (spinning) {
                    FieldBackgroundPainter.drawSpiralingBall(it, x, y, t * SPIN_CYCLES)
                } else {
                    FieldBackgroundPainter.drawBall(it, x, y)
                }
            }
        }
    }

    companion object {
        private const val CATCH_DEPTH = 0
        private const val PHASE_SPLIT = 0.4f
        private const val KICK_ARC_HEIGHT = 170
        private const val SPIN_CYCLES = 6f
        private const val BOUNCE_AMPLITUDE = 20
        private const val BOUNCE_CYCLES = 2.0
        private const val END_ZONE_CENTER = 5
    }
}
