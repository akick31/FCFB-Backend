package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object GeorgianCampus : CampusStyle {
    private enum class Kind { HALL, LIBRARY, TOWER, CHAPEL }

    private val brickShades = listOf(Color(150, 70, 52), Color(138, 64, 48), Color(160, 84, 60))
    private val stone = Color(204, 194, 174)
    private val slate = Color(74, 82, 90)
    private val copper = Color(98, 150, 130)
    private val trim = Color(238, 230, 212)
    private val pane = Color(46, 54, 66)

    override val kindCount = Kind.entries.size

    override val palette: List<Color> = brickShades + listOf(stone, slate, copper, trim, pane)

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
            Kind.LIBRARY -> paintLibrary(g, left, groundY, unit)
            Kind.TOWER -> paintTower(g, random, left, groundY, unit)
            Kind.CHAPEL -> paintChapel(g, random, left, groundY, unit)
        }

    private fun window(
        g: Graphics2D,
        unit: Float,
    ): (Int, Int) -> Unit =
        { x, y ->
            CampusShapes.framedWindow(
                g,
                x,
                y,
                (unit * 0.035f).toInt().coerceAtLeast(1),
                (unit * 0.07f).toInt().coerceAtLeast(2),
                trim,
                pane,
            )
        }

    private fun paintHall(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * (1.4f + random.nextFloat() * 0.6f)).toInt()
        val bodyHeight = (unit * (0.38f + random.nextFloat() * 0.12f)).toInt()
        val bodyTop = groundY - bodyHeight
        g.color = brickShades[random.nextInt(brickShades.size)]
        g.fillRect(left, bodyTop, width, bodyHeight)
        CampusShapes.trapezoidRoof(g, left, bodyTop, width, (unit * 0.1f).toInt(), (unit * 0.05f).toInt(), slate)
        g.color = trim
        g.fillRect(left, bodyTop, width, (unit * 0.025f).toInt().coerceAtLeast(1))
        CampusShapes.windowGrid(
            g,
            left,
            bodyTop + (unit * 0.025f).toInt(),
            width,
            bodyHeight,
            (unit * 0.08f).toInt().coerceAtLeast(3),
            (unit * 0.13f).toInt().coerceAtLeast(3),
            window(g, unit),
        )
        paintPortico(g, left + width / 2, groundY, (width * 0.3f).toInt(), bodyHeight, unit)
        return width
    }

    private fun paintPortico(
        g: Graphics2D,
        centerX: Int,
        groundY: Int,
        width: Int,
        bodyHeight: Int,
        unit: Float,
    ) {
        val height = (bodyHeight * 0.85f).toInt()
        val top = groundY - height
        val left = centerX - width / 2
        g.color = pane
        g.fillRect(left, top, width, height)
        CampusShapes.columns(g, left, top, width, height, 4, (unit * 0.03f).toInt().coerceAtLeast(1), trim)
        g.fillRect(left, top, width, (unit * 0.025f).toInt().coerceAtLeast(1))
        CampusShapes.gable(g, left - width / 12, top, width + width / 6, (width * 0.22f).toInt(), trim)
    }

    private fun paintLibrary(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 1.15f).toInt()
        val bodyHeight = (unit * 0.42f).toInt()
        val bodyTop = groundY - bodyHeight
        val centerX = left + width / 2
        val drumWidth = (width * 0.42f).toInt()
        val drumHeight = (unit * 0.1f).toInt()
        val drumTop = bodyTop - drumHeight
        g.color = copper
        g.fillArc(centerX - drumWidth / 2, drumTop - drumWidth / 2, drumWidth, drumWidth, 0, 180)
        val lantern = (drumWidth * 0.14f).toInt().coerceAtLeast(1)
        g.fillRect(centerX - lantern / 2, drumTop - drumWidth / 2 - lantern * 2, lantern, lantern * 2)
        g.color = stone
        g.fillRect(centerX - drumWidth / 2, drumTop, drumWidth, drumHeight)
        g.fillRect(left, bodyTop, width, bodyHeight)
        g.color = trim
        g.fillRect(left, bodyTop, width, (unit * 0.025f).toInt().coerceAtLeast(1))
        val recessTop = bodyTop + (unit * 0.06f).toInt()
        g.color = pane
        g.fillRect(left + width / 8, recessTop, width * 3 / 4, groundY - recessTop)
        CampusShapes.columns(
            g,
            left + width / 8,
            recessTop,
            width * 3 / 4,
            groundY - recessTop,
            6,
            (unit * 0.03f).toInt().coerceAtLeast(1),
            trim,
        )
        return width
    }

    private fun paintTower(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 0.24f).toInt().coerceAtLeast(4)
        val shaftHeight = (unit * (0.62f + random.nextFloat() * 0.12f)).toInt()
        val shaftTop = groundY - shaftHeight
        g.color = brickShades[random.nextInt(brickShades.size)]
        g.fillRect(left, shaftTop, width, shaftHeight)
        CampusShapes.gable(g, left - width / 10, shaftTop, width + width / 5, (unit * 0.22f).toInt(), slate)
        val belfry = width / 2
        val belfryTop = shaftTop + (unit * 0.05f).toInt()
        CampusShapes.roundArch(g, left + width / 2, belfryTop, belfry, belfry * 3 / 2, pane)
        val clockSize = (width * 0.62f).toInt()
        val clockTop = belfryTop + belfry * 2
        g.color = trim
        g.fillOval(left + (width - clockSize) / 2, clockTop, clockSize, clockSize)
        g.color = pane
        val clockCenterY = clockTop + clockSize / 2
        g.drawLine(left + width / 2, clockCenterY, left + width / 2, clockCenterY - clockSize * 2 / 5)
        g.drawLine(left + width / 2, clockCenterY, left + width / 2 + clockSize / 4, clockCenterY)
        return width
    }

    private fun paintChapel(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 0.7f).toInt()
        val bodyHeight = (unit * 0.44f).toInt()
        val bodyTop = groundY - bodyHeight
        val gableHeight = (unit * 0.26f).toInt()
        val wall = if (random.nextBoolean()) stone else brickShades[random.nextInt(brickShades.size)]
        g.color = wall
        g.fillRect(left, bodyTop, width, bodyHeight)
        CampusShapes.gable(
            g,
            left - (unit * 0.03f).toInt(),
            bodyTop,
            width + (unit * 0.06f).toInt(),
            gableHeight + (unit * 0.03f).toInt(),
            slate,
        )
        CampusShapes.gable(g, left, bodyTop, width, gableHeight, wall)
        val rose = (gableHeight * 0.5f).toInt().coerceAtLeast(2)
        g.color = trim
        g.fillOval(left + (width - rose) / 2, bodyTop - gableHeight / 2 - rose / 3, rose, rose)
        val lancet = (unit * 0.05f).toInt().coerceAtLeast(1)
        repeat(3) { index ->
            CampusShapes.pointedArch(
                g,
                left + width * (index + 1) / 4,
                bodyTop + bodyHeight / 5,
                lancet,
                (bodyHeight * 0.55f).toInt(),
                pane,
            )
        }
        return width
    }
}
