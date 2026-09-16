package com.fcfb.arceus.service.fcfb.animation.choreography

/**
 * Blockers and receivers finish their assignment long before the whistle, and a waypoint track holds its last position, so
 * without this they stand still while the play is still running. They keep working toward the carrier until he is down.
 */
object DownfieldEscort {
    private const val TRAIL_RADIUS = 2.6f
    private const val TRAIL_STEP = 1.4f

    fun follow(
        before: List<Track>,
        carrier: Track,
        from: Float,
        until: Float,
        exclude: Set<Int>,
        speed: (Int) -> Float,
    ): List<Track> =
        before.mapIndexed { index, track ->
            if (index in exclude) {
                track
            } else {
                val radius = TRAIL_RADIUS + (index % 3) * TRAIL_STEP
                val chased = Pursuit.chase(track, from, speed(index), Pursuit.trail(carrier, radius))
                switchAt(until, chased, hold(chased.at(until)))
            }
        }
}
