package com.fcfb.arceus.service.fcfb.scorebug

import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

@Component
class ChewBadgeOverlay {
    fun apply(source: BufferedImage): BufferedImage {
        val image = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.drawImage(source, 0, 0, null)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val barHeight = source.height * BOTTOM_BAR_FRACTION
        val pillHeight = barHeight * 0.56
        g.font = loadFont((pillHeight * 0.58).toFloat())

        val textWidth = g.fontMetrics.stringWidth(LABEL)
        val padX = pillHeight * 0.44
        val pillWidth = textWidth + padX * 2
        val x = source.width - MARGIN - pillWidth
        val y = source.height - barHeight / 2 - pillHeight / 2

        g.color = AMBER
        g.fill(RoundRectangle2D.Double(x, y, pillWidth, pillHeight, pillHeight * 0.5, pillHeight * 0.5))

        g.color = TEXT
        val baseline = y + (pillHeight + g.fontMetrics.ascent - g.fontMetrics.descent) / 2
        g.drawString(LABEL, (x + padX).toFloat(), baseline.toFloat())
        g.dispose()
        return image
    }

    private fun loadFont(size: Float): Font {
        val stream = this::class.java.classLoader.getResourceAsStream("Helvetica-Bold.ttf")
        return if (stream != null) {
            Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(Font.BOLD, size)
        } else {
            Font("SansSerif", Font.BOLD, size.toInt())
        }
    }

    companion object {
        private const val LABEL = "CHEW"
        private const val BOTTOM_BAR_FRACTION = 60.0 / 400.0
        private const val MARGIN = 6.0
        private val AMBER = Color(0xE8, 0xA2, 0x3D)
        private val TEXT = Color(0x24, 0x1A, 0x06)
    }
}
