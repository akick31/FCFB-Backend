package com.fcfb.arceus.service.fcfb.animation.choreography

object GangTackle {
    private const val TRAIL_RADIUS = 3.5f
    private const val TRAIL_STEP = 1.5f
    private const val TRAIL_SPEED_FACTOR = 0.8f
    private const val ARRIVAL_MARGIN = 1.3f
    private const val MIN_CHASE_TIME = 0.05f

    private const val MAX_CHASE_SPEED = 95f
    private const val CHASE_PACE_FACTOR = 0.9f

    private fun carrierPace(
        carrier: Track,
        reactAt: Float,
        tackleAt: Float,
    ): Float = carrier.at(reactAt).distanceTo(carrier.at(tackleAt)) / maxOf(tackleAt - reactAt, MIN_CHASE_TIME)

    private fun arrivalSpeed(
        from: FieldPoint,
        carrier: Track,
        tackleAt: Float,
        reactAt: Float,
        ownSpeed: Float,
    ): Float {
        val needed = from.distanceTo(carrier.at(tackleAt)) / maxOf(tackleAt - reactAt, MIN_CHASE_TIME) * ARRIVAL_MARGIN
        val pacing = carrierPace(carrier, reactAt, tackleAt) * ARRIVAL_MARGIN
        return Pursuit.paced(maxOf(needed, pacing).coerceAtMost(MAX_CHASE_SPEED * positionFactor(ownSpeed)))
    }

    private fun positionFactor(ownSpeed: Float): Float = ownSpeed / Pursuit.COVERAGE_SPEED

    fun converge(
        before: List<Track>,
        candidates: Iterable<Int>,
        carrier: Track,
        direction: Float,
        tackleAt: Float,
        tacklers: Int,
        reactAt: (Int) -> Float,
        speed: (Int) -> Float,
        lead: (Int) -> Float = { 0f },
    ): List<Track> {
        val reactPositions = before.mapIndexed { index, track -> track.at(reactAt(index)) }
        val tacklerIds = Pursuit.closest(reactPositions, candidates, carrier.at(tackleAt), tacklers)
        return before.indices.map { index ->
            val slot = tacklerIds.indexOf(index)
            val chased =
                if (slot >= 0) {
                    val arrivalSpeed = arrivalSpeed(reactPositions[index], carrier, tackleAt, reactAt(index), speed(index))
                    Pursuit.chase(
                        before[index],
                        reactAt(index),
                        maxOf(speed(index), arrivalSpeed),
                        Pursuit.tackle(carrier, direction, slot, tackleAt),
                    )
                } else {
                    Pursuit.chase(
                        before[index],
                        reactAt(index),
                        trailSpeed(index, speed, carrier, reactAt(index), tackleAt),
                        Pursuit.intercept(carrier, lead(index), tackleAt, direction),
                    )
                }
            switchAt(tackleAt, chased, hold(chased.at(tackleAt)))
        }
    }

    private fun trailSpeed(
        index: Int,
        speed: (Int) -> Float,
        carrier: Track,
        reactAt: Float,
        tackleAt: Float,
    ): Float {
        val trail = speed(index) * TRAIL_SPEED_FACTOR
        val paced = carrierPace(carrier, reactAt, tackleAt) * CHASE_PACE_FACTOR * positionFactor(speed(index))
        return Pursuit.paced(maxOf(trail, paced))
    }
}
