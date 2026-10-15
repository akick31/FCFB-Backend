package com.fcfb.arceus.rendering.animation.appearance

import java.awt.Color

class ColorToken {
    fun resolve(
        value: String?,
        primary: Color,
        secondary: Color,
        tertiary: Color,
    ): Color? =
        when {
            value.isNullOrBlank() -> null
            value.equals("PRIMARY", ignoreCase = true) -> primary
            value.equals("SECONDARY", ignoreCase = true) -> secondary
            value.equals("TERTIARY", ignoreCase = true) -> tertiary
            value.equals("WHITE", ignoreCase = true) -> Color.WHITE
            value.equals("BLACK", ignoreCase = true) -> Color.BLACK
            value.equals("HOME", ignoreCase = true) -> primary
            value.equals("AWAY", ignoreCase = true) -> primary
            else -> FieldColors.parseColor(value)
        }
}
