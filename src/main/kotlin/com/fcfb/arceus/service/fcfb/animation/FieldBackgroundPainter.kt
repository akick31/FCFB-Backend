package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

object FieldBackgroundPainter {
    const val WIDTH = 500
    const val HEIGHT = 220
    const val MARGIN = 40

    val TURF_COLOR: Color = Color(34, 102, 51)
    val LINE_COLOR: Color = Color.WHITE
    val BALL_COLOR: Color = Color(101, 67, 33)

    fun paint(
        homeTeam: Team,
        awayTeam: Team,
    ): BufferedImage {
        val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.color = TURF_COLOR
        g.fillRect(0, 0, WIDTH, HEIGHT)

        g.color = parseColor(homeTeam.primaryColor)
        g.fillRect(0, 0, MARGIN, HEIGHT)
        g.color = parseColor(awayTeam.primaryColor)
        g.fillRect(WIDTH - MARGIN, 0, MARGIN, HEIGHT)

        g.color = LINE_COLOR
        g.font = Font("Arial", Font.PLAIN, 10)
        for (yard in 0..100 step 10) {
            val x = FieldCoordinateMapper.toPixelX(yard, WIDTH, MARGIN)
            g.drawLine(x, 20, x, HEIGHT - 20)
            val label = if (yard <= 50) yard.toString() else (100 - yard).toString()
            val labelWidth = g.fontMetrics.stringWidth(label)
            g.drawString(label, x - labelWidth / 2, 16)
            g.drawString(label, x - labelWidth / 2, HEIGHT - 8)
        }

        g.dispose()
        return image
    }

    fun drawBall(
        image: BufferedImage,
        x: Int,
        y: Int,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = BALL_COLOR
        g.fillOval(x - 6, y - 4, 12, 8)
        g.color = LINE_COLOR
        g.drawLine(x - 3, y, x + 3, y)
        g.dispose()
    }

    fun parseColor(colorString: String?): Color {
        return try {
            Color.decode(colorString ?: "#000000")
        } catch (e: NumberFormatException) {
            Color.BLACK
        }
    }
}
