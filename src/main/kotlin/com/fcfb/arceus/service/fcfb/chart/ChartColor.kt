package com.fcfb.arceus.service.fcfb.chart

import java.awt.Color
import kotlin.math.pow

internal object ChartColor {
    fun select(
        primary: Color?,
        secondary: Color?,
        background: Color,
    ): Color = listOfNotNull(primary, secondary).firstOrNull { contrast(it, background) >= 3.0 } ?: Color.WHITE

    private fun contrast(
        color: Color,
        background: Color,
    ): Double {
        val foregroundLuminance = luminance(color)
        val backgroundLuminance = luminance(background)
        return (maxOf(foregroundLuminance, backgroundLuminance) + 0.05) /
            (minOf(foregroundLuminance, backgroundLuminance) + 0.05)
    }

    private fun luminance(color: Color): Double =
        0.2126 * linearize(color.red) + 0.7152 * linearize(color.green) + 0.0722 * linearize(color.blue)

    private fun linearize(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
}
