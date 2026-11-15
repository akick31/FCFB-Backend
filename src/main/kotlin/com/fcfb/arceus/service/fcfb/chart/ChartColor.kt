package com.fcfb.arceus.service.fcfb.chart

import java.awt.Color
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow

internal object ChartColor {
    private const val MIN_CONTRAST = 3.0
    private const val MIN_OPPONENT_DISTANCE = 90.0
    private const val NEUTRAL_SPREAD = 40
    private const val MIN_HUE_GAP = 40.0
    private val ACCENT_FALLBACKS = listOf(Color(0x3d8bd9), Color(0xe0a526), Color(0x2fa56b), Color(0xd9653d))

    fun select(
        primary: Color?,
        secondary: Color?,
        background: Color,
    ): Color = listOfNotNull(primary, secondary).firstOrNull { isVisible(it, background) } ?: Color.WHITE

    fun selectMatchup(
        homePrimary: Color?,
        homeSecondary: Color?,
        awayPrimary: Color?,
        awaySecondary: Color?,
        background: Color,
    ): Pair<Color, Color> {
        val home = select(homePrimary, homeSecondary, background)
        val accents = ACCENT_FALLBACKS.filter { isVisible(it, background) }
        val homeBrand = listOfNotNull(homePrimary, homeSecondary)
        val awayPool =
            listOfNotNull(awayPrimary, awaySecondary).filter { isVisible(it, background) } +
                Color.WHITE +
                accents.filter { accent -> homeBrand.all { distinctFrom(accent, it) } }
        val away = awayPool.firstOrNull { distinctFrom(it, home) } ?: accents.firstOrNull() ?: ACCENT_FALLBACKS.first()
        return home to away
    }

    private fun isVisible(
        color: Color,
        background: Color,
    ): Boolean {
        val foregroundLuminance = luminance(color)
        val backgroundLuminance = luminance(background)
        val contrast =
            (maxOf(foregroundLuminance, backgroundLuminance) + 0.05) /
                (minOf(foregroundLuminance, backgroundLuminance) + 0.05)
        return contrast >= MIN_CONTRAST
    }

    private fun distinctFrom(
        color: Color,
        other: Color,
    ): Boolean {
        val colorNeutral = isNeutral(color)
        val otherNeutral = isNeutral(other)
        if (colorNeutral && otherNeutral) return false
        if (!colorNeutral && !otherNeutral && sameHueFamily(color, other)) return false
        val distance =
            hypot(
                hypot((color.red - other.red).toDouble(), (color.green - other.green).toDouble()),
                (color.blue - other.blue).toDouble(),
            )
        return distance >= MIN_OPPONENT_DISTANCE
    }

    private fun isNeutral(color: Color): Boolean {
        val channels = listOf(color.red, color.green, color.blue)
        return channels.max() - channels.min() < NEUTRAL_SPREAD
    }

    private fun sameHueFamily(
        color: Color,
        other: Color,
    ): Boolean {
        val gap = abs(hue(color) - hue(other))
        return minOf(gap, 360 - gap) < MIN_HUE_GAP
    }

    private fun hue(color: Color): Double {
        val red = color.red.toDouble()
        val green = color.green.toDouble()
        val blue = color.blue.toDouble()
        val max = maxOf(red, green, blue)
        val delta = max - minOf(red, green, blue)
        return when (max) {
            red -> ((green - blue) / delta * 60 + 360) % 360
            green -> (blue - red) / delta * 60 + 120
            else -> (red - green) / delta * 60 + 240
        }
    }

    private fun luminance(color: Color): Double =
        0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)

    private fun linearize(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
}
