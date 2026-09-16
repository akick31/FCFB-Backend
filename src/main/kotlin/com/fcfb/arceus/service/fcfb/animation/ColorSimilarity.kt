package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color
import kotlin.math.sqrt

/** "Redmean" distance: a cheap RGB distance weighted to track how different two colors look rather than how different their numbers are. */
object ColorSimilarity {
    private const val SIMILAR_DISTANCE = 120.0
    private val FIELD_GREENS = listOf(FieldBackgroundPainter.TURF_COLOR, Color(0, 128, 0), Color(46, 139, 87))

    fun areSimilar(
        first: Color,
        second: Color,
    ): Boolean = distance(first, second) < SIMILAR_DISTANCE

    fun isNearGreen(color: Color): Boolean = FIELD_GREENS.any { areSimilar(color, it) }

    private fun distance(
        first: Color,
        second: Color,
    ): Double {
        val meanRed = (first.red + second.red) / 2.0
        val red = (first.red - second.red).toDouble()
        val green = (first.green - second.green).toDouble()
        val blue = (first.blue - second.blue).toDouble()
        return sqrt((2 + meanRed / 256) * red * red + 4 * green * green + (2 + (255 - meanRed) / 256) * blue * blue)
    }
}
