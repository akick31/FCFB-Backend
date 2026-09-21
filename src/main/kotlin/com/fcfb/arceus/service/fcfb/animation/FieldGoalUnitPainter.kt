package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import java.awt.image.BufferedImage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

object FieldGoalUnitPainter {
    const val KICK_AT = 0.3f

    private const val CENTER_X = GoalPostScenePainter.CENTER_X.toFloat()
    private const val SNAP_AT = 0.1f
    private const val HOLD_AT = 0.18f
    private const val GAP_AT = HOLD_AT + 0.04f
    private const val BLOCK_AT = 0.38f
    private const val SETTLED_AT = 0.85f
    private const val FOLLOW_THROUGH = 0.12f

    private const val RUSH_DEPTH = -2.2f
    private const val MIDDLE_RUSHER_EXTRA_DEPTH = -1.5f
    private const val RUSH_SURGE = 0.6f
    private const val LINE_SET_BACK = 0.4f
    private const val WING_DEPTH = 1f
    private const val KICKER_START_DEPTH = 9.5f
    private const val KICKER_PLANT_DEPTH = 8.2f
    private const val BLOCKER_END_DEPTH = 2.5f
    private const val REBOUND_DEPTH = 5f

    private const val LINE_SCALE = 0.85f
    private const val WING_SCALE = 0.9f
    private const val RUSH_SCALE = 0.8f
    private const val HOLDER_SCALE = 0.95f
    private const val KICKER_SCALE = 1f
    private const val BLOCKER_END_SCALE = 0.9f
    private const val FULL_SIZE_PIXELS_PER_YARD = 8f
    private const val MIN_UNIT_SCALE = 0.65f

    private const val HOLDER_OFFSET_X = 18f
    private const val HOLD_OFFSET_X = -4f
    private const val HOLD_LIFT = 13f
    private const val SNAP_LIFT = 8f
    private const val KICKER_START_X = -60f
    private const val KICKER_PLANT_X = -20f
    private const val BLOCKER_START_X = 21f
    private const val BLOCKER_GAP_X = 21f
    private const val BLOCKER_END_X = 12f
    private const val BLOCKER_JUMP = 22f
    private const val NEAR_MISS_REACH = 0.72f
    private const val JUMP_WINDOW = 0.08f
    private const val HAND_REACH = 78f

    private const val BALL_SCALE = 1.1f
    private const val SNAP_ARC = 10f
    private const val REBOUND_X = 150f
    private const val REBOUND_FALL_PORTION = 0.3f
    private const val REBOUND_HOP = 40f
    private const val REBOUND_BOUNCES = 3.0

    private const val HOLDER_NUMBER = 12
    private const val KICKER_NUMBER = 39
    private val LINE_OFFSETS = listOf(0f, -42f, 42f, -84f, 84f, -126f, 126f)
    private val LINE_NUMBERS = listOf(55, 64, 71, 68, 77, 85, 88)
    private val WING_OFFSETS = listOf(-164f, 164f)
    private val WING_NUMBERS = listOf(81, 42)
    private val RUSH_OFFSETS = listOf(-189f, -147f, -105f, -63f, -21f, 0f, 21f, 63f, 105f, 147f, 189f)
    private val MIDDLE_RUSHER = RUSH_OFFSETS.indexOf(0f)
    private val RUSH_NUMBERS = listOf(94, 21, 58, 97, 90, 99, 92, 45, 91, 3, 24)

    fun paint(
        scene: BufferedImage,
        layout: GoalPostScenePainter.Layout,
        progress: Float,
        kicking: Uniform,
        rushing: Uniform,
        blockSide: Int,
        blocked: Boolean,
        rusherIndex: Int = -1,
        nearMiss: Boolean = false,
    ) {
        val blocker = if (blocked || nearMiss) blockerIndex(rusherIndex, blockSide) else -1
        val rushUnit =
            RUSH_OFFSETS.indices.map { index ->
                if (index == blocker) {
                    blockerFigure(
                        layout,
                        progress,
                        rushing,
                        blockSide,
                        index,
                        nearMiss,
                    )
                } else {
                    rusher(layout, progress, rushing, index)
                }
            }
        (kickingUnit(layout, progress, kicking) + rushUnit).sortedBy { it.footY }.forEach { FieldGoalPlayerPainter.draw(scene, it) }
        if (blocked || progress < KICK_AT) {
            val (ballX, ballY) = ballPosition(layout, progress, blockSide)
            val upright = progress >= HOLD_AT && progress < KICK_AT
            FieldBackgroundPainter.drawBall(
                scene,
                ballX.toInt(),
                ballY.toInt(),
                heldBallScale(layout),
                angle = if (upright) -PI / 2 else 0.0,
            )
        }
    }

    fun holdSpot(layout: GoalPostScenePainter.Layout): Pair<Float, Float> {
        val unit = unitScale(layout)
        return CENTER_X + HOLD_OFFSET_X * unit to depthY(layout, GoalPostScenePainter.HOLD_DEPTH_YARDS) - HOLD_LIFT * unit
    }

    fun heldBallScale(layout: GoalPostScenePainter.Layout): Float = BALL_SCALE * unitScale(layout)

    private fun unitScale(layout: GoalPostScenePainter.Layout): Float =
        (layout.pixelsPerYard / FULL_SIZE_PIXELS_PER_YARD).coerceIn(MIN_UNIT_SCALE, 1f)

    private fun depthY(
        layout: GoalPostScenePainter.Layout,
        yardsBehindLine: Float,
    ): Float = layout.yardY(layout.lineOfScrimmageYards + yardsBehindLine)

    private fun kickingUnit(
        layout: GoalPostScenePainter.Layout,
        progress: Float,
        uniform: Uniform,
    ): List<FieldGoalFigure> {
        val unit = unitScale(layout)
        val setBack = segment(progress, SNAP_AT, HOLD_AT) * LINE_SET_BACK
        val line =
            LINE_OFFSETS.mapIndexed { index, offset ->
                FieldGoalFigure(
                    CENTER_X + offset * unit,
                    depthY(layout, setBack),
                    LINE_SCALE * unit,
                    uniform,
                    LINE_NUMBERS[index],
                    PlayerPose.STANDING,
                )
            }
        val wings =
            WING_OFFSETS.mapIndexed { index, offset ->
                FieldGoalFigure(
                    CENTER_X + offset * unit,
                    depthY(layout, WING_DEPTH + setBack),
                    WING_SCALE * unit,
                    uniform,
                    WING_NUMBERS[index],
                    PlayerPose.STANDING,
                )
            }
        val holder =
            FieldGoalFigure(
                CENTER_X + HOLDER_OFFSET_X * unit,
                depthY(layout, GoalPostScenePainter.HOLD_DEPTH_YARDS),
                HOLDER_SCALE * unit,
                uniform,
                HOLDER_NUMBER,
                PlayerPose.KNEELING,
            )
        val approach = segment(progress, HOLD_AT, KICK_AT)
        val kickerPose = if (progress >= KICK_AT && progress < KICK_AT + FOLLOW_THROUGH) PlayerPose.KICKING else PlayerPose.STANDING
        val kicker =
            FieldGoalFigure(
                CENTER_X + lerp(KICKER_START_X, KICKER_PLANT_X, approach) * unit,
                depthY(layout, lerp(KICKER_START_DEPTH, KICKER_PLANT_DEPTH, approach)),
                KICKER_SCALE * unit,
                uniform,
                KICKER_NUMBER,
                kickerPose,
            )
        return line + wings + holder + kicker
    }

    private fun rusher(
        layout: GoalPostScenePainter.Layout,
        progress: Float,
        uniform: Uniform,
        index: Int,
    ): FieldGoalFigure {
        val unit = unitScale(layout)
        val pose = if (index % 2 == 0 && progress >= HOLD_AT) PlayerPose.ARMS_UP else PlayerPose.STANDING
        return FieldGoalFigure(
            CENTER_X + RUSH_OFFSETS[index] * unit,
            depthY(layout, rushDepth(index, progress)),
            RUSH_SCALE * unit,
            uniform,
            RUSH_NUMBERS[index],
            pose,
            facingCamera = true,
        )
    }

    private fun rushDepth(
        index: Int,
        progress: Float,
    ): Float {
        val stagger = if (index == MIDDLE_RUSHER) MIDDLE_RUSHER_EXTRA_DEPTH else 0f
        return RUSH_DEPTH + stagger + segment(progress, SNAP_AT, HOLD_AT) * RUSH_SURGE
    }

    private fun blockerIndex(
        rusherIndex: Int,
        blockSide: Int,
    ): Int {
        if (rusherIndex in RUSH_OFFSETS.indices) return rusherIndex
        return RUSH_OFFSETS.indexOf(BLOCKER_START_X * blockSide)
    }

    private fun blockerFigure(
        layout: GoalPostScenePainter.Layout,
        progress: Float,
        uniform: Uniform,
        blockSide: Int,
        index: Int,
        nearMiss: Boolean,
    ): FieldGoalFigure {
        val (x, footY, scale) = blockerStance(layout, progress, blockSide, nearMiss)
        val pose = if (progress >= GAP_AT) PlayerPose.ARMS_UP else PlayerPose.STANDING
        return FieldGoalFigure(x, footY, scale, uniform, RUSH_NUMBERS[index], pose, facingCamera = true)
    }

    private fun blockerStance(
        layout: GoalPostScenePainter.Layout,
        progress: Float,
        blockSide: Int,
        nearMiss: Boolean = false,
    ): Triple<Float, Float, Float> {
        val unit = unitScale(layout)
        val reach = if (nearMiss) NEAR_MISS_REACH else 1f
        val toGap = segment(progress, SNAP_AT, GAP_AT)
        val through = segment(progress, GAP_AT, BLOCK_AT - 0.04f) * reach
        val offset = if (through <= 0f) lerp(BLOCKER_START_X, BLOCKER_GAP_X, toGap) else lerp(BLOCKER_GAP_X, BLOCKER_END_X, through)
        val depth = if (through <= 0f) lerp(RUSH_DEPTH, 0f, toGap) else lerp(0f, BLOCKER_END_DEPTH, through)
        val scale = lerp(RUSH_SCALE, BLOCKER_END_SCALE, segment(depth, RUSH_DEPTH, BLOCKER_END_DEPTH)) * unit
        val jump = BLOCKER_JUMP * unit * (1f - abs(progress - BLOCK_AT) / JUMP_WINDOW).coerceIn(0f, 1f)
        return Triple(CENTER_X + blockSide * offset * unit, depthY(layout, depth) - jump, scale)
    }

    private fun ballPosition(
        layout: GoalPostScenePainter.Layout,
        progress: Float,
        blockSide: Int,
    ): Pair<Float, Float> {
        val unit = unitScale(layout)
        val (holdX, holdY) = holdSpot(layout)
        val snapY = depthY(layout, 0f) - SNAP_LIFT * unit
        val (blockerX, blockerFootY, blockerScale) = blockerStance(layout, BLOCK_AT, blockSide, false)
        val handsY = blockerFootY - HAND_REACH * blockerScale
        return when {
            progress < SNAP_AT -> CENTER_X to snapY
            progress < HOLD_AT -> {
                val fraction = segment(progress, SNAP_AT, HOLD_AT)
                lerp(CENTER_X, holdX, fraction) to lerp(snapY, holdY, fraction) - SNAP_ARC * unit * sin(PI * fraction).toFloat()
            }
            progress < KICK_AT -> holdX to holdY
            progress < BLOCK_AT -> {
                val fraction = segment(progress, KICK_AT, BLOCK_AT)
                lerp(holdX, blockerX, fraction) to lerp(holdY, handsY, fraction)
            }
            else -> rebound(segment(progress, BLOCK_AT, SETTLED_AT), blockerX, handsY, depthY(layout, REBOUND_DEPTH), blockSide * unit)
        }
    }

    private fun rebound(
        fraction: Float,
        fromX: Float,
        fromY: Float,
        groundY: Float,
        direction: Float,
    ): Pair<Float, Float> {
        val x = fromX + direction * REBOUND_X * sqrt(fraction)
        if (fraction < REBOUND_FALL_PORTION) {
            val fall = fraction / REBOUND_FALL_PORTION
            return x to fromY + (groundY - fromY) * fall * fall
        }
        val settle = (fraction - REBOUND_FALL_PORTION) / (1f - REBOUND_FALL_PORTION)
        val hop = abs(sin(settle * REBOUND_BOUNCES * PI)).toFloat() * REBOUND_HOP * abs(direction) * (1f - settle) * (1f - settle)
        return x to groundY - hop
    }

    private fun lerp(
        from: Float,
        to: Float,
        fraction: Float,
    ): Float = from + (to - from) * fraction
}
