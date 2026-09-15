package com.fcfb.arceus.service.fcfb.animation.choreography

class SampledTrack(
    private val samples: List<FieldPoint>,
) : Track {
    override fun at(progress: Float): FieldPoint {
        val position = progress.coerceIn(0f, 1f) * (samples.size - 1)
        val index = position.toInt().coerceAtMost(samples.size - 2)
        return samples[index].lerp(samples[index + 1], position - index)
    }
}
