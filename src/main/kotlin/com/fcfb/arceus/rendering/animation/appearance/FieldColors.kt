package com.fcfb.arceus.rendering.animation.appearance

import java.awt.Color

object FieldColors {
    val TURF_COLOR: Color = Color(34, 102, 51)
    val LINE_COLOR: Color = Color.WHITE
    val BALL_COLOR: Color = Color(101, 67, 33)
    val LOS_COLOR: Color = Color(30, 90, 220)
    val FIRST_DOWN_COLOR: Color = Color(255, 215, 0)

    fun parseColor(colorString: String?): Color {
        return try {
            Color.decode(colorString ?: "#000000")
        } catch (e: NumberFormatException) {
            Color.BLACK
        }
    }
}
