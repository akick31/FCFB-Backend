package com.fcfb.arceus.service.fcfb.animation.choreography

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

internal const val SNAP_END = 0.07f
internal const val SCORE_AT = 0.92f
private const val CARRY_AHEAD = 0.45f
private const val CARRY_BELOW = 0.3f
private const val SHOTGUN_SNAP_HEIGHT = 1.2f
private const val UNDER_CENTER_SNAP_HEIGHT = 0.2f

/** Under center the exchange is hand to hand, so the ball is in the quarterback's hands almost as he turns to drop. */
internal const val UNDER_CENTER_SNAP_SPEED = 0.25f

private const val CARRIER_YARDS_PER_MOTION = 58f

/** How much of the motion phase a ball carrier needs to cover [yards], so long runs take proportionally longer. */
internal fun carryTime(yards: Float): Float = abs(yards) / CARRIER_YARDS_PER_MOTION

internal fun path(vararg waypoints: Pair<Float, FieldPoint>): Track = WaypointTrack(waypoints.map { Waypoint(it.first, it.second) })

internal fun hold(point: FieldPoint): Track = Track { point }

internal fun segment(
    progress: Float,
    start: Float,
    end: Float,
): Float = if (end <= start) (if (progress >= end) 1f else 0f) else ((progress - start) / (end - start)).coerceIn(0f, 1f)

internal fun arc(
    fraction: Float,
    peak: Float,
): Float = (peak * sin(PI * fraction)).toFloat()

internal fun bounce(
    fraction: Float,
    peak: Float,
    bounces: Int,
): Float = (peak * (1f - fraction) * abs(sin(PI * fraction * bounces))).toFloat()

internal fun Track.offsetBy(offset: FieldPoint): Track = Track { at(it) + offset }

/** Where the ball sits relative to the player carrying it, so the ball — not the marker — marks the spot. */
internal fun carryOffset(direction: Float) = FieldPoint(direction * CARRY_AHEAD, CARRY_BELOW)

internal fun carrierOf(
    ball: Track,
    direction: Float,
): Track = ball.offsetBy(carryOffset(direction) * -1f)

internal fun switchAt(
    progress: Float,
    before: Track,
    after: Track,
): Track = Track { if (it < progress) before.at(it) else after.at(it) }

internal fun snapBall(
    progress: Float,
    center: FieldPoint,
    hands: FieldPoint,
    shotgun: Boolean,
): BallState {
    val exchange = if (shotgun) SNAP_END else SNAP_END * UNDER_CENTER_SNAP_SPEED
    val fraction = segment(progress, 0f, exchange)
    return BallState(center.lerp(hands, fraction), arc(fraction, if (shotgun) SHOTGUN_SNAP_HEIGHT else UNDER_CENTER_SNAP_HEIGHT))
}
