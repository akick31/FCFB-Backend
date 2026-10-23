package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

object ScoreboardText {
    private const val ALPHA_SHIFT = 24
    private const val SOLID_ALPHA_THRESHOLD = 110
    private const val TEXT_FILL_SHARE = 0.82f
    private const val MIN_FONT_SIZE = 6
    private const val MAX_FONT_SIZE = 72
    private const val SECONDS_PER_MINUTE = 60
    private const val MAX_TWO_DIGIT = 99

    fun sharedFont(
        g: Graphics2D,
        labels: List<Pair<String, Int>>,
        height: Int,
        family: String = Font.SANS_SERIF,
    ): Font {
        val maxHeight = (height * TEXT_FILL_SHARE).toInt()
        var best = Font(family, Font.BOLD, MIN_FONT_SIZE)
        for (size in MIN_FONT_SIZE..MAX_FONT_SIZE) {
            val candidate = Font(family, Font.BOLD, size)
            val metrics = g.getFontMetrics(candidate)
            val fits =
                metrics.ascent - metrics.descent <= maxHeight &&
                    labels.all { (text, width) -> metrics.stringWidth(text) <= width * TEXT_FILL_SHARE }
            if (!fits) break
            best = candidate
        }
        return best
    }

    fun paintLabel(
        g: Graphics2D,
        text: String,
        centerX: Int,
        top: Int,
        height: Int,
        color: Color,
        font: Font,
    ) {
        val metrics = g.getFontMetrics(font)
        val baseline = top + (height + metrics.ascent - metrics.descent) / 2
        val label = solidText(text, font, metrics.stringWidth(text), metrics.ascent, metrics.descent, color)
        g.drawImage(label, centerX - metrics.stringWidth(text) / 2 - 1, baseline - metrics.ascent - 1, null)
    }

    fun twoDigits(value: Int): String = value.coerceIn(0, MAX_TWO_DIGIT).toString().padStart(2, ' ')

    fun clockText(seconds: Int): String {
        val whole = seconds.coerceAtLeast(0)
        val remainder = (whole % SECONDS_PER_MINUTE).toString().padStart(2, '0')
        return "${whole / SECONDS_PER_MINUTE}:$remainder"
    }

    fun ordinal(value: Int): String =
        when (value) {
            1 -> "1ST"
            2 -> "2ND"
            3 -> "3RD"
            else -> "${value}TH"
        }

    /** Anti-aliased glyph edges are colours outside the GIF palette, so labels are thresholded to solid pixels. */
    private fun solidText(
        text: String,
        font: Font,
        width: Int,
        ascent: Int,
        descent: Int,
        color: Color,
    ): BufferedImage {
        val mask = BufferedImage(width + 2, ascent + descent + 2, BufferedImage.TYPE_INT_ARGB)
        val maskGraphics = mask.createGraphics()
        maskGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        maskGraphics.font = font
        maskGraphics.color = Color.WHITE
        maskGraphics.drawString(text, 1, ascent + 1)
        maskGraphics.dispose()
        val solid = BufferedImage(mask.width, mask.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                if (mask.getRGB(x, y) ushr ALPHA_SHIFT >= SOLID_ALPHA_THRESHOLD) solid.setRGB(x, y, color.rgb)
            }
        }
        return solid
    }
}
