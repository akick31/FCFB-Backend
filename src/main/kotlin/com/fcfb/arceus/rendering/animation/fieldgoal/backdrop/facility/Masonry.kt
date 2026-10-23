package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import java.awt.Color
import java.awt.Graphics2D

object Masonry {
    val brick = Color(146, 72, 52)
    val brickShadow = Color(118, 56, 40)
    val stone = Color(214, 200, 170)
    val stoneShadow = Color(180, 166, 138)
    val glass = Color(46, 56, 68)
    val glassGlint = Color(92, 110, 128)
    val roof = Color(70, 84, 92)

    val PALETTE: List<Color> = listOf(brick, brickShadow, stone, stoneShadow, glass, glassGlint, roof)

    fun band(
        g: Graphics2D,
        color: Color,
        left: Int,
        right: Int,
        top: Int,
        height: Int,
    ) {
        g.color = color
        g.fillRect(left, top, right - left, height.coerceAtLeast(1))
    }

    fun window(
        g: Graphics2D,
        centerX: Int,
        top: Int,
        width: Int,
        height: Int,
    ) {
        g.color = stone
        g.fillRect(centerX - width / 2 - 1, top - 1, width + 2, height + 3)
        g.color = glass
        g.fillRect(centerX - width / 2, top, width, height)
        g.color = glassGlint
        g.fillRect(centerX - width / 2, top, width, (height / 4).coerceAtLeast(1))
        g.color = stone
        g.fillRect(centerX, top, 1, height)
    }

    fun oculus(
        g: Graphics2D,
        centerX: Int,
        centerY: Int,
        diameter: Int,
    ) {
        g.color = stone
        g.fillOval(centerX - diameter / 2 - 2, centerY - diameter / 2 - 2, diameter + 4, diameter + 4)
        g.color = glass
        g.fillOval(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter)
        g.color = stone
        g.fillRect(centerX, centerY - diameter / 2, 1, diameter)
        g.fillRect(centerX - diameter / 2, centerY, diameter, 1)
    }

    fun ribbon(
        g: Graphics2D,
        left: Int,
        right: Int,
        top: Int,
        height: Int,
        mullionPitch: Int,
    ) {
        g.color = glass
        g.fillRect(left, top, right - left, height)
        g.color = glassGlint
        g.fillRect(left, top, right - left, (height / 5).coerceAtLeast(1))
        g.color = stoneShadow
        var x = left + mullionPitch
        while (x < right) {
            g.fillRect(x, top, 1, height)
            x += mullionPitch
        }
    }

    fun archedWindow(
        g: Graphics2D,
        centerX: Int,
        top: Int,
        width: Int,
        height: Int,
        surround: Color = stone,
    ) {
        g.color = surround
        g.fillRect(centerX - width / 2 - 2, top + width / 2 - 1, width + 4, height - width / 2 + 2)
        g.fillOval(centerX - width / 2 - 2, top - 2, width + 4, width + 4)
        g.color = glass
        g.fillRect(centerX - width / 2, top + width / 2, width, height - width / 2)
        g.fillOval(centerX - width / 2, top, width, width)
    }

    fun lancet(
        g: Graphics2D,
        centerX: Int,
        top: Int,
        width: Int,
        height: Int,
    ) {
        val point = width
        g.color = stoneShadow
        g.fillPolygon(
            intArrayOf(centerX - width / 2 - 2, centerX, centerX + width / 2 + 2, centerX + width / 2 + 2, centerX - width / 2 - 2),
            intArrayOf(top + point, top - 3, top + point, top + height + 2, top + height + 2),
            5,
        )
        g.color = glass
        g.fillPolygon(
            intArrayOf(centerX - width / 2, centerX, centerX + width / 2, centerX + width / 2, centerX - width / 2),
            intArrayOf(top + point, top, top + point, top + height, top + height),
            5,
        )
    }

    fun clock(
        g: Graphics2D,
        centerX: Int,
        centerY: Int,
        diameter: Int,
    ) {
        g.color = stoneShadow
        g.fillOval(centerX - diameter / 2 - 3, centerY - diameter / 2 - 3, diameter + 6, diameter + 6)
        g.color = stone
        g.fillOval(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter)
        g.color = glass
        g.fillRect(centerX, centerY - diameter * 2 / 5, 1, diameter * 2 / 5)
        g.fillRect(centerX, centerY, diameter / 4, 1)
    }
}
