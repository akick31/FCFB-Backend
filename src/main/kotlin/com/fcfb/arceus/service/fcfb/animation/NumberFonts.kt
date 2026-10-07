package com.fcfb.arceus.service.fcfb.animation

import java.awt.Font

object NumberFonts {
    fun font(
        key: String?,
        size: Float,
    ): Font {
        val baseKey = key?.removeSuffix(EndZoneFonts.BOLD_SUFFIX)
        val style = EndZoneFonts.styleFor(key)
        if (baseKey.isNullOrBlank() || baseKey.equals("CLASSIC", ignoreCase = true)) {
            return AnimationFonts.graduate.deriveFont(style, size)
        }
        val family = EndZoneFonts.familyOf(baseKey)
        if (family == AnimationFonts.graduate.family) return AnimationFonts.graduate.deriveFont(style, size)
        return Font(family, style, size.toInt())
    }
}
