package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.PlayRandom
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.Track
import com.fcfb.arceus.service.fcfb.animation.choreography.path

/** The six linemen firing at the kicker together. Gunners are left out: they are jammed at the line, not rushing. */
internal object PuntRush {
    const val LANES = 6
    const val SNAP_AT = 0.12f
    const val KICK_AT = 0.24f

    private const val LANE_SPACING = 1.7f
    private const val CLOSING_YARDS = 1.2f
    private const val NEAREST_MISS = 0.008f
    private const val MISS_SPREAD = 0.03f

    fun charge(
        start: FieldPoint,
        lane: Int,
        kickerSpot: FieldPoint,
        forward: Float,
        arriveAt: Float,
    ): Track {
        val lateral = kickerSpot.lateral + (lane - (LANES - 1) / 2f) * LANE_SPACING
        val target = FieldPoint(kickerSpot.along + forward * CLOSING_YARDS, lateral)
        return path(0f to start, arriveAt to target)
    }

    fun lateArrivals(random: PlayRandom): List<Float> {
        val closest = random.pick((0 until LANES).toList())
        return (0 until LANES).map { lane ->
            if (lane == closest) KICK_AT + NEAREST_MISS else KICK_AT + NEAREST_MISS + random.between(MISS_SPREAD / 3, MISS_SPREAD)
        }
    }
}
