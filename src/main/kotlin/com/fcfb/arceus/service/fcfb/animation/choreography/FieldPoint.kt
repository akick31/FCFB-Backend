package com.fcfb.arceus.service.fcfb.animation.choreography

import kotlin.math.hypot

/** A spot on the field in yards: `along` is the absolute 0-100 frame, `lateral` is distance from midfield width-wise. */
data class FieldPoint(
    val along: Float,
    val lateral: Float,
) {
    operator fun plus(other: FieldPoint) = FieldPoint(along + other.along, lateral + other.lateral)

    operator fun minus(other: FieldPoint) = FieldPoint(along - other.along, lateral - other.lateral)

    operator fun times(scale: Float) = FieldPoint(along * scale, lateral * scale)

    fun distanceTo(other: FieldPoint): Float = hypot(along - other.along, lateral - other.lateral)

    fun lerp(
        other: FieldPoint,
        fraction: Float,
    ): FieldPoint = this + (other - this) * fraction

    fun stepToward(
        target: FieldPoint,
        maxStep: Float,
    ): FieldPoint {
        val distance = distanceTo(target)
        return if (distance <= maxStep) target else lerp(target, maxStep / distance)
    }
}
