package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class KickTrajectory(
    private val layout: GoalPostScenePainter.Layout,
    val outcome: KickOutcome,
    lateralFraction: Float,
) {
    val targetX: Float = GoalPostScenePainter.CENTER_X + lateralFraction * (layout.rightUprightX - GoalPostScenePainter.CENTER_X)
    private val start = FieldGoalUnitPainter.holdSpot(layout)
    private val startScale = FieldGoalUnitPainter.heldBallScale(layout)
    private val endScale = maxOf(layout.scale, MIN_END_BALL_SCALE)
    private val startYards = layout.lineOfScrimmageYards + GoalPostScenePainter.HOLD_DEPTH_YARDS
    private val farPixelsPerYard = (layout.endZoneTopY - layout.crossbarY) / GoalPostScenePainter.CROSSBAR_HEIGHT_YARDS
    private val netLeftX = layout.leftUprightX - GoalPostScenePainter.NET_SIDE_MARGIN
    private val netRightX = layout.rightUprightX + GoalPostScenePainter.NET_SIDE_MARGIN

    val arrival: Float =
        ((startYards + GoalPostScenePainter.END_ZONE_DEPTH_YARDS) / ARRIVAL_REFERENCE_YARDS * BASE_ARRIVAL)
            .coerceIn(MIN_ARRIVAL, MAX_ARRIVAL)

    fun at(flight: Float): KickPoint {
        val depth = 1f - (1f - (flight / arrival).coerceAtMost(1f)).pow(DECELERATION)
        val settle = maxOf(SETTLED, arrival + MIN_SETTLE_TIME)
        return when (outcome) {
            KickOutcome.GOOD, KickOutcome.WIDE, KickOutcome.DOINK_IN ->
                if (flight < arrival) airborne(depth, PEAK_DEPTH, APEX_YARDS) else dropBehindPosts(segment(flight, arrival, settle))
            KickOutcome.DOINK ->
                if (flight < arrival) airborne(depth, PEAK_DEPTH, APEX_YARDS) else deflection(segment(flight, arrival, settle))
            KickOutcome.SHORT ->
                if (flight < arrival) {
                    airborne(
                        depth * SHORT_LANDING,
                        SHORT_LANDING / 2,
                        SHORT_APEX_YARDS,
                    )
                } else {
                    roll(segment(flight, arrival, settle))
                }
            KickOutcome.CAUGHT -> caught(minOf(depth, 1f))
        }
    }

    private fun airborne(
        depth: Float,
        peakDepth: Float,
        apexYards: Float,
    ): KickPoint {
        val ground = layout.groundY(startYards - depth * (startYards + GoalPostScenePainter.END_ZONE_DEPTH_YARDS))
        val heightYards = (apexYards * (1f - ((depth - peakDepth) / peakDepth).pow(2))).coerceAtLeast(0f)
        val pixelsPerYard = farPixelsPerYard * (NEAR_HEIGHT_FACTOR + (1f - NEAR_HEIGHT_FACTOR) * depth)
        val x = start.first + (targetX - start.first) * depth.pow(LATERAL_BREAK)
        return KickPoint(x, ground - heightYards * pixelsPerYard, ground, startScale + (endScale - startScale) * depth)
    }

    private fun dropBehindPosts(progress: Float): KickPoint {
        val passing = airborne(1f, PEAK_DEPTH, APEX_YARDS)
        val x = driftedX(passing.x, progress)
        val fall = (progress / DROP_FALL).coerceAtMost(1f)
        val settle = segment(progress, DROP_FALL, 1f)
        if (caughtInNet(passing.x)) {
            val restY = layout.crossbarY - (layout.crossbarY - layout.netTopY) * NET_REST_FRACTION
            val sway = sin(settle * NET_SWAY_CYCLES * PI).toFloat() * NET_SWAY * layout.scale * (1f - settle).pow(2)
            return KickPoint(x, passing.y + (restY - passing.y) * fall * fall + sway, null, endScale * BEHIND_POSTS_SCALE)
        }
        val restY = layout.groundY(-GoalPostScenePainter.END_ZONE_DEPTH_YARDS * BEHIND_POSTS_LANDING_DEPTH)
        val hop = abs(sin(settle * DROP_BOUNCES * PI)).toFloat() * DROP_HOP * layout.scale * (1f - settle).pow(2)
        return KickPoint(x, passing.y + (restY - passing.y) * fall * fall - hop, restY, endScale * BEHIND_POSTS_SCALE)
    }

    private fun driftedX(
        passingX: Float,
        progress: Float,
    ): Float {
        val kickedIn = if (outcome == KickOutcome.DOINK_IN) -DOINK_IN_KICK else BEHIND_POSTS_DRIFT
        val away = if (passingX < GoalPostScenePainter.CENTER_X) -1f else 1f
        return passingX + (targetX - start.first) * kickedIn * progress + away * BEHIND_POSTS_ROLL * layout.scale * progress
    }

    private fun caughtInNet(passingX: Float): Boolean {
        val resting = driftedX(passingX, 1f)
        return resting >= netLeftX && resting <= netRightX
    }

    private fun deflection(progress: Float): KickPoint {
        val contact = airborne(1f, PEAK_DEPTH, APEX_YARDS)
        val direction = if (targetX < GoalPostScenePainter.CENTER_X) -1f else 1f
        val x = contact.x + direction * DEFLECT_DISTANCE * layout.scale * sqrt(progress)
        val ground = layout.groundY(-GoalPostScenePainter.END_ZONE_DEPTH_YARDS * DEFLECT_LANDING_DEPTH)
        if (progress < DEFLECT_FALL) {
            val fall = progress / DEFLECT_FALL
            return KickPoint(x, contact.y + (ground - contact.y) * fall * fall, ground, endScale)
        }
        val settle = (progress - DEFLECT_FALL) / (1f - DEFLECT_FALL)
        val hop = abs(sin(settle * DEFLECT_BOUNCES * PI)).toFloat() * DEFLECT_HOP * layout.scale * (1f - settle).pow(2)
        return KickPoint(x, ground - hop, ground, endScale)
    }

    /**
     * A returnable short kick has to reach the returner's hands still in the air — he cannot be
     * seen fielding it off a bounce. The flight is aimed at his spot and simply stops there.
     */
    private fun caught(depth: Float): KickPoint {
        val toReturner = (startYards + CATCH_DEPTH_IN_END_ZONE) / (startYards + GoalPostScenePainter.END_ZONE_DEPTH_YARDS)
        val reached = depth * toReturner
        val point = airborne(reached, toReturner / 2f, CATCH_APEX_YARDS)
        val hands = CATCH_HANDS_LIFT * layout.scale
        return point.copy(y = point.y - hands, shadowY = null)
    }

    private fun roll(progress: Float): KickPoint {
        val landed = airborne(SHORT_LANDING + SHORT_ROLL * sqrt(progress), SHORT_LANDING / 2, 0f)
        val hop = abs(sin(progress * SHORT_BOUNCES * PI)).toFloat() * SHORT_HOP_YARDS * farPixelsPerYard * (1f - progress).pow(2)
        return landed.copy(y = landed.y - hop)
    }

    companion object {
        private const val BASE_ARRIVAL = 0.62f
        private const val ARRIVAL_REFERENCE_YARDS = 45f
        private const val MIN_ARRIVAL = 0.26f
        private const val MAX_ARRIVAL = 0.72f
        private const val MIN_SETTLE_TIME = 0.2f
        private const val SETTLED = 0.92f
        private const val DECELERATION = 1.3f
        private const val PEAK_DEPTH = 0.6f
        private const val APEX_YARDS = 16f
        private const val SHORT_APEX_YARDS = 13f
        private const val CATCH_DEPTH_IN_END_ZONE = 4f
        private const val CATCH_APEX_YARDS = 15f
        private const val CATCH_HANDS_LIFT = 26f
        private const val SHORT_LANDING = 0.86f
        private const val SHORT_ROLL = 0.06f
        private const val SHORT_BOUNCES = 2.0
        private const val SHORT_HOP_YARDS = 1.2f
        private const val NEAR_HEIGHT_FACTOR = 2.5f
        private const val LATERAL_BREAK = 2.4f
        private const val MIN_END_BALL_SCALE = 0.7f
        private const val BEHIND_POSTS_LANDING_DEPTH = 0.98f
        private const val BEHIND_POSTS_DRIFT = 0.15f
        private const val BEHIND_POSTS_ROLL = 26f
        private const val DOINK_IN_KICK = 0.55f
        private const val BEHIND_POSTS_SCALE = 0.9f
        private const val NET_REST_FRACTION = 0.12f
        private const val NET_SWAY = 4f
        private const val NET_SWAY_CYCLES = 3.0
        private const val DROP_FALL = 0.45f
        private const val DROP_BOUNCES = 2.0
        private const val DROP_HOP = 10f
        private const val DEFLECT_DISTANCE = 160f
        private const val DEFLECT_FALL = 0.3f
        private const val DEFLECT_LANDING_DEPTH = 0.4f
        private const val DEFLECT_BOUNCES = 3.0
        private const val DEFLECT_HOP = 45f
    }
}
