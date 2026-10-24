package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import java.awt.Color
import java.awt.Graphics2D

object AthleticCenter : LandmarkBuilding {
    private val limestone = Color(200, 182, 148)
    private val limestoneShadow = Color(172, 154, 122)
    private val glass = Color(96, 132, 156)
    private val glassReflection = Color(150, 182, 200)
    private val mullion = Color(64, 84, 100)
    private val canopy = Color(240, 240, 236)

    override val palette: List<Color> = listOf(limestone, limestoneShadow, glass, glassReflection, mullion, canopy)

    override fun paint(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        width: Int,
        unit: Float,
    ) {
        val height = (unit * 0.52f).toInt()
        val top = groundY - height
        g.color = limestone
        g.fillRect(left, top, width, height)
        g.color = limestoneShadow
        g.fillRect(left, top + height / 2, width, 1)
        val glassLeft = left + width / 5
        val glassWidth = width * 3 / 5
        val glassTop = top + (unit * 0.06f).toInt()
        g.color = glass
        g.fillRect(glassLeft, glassTop, glassWidth, groundY - glassTop)
        g.color = glassReflection
        g.fillRect(glassLeft + glassWidth / 10, glassTop, glassWidth / 8, groundY - glassTop)
        g.fillRect(glassLeft + glassWidth * 6 / 10, glassTop, glassWidth / 14, groundY - glassTop)
        g.color = mullion
        val pitch = (unit * 0.06f).toInt().coerceAtLeast(3)
        var x = glassLeft
        while (x < glassLeft + glassWidth) {
            g.fillRect(x, glassTop, 1, groundY - glassTop)
            x += pitch
        }
        var y = glassTop + pitch * 2
        while (y < groundY) {
            g.fillRect(glassLeft, y, glassWidth, 1)
            y += pitch * 2
        }
        val canopyHeight = (unit * 0.035f).toInt().coerceAtLeast(2)
        g.color = canopy
        g.fillRect(left - width / 40, top - canopyHeight, width + width / 20, canopyHeight)
    }
}
