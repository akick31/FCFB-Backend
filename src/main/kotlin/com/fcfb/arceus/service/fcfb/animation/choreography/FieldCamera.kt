package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter

/** Broadcast-style view: the field is drawn at [zoom] and a canvas-sized window pans with the ball. */
class FieldCamera(
    private val ball: BallTrack,
    private val lookAhead: Float,
    val zoom: Float = ZOOM,
) {
    private val maxOffsetX = FieldBackgroundPainter.WIDTH * zoom - FieldBackgroundPainter.WIDTH
    private val maxOffsetY = FieldBackgroundPainter.HEIGHT * zoom - FieldBackgroundPainter.HEIGHT

    fun offsetX(progress: Float): Int {
        val focus = smoothed(progress) { it.along } + lookAhead
        return (fieldX(focus) - FieldBackgroundPainter.WIDTH / 2f).coerceIn(0f, maxOffsetX).toInt()
    }

    fun offsetY(progress: Float): Int {
        val focus = smoothed(progress) { it.lateral } * LATERAL_FOLLOW
        return (fieldY(focus) - FieldBackgroundPainter.HEIGHT / 2f).coerceIn(0f, maxOffsetY).toInt()
    }

    fun fieldX(along: Float): Float {
        val playableWidth = FieldBackgroundPainter.WIDTH - 2 * FieldBackgroundPainter.MARGIN
        return (FieldBackgroundPainter.MARGIN + along * playableWidth / 100f) * zoom
    }

    fun fieldY(lateral: Float): Float =
        (FieldBackgroundPainter.HEIGHT / 2f + lateral.coerceIn(-MAX_LATERAL, MAX_LATERAL) * LATERAL_PIXELS_PER_YARD) * zoom

    private fun smoothed(
        progress: Float,
        axis: (FieldPoint) -> Float,
    ): Float {
        val samples = (0..SMOOTHING_SAMPLES).map { (progress - SMOOTHING_WINDOW * it / SMOOTHING_SAMPLES).coerceAtLeast(0f) }
        return samples.map { axis(ball.at(it).position) }.average().toFloat()
    }

    companion object {
        const val ZOOM = 1.45f
        private const val LATERAL_PIXELS_PER_YARD = 7.2f
        private const val MAX_LATERAL = 25.5f
        private const val LATERAL_FOLLOW = 0.6f
        private const val SMOOTHING_WINDOW = 0.08f
        private const val SMOOTHING_SAMPLES = 8
    }
}
