package com.fcfb.arceus.service.fcfb.animation.choreography

/** The players closest to where the carrier goes down wrap him up; everyone else runs to the ball and pulls up short. */
object GangTackle {
    private const val TRAIL_RADIUS = 3.5f
    private const val TRAIL_STEP = 1.5f
    private const val TRAIL_SPEED_FACTOR = 0.8f
    private const val ARRIVAL_MARGIN = 1.3f
    private const val MIN_CHASE_TIME = 0.05f
    private const val MAX_CHASE_SPEED = 38f

    /** Tacklers run as fast as they need to reach the carrier when he goes down, so a carrier never stands waiting to be hit. */
    private fun arrivalSpeed(
        from: FieldPoint,
        carrier: Track,
        tackleAt: Float,
        reactAt: Float,
    ): Float =
        (from.distanceTo(carrier.at(tackleAt)) / maxOf(tackleAt - reactAt, MIN_CHASE_TIME) * ARRIVAL_MARGIN).coerceAtMost(
            MAX_CHASE_SPEED,
        )

    fun converge(
        before: List<Track>,
        candidates: Iterable<Int>,
        carrier: Track,
        direction: Float,
        tackleAt: Float,
        tacklers: Int,
        reactAt: (Int) -> Float,
        speed: (Int) -> Float,
    ): List<Track> {
        val reactPositions = before.mapIndexed { index, track -> track.at(reactAt(index)) }
        val tacklerIds = Pursuit.closest(reactPositions, candidates, carrier.at(tackleAt), tacklers)
        return before.indices.map { index ->
            val slot = tacklerIds.indexOf(index)
            if (slot >= 0) {
                val arrivalSpeed = arrivalSpeed(reactPositions[index], carrier, tackleAt, reactAt(index))
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
                    speed(index) * TRAIL_SPEED_FACTOR,
                    Pursuit.trail(carrier, TRAIL_RADIUS + (index % 3) * TRAIL_STEP),
                )
            }
        }
    }
}
