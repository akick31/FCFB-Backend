package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

/**
 * Recolors a conference logo into a single team's hue: every pixel keeps its relative lightness but takes the team
 * color's hue and saturation, so a conference's own colors (SEC blue + gold, ACC blue, MAC green) read as one team color.
 */
object ConferenceLogoTint {
    private val cache = ConcurrentHashMap<String, BufferedImage>()
    private const val MIN_BRIGHTNESS = 0.15f
    private const val BRIGHTNESS_RANGE = 0.8f

    fun load(
        url: String?,
        tint: Color?,
    ): BufferedImage? {
        val source = LogoLoader.load(url) ?: return null
        if (tint == null) return source
        return cache.getOrPut("$url@${tint.rgb}") { tinted(source, tint) }
    }

    private fun tinted(
        source: BufferedImage,
        tint: Color,
    ): BufferedImage {
        val hsb = Color.RGBtoHSB(tint.red, tint.green, tint.blue, null)
        val out = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val argb = source.getRGB(x, y)
                val alpha = argb ushr 24 and 0xFF
                if (alpha == 0) continue
                val pixel = Color(argb)
                val brightness = Color.RGBtoHSB(pixel.red, pixel.green, pixel.blue, null)[2]
                val recolored = Color.getHSBColor(hsb[0], hsb[1], MIN_BRIGHTNESS + BRIGHTNESS_RANGE * brightness)
                out.setRGB(x, y, (alpha shl 24) or (recolored.rgb and 0xFFFFFF))
            }
        }
        return out
    }
}
