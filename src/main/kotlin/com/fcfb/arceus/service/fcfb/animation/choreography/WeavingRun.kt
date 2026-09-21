package com.fcfb.arceus.service.fcfb.animation.choreography

object WeavingRun {
    private const val MIN_WEAVE_YARDS = 6f
    private const val YARDS_PER_CUT = 7f
    private const val MAX_CUTS = 3
    private const val CUT_WIDTH = 4f

    fun between(
        from: FieldPoint,
        to: FieldPoint,
        startAt: Float,
        endAt: Float,
        side: Float,
        clearAlong: Float,
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
            val lateral = from.lateral + (to.lateral - from.lateral) * fraction
            val swing = if (cut % 2 == 0) -side else side
            waypoints += (startAt + (endAt - startAt) * fraction) to FieldPoint(along, lateral + swing * CUT_WIDTH)
        }
        waypoints += endAt to to
        return path(*waypoints.toTypedArray())
    }
}
