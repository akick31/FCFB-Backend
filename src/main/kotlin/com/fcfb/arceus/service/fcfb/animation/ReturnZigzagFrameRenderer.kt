package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import kotlin.math.sin

@Component
class ReturnZigzagFrameRenderer : PlayAnimationFrameRenderer {
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
        val isMuffed = play.actualResult == ActualResult.MUFFED_KICK || play.actualResult == ActualResult.MUFFED_PUNT
        val firstDownAbs = firstDownAbsFor(play, startAbs)
        return animationTimeline().map { t ->
            val effectiveT = if (isMuffed) muffedProgress(t) else t
            val abs = startAbs + (endAbs - startAbs) * effectiveT
            val x = FieldCoordinateMapper.toPixelX(Math.round(abs), FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.MARGIN)
            val decay = 1f - effectiveT
            val bobble = if (isMuffed && t < BOBBLE_WINDOW) (BOBBLE_AMPLITUDE * sin(t / BOBBLE_WINDOW * 4 * Math.PI)).toInt() else 0
            val y = centerY + (ZIGZAG_AMPLITUDE * decay * sin(effectiveT * ZIGZAG_CYCLES * 2 * Math.PI)).toInt() + bobble
            FieldBackgroundPainter.paint(homeTeam, awayTeam).also {
                if (shouldDrawScrimmageLines(play.playCall)) {
                    FieldBackgroundPainter.drawScrimmageLines(it, startAbs, firstDownAbs)
                }
                PlayerFormationPainter.draw(it, play, startAbs, x, y, t, offensivePlaybook, defensivePlaybook)
                FieldBackgroundPainter.drawBall(it, x, y)
            }
        }
    }

    private fun muffedProgress(t: Float): Float = if (t < BOBBLE_WINDOW) 0f else (t - BOBBLE_WINDOW) / (1f - BOBBLE_WINDOW)

    companion object {
        private const val ZIGZAG_AMPLITUDE = 90
        private const val ZIGZAG_CYCLES = 3.5
        private const val BOBBLE_WINDOW = 0.25f
        private const val BOBBLE_AMPLITUDE = 20
    }
}
