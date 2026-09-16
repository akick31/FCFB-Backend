package com.fcfb.arceus.service.fcfb.animation.choreography

/** The players closest to where the carrier goes down wrap him up; everyone else runs to the ball and pulls up short. */
object GangTackle {
    private const val TRAIL_RADIUS = 3.5f
    private const val TRAIL_STEP = 1.5f
    private const val TRAIL_SPEED_FACTOR = 0.8f
    private const val ARRIVAL_MARGIN = 1.3f
    private const val MIN_CHASE_TIME = 0.05f

    /**
     * A gunner releases at the snap and covers about 50 yards while the returner stands waiting for the catch, which needs
     * roughly 90 of these units. Anything lower leaves him short and the returner stops untouched.
     */
    private const val MAX_CHASE_SPEED = 95f
    private const val CHASE_PACE_FACTOR = 0.9f

    /** How fast the carrier is covering ground, so a breakaway is chased at nearly his pace instead of walking speed. */
    private fun carrierPace(
        carrier: Track,
        reactAt: Float,
        tackleAt: Float,
    ): Float = carrier.at(reactAt).distanceTo(carrier.at(tackleAt)) / maxOf(tackleAt - reactAt, MIN_CHASE_TIME)

    /** Tacklers run as fast as they need to reach the carrier when he goes down, so a carrier never stands waiting to be hit. */
    private fun arrivalSpeed(
        from: FieldPoint,
        carrier: Track,
        tackleAt: Float,
        reactAt: Float,
        ownSpeed: Float,
    ): Float {
        val needed = from.distanceTo(carrier.at(tackleAt)) / maxOf(tackleAt - reactAt, MIN_CHASE_TIME) * ARRIVAL_MARGIN
        return needed.coerceAtMost(MAX_CHASE_SPEED * positionFactor(ownSpeed))
    }

    /** A lineman never runs a receiver down, so every pursuit ceiling scales with what that position can actually do. */
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
                        closingTarget(carrier),
                    )
                }
            switchAt(tackleAt, chased, hold(chased.at(tackleAt)))
        }
    }

    /**
     * Everyone runs at the ball carrier himself. Holding a fixed trailing radius made pursuit look like it was heading
     * downfield on its own rather than converging on him; the speed differences alone are what string the chase out.
     */
    private fun closingTarget(carrier: Track): PursuitTarget = Pursuit.pursue(carrier)

    /**
     * Everyone chases at a share of the carrier's own pace, scaled by what their position can do, so a lineman still runs
     * the play out instead of standing still while staying well behind the backs and defensive backs.
     */
    private fun trailSpeed(
        index: Int,
        speed: (Int) -> Float,
        carrier: Track,
        reactAt: Float,
        tackleAt: Float,
    ): Float {
        val trail = speed(index) * TRAIL_SPEED_FACTOR
        val paced = carrierPace(carrier, reactAt, tackleAt) * CHASE_PACE_FACTOR * positionFactor(speed(index))
        return maxOf(trail, paced)
    }
}
