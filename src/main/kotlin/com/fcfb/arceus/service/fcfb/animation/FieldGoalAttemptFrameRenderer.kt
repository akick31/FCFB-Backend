package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.util.Random

@Component
class FieldGoalAttemptFrameRenderer : PlayAnimationFrameRenderer {
    override fun renderFrames(
        play: Play,
        startAbs: Int,
        endAbs: Int,
        theme: FieldTheme,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
    ): List<BufferedImage> {
        val layout = GoalPostScenePainter.layoutFor(play)
        val kickingHome = play.possession == TeamSide.HOME
        val targetEndZone = theme.endZoneOf(if (kickingHome) TeamSide.AWAY else TeamSide.HOME)
        val (homeUniform, awayUniform) = theme.uniforms()
        val (kicking, rushing) = if (kickingHome) homeUniform to awayUniform else awayUniform to homeUniform

        val blocked = play.actualResult == ActualResult.BLOCKED
        val blockSide = if (play.playId % 2 == 0) 1 else -1
        val trajectory = if (blocked) null else kickTrajectory(play, layout)

        val timeline = animationTimeline()
        val doinked = trajectory?.outcome == KickOutcome.DOINK || trajectory?.outcome == KickOutcome.DOINK_IN
        val contactFrameIndex = if (doinked && trajectory != null) timeline.indexOfFirst { flight(it) >= trajectory.arrival } else -1

        return timeline.mapIndexed { index, t ->
            val scene = GoalPostScenePainter.paint(theme, targetEndZone, layout, midfieldTopOnLeft = kickingHome != theme.flipped)
            FieldGoalUnitPainter.paint(scene, layout, t, kicking, rushing, blockSide, blocked)
            if (trajectory != null && t >= FieldGoalUnitPainter.KICK_AT) {
                drawKick(scene, trajectory.at(flight(t)), flight(t))
                if (flight(t) >= trajectory.arrival && trajectory.outcome in PASSES_THE_POSTS) {
                    GoalPostScenePainter.drawPost(scene, layout)
                }
            }
            if (trajectory != null && contactFrameIndex != -1 && index in contactFrameIndex until contactFrameIndex + DOINK_FRAME_COUNT) {
                GoalPostScenePainter.drawDoink(scene, trajectory.targetX.toInt(), layout.uprightMidY)
            }
            scene
        }
    }

    private fun flight(t: Float): Float = segment(t, FieldGoalUnitPainter.KICK_AT, 1f)

    /** Makes land anywhere between the uprights; misses under the long-kick limit are a random doink or a random side wide. */
    private fun kickTrajectory(
        play: Play,
        layout: GoalPostScenePainter.Layout,
    ): KickTrajectory {
        val random = Random(play.playId.toLong())
        val side = if (random.nextBoolean()) 1f else -1f
        val kickYards = layout.lineOfScrimmageYards + GoalPostScenePainter.HOLD_DEPTH_YARDS + GoalPostScenePainter.END_ZONE_DEPTH_YARDS
        return when {
            play.actualResult == ActualResult.GOOD && random.nextFloat() < DOINK_IN_CHANCE ->
                KickTrajectory(layout, KickOutcome.DOINK_IN, side)
            play.actualResult == ActualResult.GOOD ->
                KickTrajectory(layout, KickOutcome.GOOD, (random.nextFloat() * 2f - 1f) * MAX_MAKE_FRACTION)
            kickYards >= SHORT_KICK_YARDS ->
                KickTrajectory(layout, KickOutcome.SHORT, side * random.nextFloat() * MAX_SHORT_FRACTION)
            random.nextFloat() < DOINK_CHANCE -> KickTrajectory(layout, KickOutcome.DOINK, side)
            else ->
                KickTrajectory(
                    layout,
                    KickOutcome.WIDE,
                    side * (MIN_WIDE_FRACTION + random.nextFloat() * (MAX_WIDE_FRACTION - MIN_WIDE_FRACTION)),
                )
        }
    }

    private fun drawKick(
        scene: BufferedImage,
        point: KickPoint,
        flight: Float,
    ) {
        point.shadowY?.let { shadowY ->
            val g = scene.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.color = SHADOW_COLOR
            val width = SHADOW_WIDTH * point.scale
            g.fill(Ellipse2D.Float(point.x - width / 2, shadowY - width * SHADOW_FLATTENING / 2, width, width * SHADOW_FLATTENING))
            g.dispose()
        }
        FieldBackgroundPainter.drawTumblingBall(
            scene,
            point.x.toInt(),
            point.y.toInt(),
            flight * TUMBLE_CYCLES,
            point.scale,
            angle = -Math.PI / 2,
        )
    }

    companion object {
        private const val DOINK_FRAME_COUNT = 6
        private const val SHORT_KICK_YARDS = 60f
        private const val DOINK_CHANCE = 0.35f
        private const val DOINK_IN_CHANCE = 0.15f
        private val PASSES_THE_POSTS = setOf(KickOutcome.GOOD, KickOutcome.WIDE, KickOutcome.DOINK_IN)
        private const val MAX_MAKE_FRACTION = 0.75f
        private const val MAX_SHORT_FRACTION = 0.6f
        private const val MIN_WIDE_FRACTION = 1.35f
        private const val MAX_WIDE_FRACTION = 1.9f
        private const val TUMBLE_CYCLES = 5f
        private const val SHADOW_WIDTH = 20f
        private const val SHADOW_FLATTENING = 0.4f
        private val SHADOW_COLOR = Color(0, 0, 0, 90)
    }
}
