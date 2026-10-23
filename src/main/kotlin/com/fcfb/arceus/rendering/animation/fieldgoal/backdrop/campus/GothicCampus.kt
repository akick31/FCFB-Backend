package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object GothicCampus : CampusStyle {
    private enum class Kind { HALL, TOWER, CHAPEL, LIBRARY, CLOISTER }

    private val stoneShades = listOf(Color(170, 160, 140), Color(156, 146, 128), Color(182, 172, 152))
    private val slate = Color(62, 68, 76)
    private val pane = Color(40, 46, 58)
    private val mortar = Color(132, 124, 108)

    override val kindCount = Kind.entries.size

    override val palette: List<Color> = stoneShades + listOf(slate, pane, mortar)

    override fun paintBuilding(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
        kind: Int,
    ): Int =
        when (Kind.entries[kind]) {
            Kind.HALL -> paintHall(g, random, left, groundY, unit)
            Kind.TOWER -> paintTower(g, random, left, groundY, unit)
            Kind.CHAPEL -> paintChapel(g, random, left, groundY, unit)
            Kind.LIBRARY -> paintLibrary(g, random, left, groundY, unit)
            Kind.CLOISTER -> paintCloister(g, random, left, groundY, unit)
        }

    private fun battlements(
        g: Graphics2D,
        left: Int,
        top: Int,
        width: Int,
        unit: Float,
    ) {
        val merlon = (unit * 0.03f).toInt().coerceAtLeast(2)
        var x = left
        while (x < left + width) {
            g.fillRect(x, top - merlon, merlon.coerceAtMost(left + width - x), merlon)
            x += merlon * 2
        }
    }

    private fun pinnacle(
        g: Graphics2D,
        centerX: Int,
        baseY: Int,
        unit: Float,
        color: Color,
    ) {
        val width = (unit * 0.035f).toInt().coerceAtLeast(2)
        g.color = color
        g.fillRect(centerX - width / 2, baseY - width * 2, width, width * 2)
        CampusShapes.gable(g, centerX - width / 2, baseY - width * 2, width, width * 2, color)
    }

    private fun paintHall(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * (1.2f + random.nextFloat() * 0.5f)).toInt()
        val height = (unit * (0.4f + random.nextFloat() * 0.1f)).toInt()
        val top = groundY - height
        val stone = stoneShades[random.nextInt(stoneShades.size)]
        CampusShapes.trapezoidRoof(g, left + width / 12, top, width - width / 6, (unit * 0.16f).toInt(), (unit * 0.08f).toInt(), slate)
        g.color = stone
        g.fillRect(left, top, width, height)
        battlements(g, left, top, width, unit)
        g.color = mortar
        g.fillRect(left, top + height / 2, width, 1)
        val arch = (unit * 0.04f).toInt().coerceAtLeast(2)
        CampusShapes.windowGrid(
            g,
            left,
            top,
            width,
            height,
            (unit * 0.09f).toInt().coerceAtLeast(3),
            (unit * 0.2f).toInt().coerceAtLeast(3),
        ) { x, y ->
            CampusShapes.pointedArch(g, x, y - arch * 3 / 2, arch, arch * 3, pane)
        }
        pinnacle(g, left + 1, top, unit, stone)
        pinnacle(g, left + width - 2, top, unit, stone)
        return width
    }

    private fun paintLibrary(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 0.95f).toInt()
        val height = (unit * 0.5f).toInt()
        val top = groundY - height
        val stone = stoneShades[random.nextInt(stoneShades.size)]
        val turret = (unit * 0.12f).toInt().coerceAtLeast(3)
        CampusShapes.gable(g, left + turret, top, width - turret * 2, (unit * 0.2f).toInt(), slate)
        g.color = stone
        g.fillRect(left + turret, top, width - turret * 2, height)
        listOf(left, left + width - turret).forEach { turretLeft ->
            g.color = stone
            g.fillRect(turretLeft, top - (unit * 0.12f).toInt(), turret, height + (unit * 0.12f).toInt())
            CampusShapes.gable(g, turretLeft - 1, top - (unit * 0.12f).toInt(), turret + 2, turret * 2, slate)
        }
        val windowWidth = (width - turret * 2) / 3
        CampusShapes.pointedArch(g, left + width / 2, top + height / 8, windowWidth, height * 3 / 4, pane)
        g.color = mortar
        g.fillRect(left + width / 2, top + height / 4, 1, height / 2)
        g.fillRect(left + width / 2 - windowWidth / 2, top + height / 2, windowWidth, 1)
        return width
    }

    private fun paintCloister(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * (1.1f + random.nextFloat() * 0.3f)).toInt()
        val height = (unit * 0.22f).toInt()
        val top = groundY - height
        val stone = stoneShades[random.nextInt(stoneShades.size)]
        CampusShapes.trapezoidRoof(g, left - 2, top, width + 4, (unit * 0.07f).toInt(), (unit * 0.04f).toInt(), slate)
        g.color = stone
        g.fillRect(left, top, width, height)
        val arch = (unit * 0.07f).toInt().coerceAtLeast(3)
        var x = left + arch
        while (x + arch / 2 < left + width) {
            CampusShapes.pointedArch(g, x, top + height / 5, arch, height * 4 / 5, pane)
            x += arch * 3 / 2
        }
        return width
    }

    private fun paintTower(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 0.3f).toInt().coerceAtLeast(5)
        val height = (unit * (0.72f + random.nextFloat() * 0.12f)).toInt()
        val top = groundY - height
        val stone = stoneShades[random.nextInt(stoneShades.size)]
        g.color = stone
        g.fillRect(left, top, width, height)
        battlements(g, left, top, width, unit)
        listOf(left + 1, left + width - 2, left + width / 2).forEach { pinnacle(g, it, top - (unit * 0.03f).toInt(), unit, stone) }
        val opening = width / 4
        CampusShapes.pointedArch(g, left + width / 3, top + (unit * 0.06f).toInt(), opening, opening * 3, pane)
        CampusShapes.pointedArch(g, left + width * 2 / 3, top + (unit * 0.06f).toInt(), opening, opening * 3, pane)
        CampusShapes.pointedArch(g, left + width / 2, top + height / 2, opening, opening * 4, pane)
        return width
    }

    private fun paintChapel(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 0.8f).toInt()
        val height = (unit * 0.42f).toInt()
        val top = groundY - height
        val stone = stoneShades[random.nextInt(stoneShades.size)]
        CampusShapes.gable(g, left, top, width, (unit * 0.3f).toInt(), slate)
        g.color = stone
        g.fillRect(left, top, width, height)
        val spireBase = (unit * 0.16f).toInt()
        val spireLeft = left + width / 2 - spireBase / 2
        g.fillRect(spireLeft, top - (unit * 0.32f).toInt(), spireBase, (unit * 0.32f).toInt())
        CampusShapes.gable(g, spireLeft - 1, top - (unit * 0.32f).toInt(), spireBase + 2, (unit * 0.28f).toInt(), slate)
        val rose = (unit * 0.1f).toInt().coerceAtLeast(2)
        g.color = pane
        g.fillOval(left + width / 2 - rose / 2, top + (unit * 0.04f).toInt(), rose, rose)
        val lancet = (unit * 0.05f).toInt().coerceAtLeast(2)
        listOf(left + width / 5, left + width * 4 / 5).forEach {
            CampusShapes.pointedArch(g, it, top + height / 4, lancet, (height * 0.6f).toInt(), pane)
        }
        return width
    }
}
