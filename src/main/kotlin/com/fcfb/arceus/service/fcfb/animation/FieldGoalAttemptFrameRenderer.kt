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
class FieldGoalAttemptFrameRenderer : PlayAnimationFrameRenderer {
    override fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        homeTeam: Team,
        awayTeam: Team,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ): List<BufferedImage> {
        val layout = GoalPostScenePainter.layoutFor(play)
        val good = play.actualResult == ActualResult.GOOD
        val blocked = play.actualResult == ActualResult.BLOCKED
        val missLeft = play.playId % 2 == 0
        val targetX =
            when {
                good || blocked -> GoalPostScenePainter.CENTER_X
                missLeft -> layout.leftUprightX
                else -> layout.rightUprightX
            }

        val timeline = animationTimeline()
        val contactFrameIndex = if (good || blocked) -1 else timeline.indexOfFirst { it >= CONTACT_PROGRESS }

        return timeline.mapIndexed { index, t ->
            val scene = GoalPostScenePainter.paint(homeTeam, layout)
            if (blocked) {
                GoalPostScenePainter.drawDefender(scene, GoalPostScenePainter.CENTER_X, defenderTopY(t))
            }
            val (x, y, ballScale) = ballPosition(t, targetX, good, blocked, layout)
            if (y in 0..FieldBackgroundPainter.HEIGHT) {
                FieldBackgroundPainter.drawBall(scene, x, y, ballScale)
            }
            if (contactFrameIndex != -1 && index in contactFrameIndex until contactFrameIndex + DOINK_FRAME_COUNT) {
                GoalPostScenePainter.drawDoink(scene, targetX, layout.uprightMidY)
            }
            scene
        }
    }

    private fun defenderTopY(t: Float): Int {
        val distanceFromContact = kotlin.math.abs(t - BLOCK_CONTACT_PROGRESS)
        val jumpProgress = (1f - (distanceFromContact / DEFENDER_JUMP_WINDOW)).coerceIn(0f, 1f)
        return (DEFENDER_GROUNDED_TOP_Y - DEFENDER_PEAK_HEIGHT * jumpProgress).toInt()
    }

    private fun ballPosition(
        t: Float,
        targetX: Int,
        good: Boolean,
        blocked: Boolean,
        layout: GoalPostScenePainter.Layout,
    ): Triple<Int, Int, Float> {
        if (good) {
            if (t <= NET_RISE_PORTION) {
                val progress = t / NET_RISE_PORTION
                val eased = sin(progress * Math.PI / 2)
                val y = GoalPostScenePainter.BASE_Y - (GoalPostScenePainter.BASE_Y - layout.netContactY) * eased
                val curve = (CURVE_AMOUNT * sin(Math.PI * progress)).toInt()
                val scale = 1f - (1f - layout.scale) * progress.toFloat()
                return Triple(GoalPostScenePainter.CENTER_X + curve, y.toInt(), scale)
            }
            return Triple(GoalPostScenePainter.CENTER_X, layout.netContactY, layout.scale)
        }
        if (blocked) {
            if (t <= BLOCK_CONTACT_PROGRESS) {
                val progress = t / BLOCK_CONTACT_PROGRESS
                val y = GoalPostScenePainter.BASE_Y - BLOCKED_BALL_RISE * progress
                return Triple(GoalPostScenePainter.CENTER_X, y.toInt(), 1f)
            }
            val dropProgress = ((t - BLOCK_CONTACT_PROGRESS) / (1f - BLOCK_CONTACT_PROGRESS)).coerceIn(0f, 1f)
            val y = (GoalPostScenePainter.BASE_Y - BLOCKED_BALL_RISE) + (BLOCKED_BALL_RISE + BLOCKED_BALL_SETTLE) * dropProgress
            return Triple(GoalPostScenePainter.CENTER_X, y.toInt(), 1f)
        }
        if (t <= CONTACT_PROGRESS) {
            val progress = t / CONTACT_PROGRESS
            val x = GoalPostScenePainter.CENTER_X + (targetX - GoalPostScenePainter.CENTER_X) * progress
            val y = GoalPostScenePainter.BASE_Y + (layout.uprightMidY - GoalPostScenePainter.BASE_Y) * progress
            val scale = 1f - (1f - layout.scale) * progress
            return Triple(x.toInt(), y.toInt(), scale)
        }
        val deflectProgress = ((t - CONTACT_PROGRESS) / (1f - CONTACT_PROGRESS)).coerceIn(0f, 1f)
        val deflectDirection = if (targetX < GoalPostScenePainter.CENTER_X) -1 else 1
        val x = targetX + deflectDirection * DEFLECT_DISTANCE * deflectProgress
        val fall = layout.uprightMidY + DEFLECT_DROP * deflectProgress
        val bounce = -kotlin.math.abs(BOUNCE_HEIGHT * sin(deflectProgress * BOUNCE_CYCLES * Math.PI)) * (1f - deflectProgress)
        val y = fall + bounce
        return Triple(x.toInt(), y.toInt(), layout.scale)
    }

    companion object {
        private const val CONTACT_PROGRESS = 0.65f
        private const val DOINK_FRAME_COUNT = 4
        private const val NET_RISE_PORTION = 0.55f
        private const val CURVE_AMOUNT = 45f
        private const val DEFLECT_DISTANCE = 50f
        private const val DEFLECT_DROP = 360f
        private const val BOUNCE_HEIGHT = 50f
        private const val BOUNCE_CYCLES = 2.5
        private const val BLOCK_CONTACT_PROGRESS = 0.35f
        private const val BLOCKED_BALL_RISE = 60f
        private const val BLOCKED_BALL_SETTLE = 10f
        private const val DEFENDER_JUMP_WINDOW = 0.35f
        private const val DEFENDER_PEAK_HEIGHT = 80f
        private const val DEFENDER_GROUNDED_TOP_Y = GoalPostScenePainter.BASE_Y - 72
    }
}
