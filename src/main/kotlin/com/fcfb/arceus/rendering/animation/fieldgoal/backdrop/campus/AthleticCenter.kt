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
    private val fin = Color(84, 88, 96)

    override val palette: List<Color> = listOf(limestone, limestoneShadow, glass, glassReflection, mullion, canopy, fin)

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
        val towerWidth = (unit * 0.22f).toInt()
        val towerHeight = (unit * 0.78f).toInt()
        val towerLeft = left + width - width / 8 - towerWidth
        g.color = limestone
        g.fillRect(towerLeft, groundY - towerHeight, towerWidth, towerHeight)
        g.color = fin
        val finWidth = (towerWidth / 10).coerceAtLeast(1)
        repeat(4) { index ->
            g.fillRect(
                towerLeft + towerWidth * (index + 1) / 5,
                groundY - towerHeight + (unit * 0.04f).toInt(),
                finWidth,
                towerHeight * 2 / 3,
            )
        }
        g.color = canopy
        g.fillRect(towerLeft - 2, groundY - towerHeight - canopyHeight, towerWidth + 4, canopyHeight)
    }
}
