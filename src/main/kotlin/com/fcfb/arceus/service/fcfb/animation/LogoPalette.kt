package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color
import java.awt.image.BufferedImage

object LogoPalette {
    private const val CHANNEL_BUCKET = 24
    private const val SAMPLE_STEP = 2
    private const val OPAQUE_ALPHA = 128

    fun dominantColors(
        image: BufferedImage,
        count: Int,
    ): List<Color> {
        val counts = HashMap<Int, Int>()
        for (y in 0 until image.height step SAMPLE_STEP) {
            for (x in 0 until image.width step SAMPLE_STEP) {
                val argb = image.getRGB(x, y)
                if (argb ushr 24 < OPAQUE_ALPHA) continue
                val key = bucket(argb)
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
        return counts.entries.sortedByDescending { it.value }.take(count).map { Color(it.key) }
    }

    private fun bucket(argb: Int): Int {
        val red = center((argb shr 16) and 0xFF)
        val green = center((argb shr 8) and 0xFF)
        val blue = center(argb and 0xFF)
        return (red shl 16) or (green shl 8) or blue
    }

    private fun center(channel: Int): Int = minOf(255, channel / CHANNEL_BUCKET * CHANNEL_BUCKET + CHANNEL_BUCKET / 2)
}
