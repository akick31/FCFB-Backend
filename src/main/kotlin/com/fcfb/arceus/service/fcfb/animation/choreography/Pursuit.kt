package com.fcfb.arceus.service.fcfb.animation.choreography

typealias PursuitTarget = (progress: Float, from: FieldPoint) -> FieldPoint

/** Speeds are yards covered over the whole motion phase (~3 seconds of play). */
object Pursuit {
    const val LINEMAN_SPEED = 19f
    const val LINEBACKER_SPEED = 25f
    const val DEFENSIVE_BACK_SPEED = 28f
    const val COVERAGE_SPEED = 32f

    private const val SAMPLE_COUNT = 240
    private const val LEAD = 0.015f
    private const val TACKLE_RADIUS = 1.3f
    private const val TACKLE_APPROACH = 0.12f
    private val tackleSlots =
        listOf(FieldPoint(1f, 0f), FieldPoint(0.35f, 1f), FieldPoint(0.35f, -1f), FieldPoint(-0.7f, 0.8f))

    fun chase(
        before: Track,
        reactAt: Float,
        speed: Float,
        target: PursuitTarget,
    ): Track {
        val step = speed / SAMPLE_COUNT
        var position = before.at(0f)
        val samples =
            (0..SAMPLE_COUNT).map { index ->
                val progress = index.toFloat() / SAMPLE_COUNT
                position = if (progress < reactAt) before.at(progress) else position.stepToward(target(progress, position), step)
                position
            }
        return SampledTrack(samples)
    }

    /**
     * A tackler attacks where the carrier is now, with only a small lead and a slot offset so several of them arrive
     * from different angles. Aiming at a fixed point ahead of him made a defender run to the end of the play and wait
     * there, which reads as running downfield rather than chasing the ball.
     */
    fun tackle(
        carrier: Track,
        direction: Float,
        slot: Int,
        tackleAt: Float,
    ): PursuitTarget {
        val base = tackleSlots[slot % tackleSlots.size]
        val offset = FieldPoint(base.along * direction, base.lateral) * TACKLE_RADIUS
        return { progress, _ -> carrier.at(minOf(progress + LEAD, tackleAt)) + offset * closingFraction(progress, tackleAt) }
    }

    /** The slot offset collapses to nothing as the tackle arrives, so the last step is into the carrier, not past him. */
    private fun closingFraction(
        progress: Float,
        tackleAt: Float,
    ): Float = ((tackleAt - progress) / TACKLE_APPROACH).coerceIn(0f, 1f)

    fun trail(
        carrier: Track,
        radius: Float,
    ): PursuitTarget =
        { progress, from ->
            val ball = carrier.at(progress)
            val distance = from.distanceTo(ball)
            if (distance < 0.01f) ball else ball + (from - ball) * (radius / distance)
        }

    fun toward(point: FieldPoint): PursuitTarget = { _, _ -> point }

    /**
     * Aims straight at the carrier rather than holding station off him. Anyone already in his path has to close on him,
     * because trailing at a fixed radius makes a defender back away to keep the gap as the carrier arrives.
     */
    fun pursue(carrier: Track): PursuitTarget = { progress, _ -> carrier.at(progress) }

    fun closest(
        positions: List<FieldPoint>,
        candidates: Iterable<Int>,
        spot: FieldPoint,
        count: Int,
    ): List<Int> = candidates.sortedBy { positions[it].distanceTo(spot) }.take(count)
}
