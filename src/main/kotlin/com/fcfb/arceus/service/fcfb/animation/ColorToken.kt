package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color

/**
 * A per-side postseason color that is either a literal hex value or a token resolved against the two
 * teams playing: HOME and AWAY use each team's primary color, WHITE is literal white.
 */
object ColorToken {
    fun resolve(
        value: String?,
        home: Color,
        away: Color,
    ): Color? =
        when {
            value.isNullOrBlank() -> null
            value.equals("HOME", ignoreCase = true) -> home
            value.equals("AWAY", ignoreCase = true) -> away
            value.equals("WHITE", ignoreCase = true) -> Color.WHITE
            else -> FieldBackgroundPainter.parseColor(value)
        }
}
