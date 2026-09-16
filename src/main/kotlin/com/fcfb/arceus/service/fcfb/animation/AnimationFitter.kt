package com.fcfb.arceus.service.fcfb.animation

import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Discord rejects attachments over 8MB, so an animation is encoded at full fidelity and only degraded when it will not fit.
 * The palette is thinned first — it drops logo shades before anything structural — and frames are only dropped after that,
 * since a lower frame rate is the most visible loss.
 */
@Component
class AnimationFitter(
    private val encoder: AnimatedGifEncoder,
) {
    fun fit(
        frames: List<BufferedImage>,
        palette: List<Color>,
    ): ByteArray {
        var smallest = encoder.encode(frames, palette)
        if (smallest.size <= SIZE_LIMIT_BYTES) return smallest
        for (colors in PALETTE_STEPS) {
            if (colors >= palette.size) continue
            val attempt = encoder.encode(frames, palette.take(colors))
            if (attempt.size <= SIZE_LIMIT_BYTES) return attempt
            smallest = attempt
        }
        for (keepEvery in FRAME_STEPS) {
            val thinned = frames.filterIndexed { index, _ -> index % keepEvery == 0 || index == frames.lastIndex }
            val attempt = encoder.encode(thinned, palette.take(PALETTE_STEPS.last()), FRAME_DELAY * keepEvery)
            if (attempt.size <= SIZE_LIMIT_BYTES) return attempt
            smallest = attempt
        }
        return smallest
    }

    private companion object {
        const val SIZE_LIMIT_BYTES = 7_800_000
        const val FRAME_DELAY = 12
        val PALETTE_STEPS = listOf(192, 128, 96, 64)
        val FRAME_STEPS = listOf(2, 3, 4)
    }
}
