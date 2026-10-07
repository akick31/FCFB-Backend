package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color

/**
 * A per-side field color that is either a literal hex value or a token resolved against the team defending that side:
 * PRIMARY, SECONDARY, and TERTIARY pick that team's color slots; WHITE and BLACK are literal. HOME/AWAY are legacy
 * aliases for the defending team's primary color.
 */
object ColorToken {
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
            else -> FieldBackgroundPainter.parseColor(value)
        }
}
