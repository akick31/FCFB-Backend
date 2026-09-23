package com.fcfb.arceus.service.fcfb.animation.choreography

object WeavingRun {
    private const val MIN_WEAVE_YARDS = 6f
    private const val YARDS_PER_CUT = 7f
    private const val MAX_CUTS = 3
    private const val CUT_WIDTH = 4f
    private const val AVOID_RADIUS = 7f
    private const val SAME_LANE = 0.1f

    fun between(
        from: FieldPoint,
        to: FieldPoint,
        startAt: Float,
        endAt: Float,
        side: Float,
        clearAlong: Float,
        traffic: List<Track>,
    ): Track {
        val direction = if (to.along >= from.along) 1f else -1f
        val trafficYards = ((clearAlong - from.along) * direction).coerceAtLeast(0f)
        val runYards = ((to.along - from.along) * direction).coerceAtLeast(0f)
        val weaveYards = minOf(trafficYards, runYards)
        val cuts = minOf(MAX_CUTS, (weaveYards / YARDS_PER_CUT).toInt())
        if (weaveYards < MIN_WEAVE_YARDS || cuts <= 0 || runYards <= 0f) return path(startAt to from, endAt to to)

        val waypoints = mutableListOf(startAt to from)
        for (cut in 1..cuts) {
            val along = from.along + direction * weaveYards * cut / (cuts + 1)
            val fraction = ((along - from.along) * direction) / runYards
            val at = startAt + (endAt - startAt) * fraction
            val lateral = from.lateral + (to.lateral - from.lateral) * fraction
            val straight = FieldPoint(along, lateral)
            val threat = traffic.map { it.at(at) }.minByOrNull { it.distanceTo(straight) } ?: continue
            if (threat.distanceTo(straight) > AVOID_RADIUS) continue
            val swing =
                when {
                    threat.lateral > lateral + SAME_LANE -> -1f
                    threat.lateral < lateral - SAME_LANE -> 1f
                    else -> side
                }
            waypoints += at to FieldPoint(along, lateral + swing * CUT_WIDTH)
        }
        if (waypoints.size == 1) return path(startAt to from, endAt to to)
        waypoints += endAt to to
        return path(*waypoints.toTypedArray())
    }
}
