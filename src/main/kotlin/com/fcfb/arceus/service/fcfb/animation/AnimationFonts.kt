package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.Logger
import java.awt.Font

/** Bundled display faces for the renderers. The JVM only guarantees its own families, so a real block face is loaded here. */
object AnimationFonts {
    private const val GRADUATE = "/Graduate-Regular.ttf"

    /** The jersey-style number face used on helmets. Falls back to a bold sans if the bundled file cannot be read. */
    val graduate: Font by lazy {
        try {
            AnimationFonts::class.java.getResourceAsStream(GRADUATE)?.use { Font.createFont(Font.TRUETYPE_FONT, it) }
                ?: fallback()
        } catch (e: Exception) {
            Logger.error("Could not load the Graduate font: ${e.message}")
            fallback()
        }
    }

    private fun fallback(): Font = Font(Font.SANS_SERIF, Font.BOLD, 1)
}
