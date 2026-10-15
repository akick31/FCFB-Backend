package com.fcfb.arceus.rendering.animation.assets

import com.fcfb.arceus.util.Logger
import java.awt.Font
import java.awt.GraphicsEnvironment

object AnimationFonts {
    private const val GRADUATE = "/Graduate-Regular.ttf"
    private const val WYOMING = "/WyomingCowboys.otf"

    val graduate: Font by lazy { loadAndRegister(GRADUATE) ?: fallback() }

    val wyoming: Font by lazy { loadAndRegister(WYOMING) ?: fallback() }

    private fun loadAndRegister(resource: String): Font? =
        try {
            AnimationFonts::class.java.getResourceAsStream(resource)?.use { stream ->
                Font.createFont(Font.TRUETYPE_FONT, stream).also {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(it)
                }
            }
        } catch (e: Exception) {
            Logger().error("Could not load the font $resource: ${e.message}")
            null
        }

    private fun fallback(): Font = Font(Font.SANS_SERIF, Font.BOLD, 1)
}
