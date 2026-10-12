package com.fcfb.arceus.rendering.animation.assets

import com.fcfb.arceus.util.Logger
import java.awt.Font
import java.awt.GraphicsEnvironment

/** Bundled display faces for the renderers. The JVM only guarantees its own families, so a real block face is loaded here. */
object AnimationFonts {
    private const val GRADUATE = "/Graduate-Regular.ttf"
    private const val WYOMING = "/WyomingCowboys.otf"

    /** The jersey-style number face used on helmets. Falls back to a bold sans if the bundled file cannot be read. */
    val graduate: Font by lazy { loadAndRegister(GRADUATE) ?: fallback() }

    /** The Wyoming Cowboys collegiate face, offered as an end zone / number font. */
    val wyoming: Font by lazy { loadAndRegister(WYOMING) ?: fallback() }

    private fun loadAndRegister(resource: String): Font? =
        try {
            AnimationFonts::class.java.getResourceAsStream(resource)?.use { stream ->
                Font.createFont(Font.TRUETYPE_FONT, stream).also {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(it)
                }
            }
        } catch (e: Exception) {
            Logger.error("Could not load the font $resource: ${e.message}")
            null
        }

    private fun fallback(): Font = Font(Font.SANS_SERIF, Font.BOLD, 1)
}
