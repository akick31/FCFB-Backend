package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import java.awt.Color

class MountainRange(
    val color: Color,
    val peakLow: Float,
    val peakHigh: Float,
    val saddleLow: Float,
    val saddleHigh: Float,
    val snowLine: Float? = null,
    val shadow: Color = color,
    val snowShadow: Color = color,
    val anchorSegments: Int = 32,
    val roughness: Float = 0.5f,
    val displacement: Float = 0.14f,
) {
    val palette: List<Color> get() = listOf(color, shadow, snowShadow)
}
