package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object ModernCampus : CampusStyle {
    private enum class Kind { PAVILION, SLAB, CURVED, CANTILEVER }

    private val glass = Color(108, 144, 166)
    private val glassReflection = Color(160, 190, 206)
    private val mullion = Color(70, 88, 102)
    private val concrete = Color(196, 192, 184)
    private val concreteShadow = Color(164, 160, 152)
    private val ribbon = Color(58, 74, 88)
    private val accent = Color(196, 120, 64)
    private val slimTree = Color(70, 118, 60)
    private val trunk = Color(90, 74, 60)

    override val kindCount = Kind.entries.size

    override val palette: List<Color> =
        listOf(glass, glassReflection, mullion, concrete, concreteShadow, ribbon, accent, slimTree, trunk)

    override fun paintBuilding(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
        kind: Int,
    ): Int =
        when (Kind.entries[kind]) {
            Kind.PAVILION -> paintPavilion(g, random, left, groundY, unit)
            Kind.SLAB -> paintSlab(g, random, left, groundY, unit)
            Kind.CURVED -> paintCurved(g, left, groundY, unit)
            Kind.CANTILEVER -> paintCantilever(g, left, groundY, unit)
        }

    override fun paintTree(
        g: Graphics2D,
        random: Random,
        centerX: Int,
        groundY: Int,
        height: Int,
    ) {
        val width = (height / 3).coerceAtLeast(2)
        g.color = trunk
        g.fillRect(centerX, groundY - height / 4, 1.coerceAtLeast(width / 8), height / 4)
        g.color = slimTree
        g.fillOval(centerX - width / 2, groundY - height, width, height * 4 / 5)
    }

    private fun glassFacade(
        g: Graphics2D,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        unit: Float,
    ) {
        g.color = glass
        g.fillRect(left, top, width, height)
        g.color = glassReflection
        g.fillRect(left + width / 6, top, width / 6, height)
        g.color = mullion
        val pitch = (unit * 0.07f).toInt().coerceAtLeast(3)
        var x = left + pitch
        while (x < left + width) {
            g.fillRect(x, top, 1, height)
            x += pitch
        }
        val floor = (unit * 0.1f).toInt().coerceAtLeast(3)
        var y = top + floor
        while (y < top + height) {
            g.fillRect(left, y, width, 1)
            y += floor
        }
    }

    private fun paintPavilion(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * (1.4f + random.nextFloat() * 0.6f)).toInt()
        val height = (unit * 0.24f).toInt()
        val roof = (unit * 0.03f).toInt().coerceAtLeast(2)
        glassFacade(g, left, groundY - height, width, height, unit)
        g.color = concrete
        g.fillRect(left - roof * 2, groundY - height - roof, width + roof * 4, roof)
        return width
    }

    private fun paintSlab(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * (0.9f + random.nextFloat() * 0.4f)).toInt()
        val height = (unit * (0.5f + random.nextFloat() * 0.15f)).toInt()
        val top = groundY - height
        g.color = concrete
        g.fillRect(left, top, width, height)
        g.color = concreteShadow
        g.fillRect(left + width - width / 10, top, width / 10, height)
        g.color = ribbon
        val floor = (unit * 0.1f).toInt().coerceAtLeast(3)
        var y = top + floor / 2
        while (y + floor / 3 < groundY) {
            g.fillRect(left + width / 20, y, width - width / 5, (floor / 3).coerceAtLeast(1))
            y += floor
        }
        g.color = accent
        g.fillRect(left + width - width / 10, top + height / 3, width / 10, height / 6)
        return width
    }

    private fun paintCurved(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 1.1f).toInt()
        val height = (unit * 0.36f).toInt()
        val top = groundY - height
        glassFacade(g, left, top, width, height, unit)
        g.color = concrete
        g.fillArc(left, top - height / 3, width, height * 2 / 3, 0, 180)
        g.fillRect(left, top, width, (unit * 0.02f).toInt().coerceAtLeast(1))
        return width
    }

    private fun paintCantilever(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 1.0f).toInt()
        val baseHeight = (unit * 0.22f).toInt()
        val upperHeight = (unit * 0.2f).toInt()
        glassFacade(g, left, groundY - baseHeight, width * 2 / 3, baseHeight, unit)
        g.color = concrete
        g.fillRect(left + width / 6, groundY - baseHeight - upperHeight, width * 5 / 6, upperHeight)
        g.color = ribbon
        g.fillRect(left + width / 6 + width / 20, groundY - baseHeight - upperHeight * 2 / 3, width * 5 / 6 - width / 10, upperHeight / 3)
        return width
    }
}
