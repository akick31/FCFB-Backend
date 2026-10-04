package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

/**
 * Recolors chosen parts of a conference logo. The user maps specific source colors in the logo to one of their team
 * tokens (PRIMARY/SECONDARY/TERTIARY/WHITE/BLACK); every other color, including whites they did not map, is left alone.
 */
object ConferenceLogoTint {
    private val cache = ConcurrentHashMap<String, BufferedImage>()
    private const val MATCH_TOLERANCE = 60 * 60 * 3
    private val ENTRY = Regex("\"(#[0-9A-Fa-f]{6})\"\\s*:\\s*\"([A-Za-z]+)\"")

    fun load(
        url: String?,
        colorMapJson: String?,
        primary: Color,
        secondary: Color,
        tertiary: Color,
    ): BufferedImage? {
        val source = LogoLoader.load(url) ?: return null
        val entries = parse(colorMapJson, primary, secondary, tertiary)
        if (entries.isEmpty()) return source
        return cache.getOrPut("$url@${colorMapJson.hashCode()}@${primary.rgb}@${secondary.rgb}@${tertiary.rgb}") {
            remap(source, entries)
        }
    }

    /** The distinct opaque colors a logo is mostly made of, as hex, so the editor can offer each one for recoloring. */
    fun dominantColors(
        url: String?,
        limit: Int = 8,
    ): List<String> {
        val source = LogoLoader.load(url) ?: return emptyList()
        val counts = HashMap<Int, Int>()
        var total = 0
        for (y in 0 until source.height step 2) {
            for (x in 0 until source.width step 2) {
                val argb = source.getRGB(x, y)
                if (argb ushr 24 and 0xFF < 128) continue
                total++
                val c = Color(argb)
                val quantized = Color(c.red and 0xF0, c.green and 0xF0, c.blue and 0xF0)
                counts[quantized.rgb] = (counts[quantized.rgb] ?: 0) + 1
            }
        }
        if (total == 0) return emptyList()
        return counts.entries
            .filter { it.value.toFloat() / total >= 0.02f }
            .sortedByDescending { it.value }
            .take(limit)
            .map { String.format("#%06X", it.key and 0xFFFFFF) }
    }

    private fun parse(
        json: String?,
        primary: Color,
        secondary: Color,
        tertiary: Color,
    ): List<Pair<Color, Color>> {
        if (json.isNullOrBlank()) return emptyList()
        return ENTRY.findAll(json).mapNotNull { match ->
            val source = runCatching { Color.decode(match.groupValues[1]) }.getOrNull() ?: return@mapNotNull null
            val target =
                when (match.groupValues[2].uppercase()) {
                    "PRIMARY" -> primary
                    "SECONDARY" -> secondary
                    "TERTIARY" -> tertiary
                    "WHITE" -> Color.WHITE
                    "BLACK" -> Color.BLACK
                    else -> return@mapNotNull null
                }
            source to target
        }.toList()
    }

    private fun remap(
        source: BufferedImage,
        entries: List<Pair<Color, Color>>,
    ): BufferedImage {
        val out = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val argb = source.getRGB(x, y)
                val alpha = argb ushr 24 and 0xFF
                if (alpha == 0) continue
                val pixel = Color(argb)
                val target =
                    entries.minByOrNull {
                        distance(
                            pixel,
                            it.first,
                        )
                    }?.takeIf { distance(pixel, it.first) <= MATCH_TOLERANCE }?.second
                val result = target ?: pixel
                out.setRGB(x, y, (alpha shl 24) or (result.rgb and 0xFFFFFF))
            }
        }
        return out
    }

    private fun distance(
        a: Color,
        b: Color,
    ): Int {
        val dr = a.red - b.red
        val dg = a.green - b.green
        val db = a.blue - b.blue
        return dr * dr + dg * dg + db * db
    }
}
