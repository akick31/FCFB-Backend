package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.DomeStyle
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.geom.Area

object DomePainter {
    private const val BEAM_SPACING = 44
    private const val BEAM_HEIGHT = 3
    private const val FABRIC_CABLE_SPACING = 64
    private const val FABRIC_CABLE_HEIGHT = 2
    private const val FASCIA_HEIGHT = 8
    private const val SUITE_LEVEL_HEIGHT = 34
    private const val SUITE_WINDOW_TOP = 8
    private const val SUITE_WINDOW_HEIGHT = 16
    private const val SUITE_SPACING = 46
    private const val SUITE_WIDTH = 34
    private const val LEDGE_HEIGHT = 4
    private const val FABRIC_BAND = 30
    private const val FABRIC_PANEL = 110
    private const val FABRIC_SEAM = 3
    private const val FABRIC_SHADOW = 14
    private const val COURSE_HEIGHT = 18
    private const val BLOCK_WIDTH = 64
    private const val WINDOW_SILL = 10
    private const val WINDOW_HEIGHT = 68
    private const val WINDOW_WIDTH = 124
    private const val WINDOW_SPACING = 156
    private const val WINDOW_MARGIN = 16
    private const val WALL_ABOVE_WINDOWS = 18

    private val ceiling = Color(48, 52, 60)
    private val beam = Color(38, 42, 49)
    private val fascia = Color(122, 124, 128)
    private val suiteWall = Color(138, 140, 142)
    private val suiteGlass = Color(38, 46, 58)
    private val ledge = Color(96, 98, 102)
    private val fabricShades = listOf(Color(214, 210, 198), Color(224, 220, 209), Color(233, 230, 220))
    private val fabricShadow = Color(204, 199, 186)
    private val fabricSeam = Color(188, 183, 170)
    private val roofUnderside = Color(70, 76, 84)
    private val wall = Color(156, 152, 142)
    private val wallCourse = Color(140, 136, 126)
    private val windowFrame = Color(48, 54, 62)

    fun paletteOf(style: DomeStyle): List<Color> =
        when (style) {
            DomeStyle.NONE -> emptyList()
            DomeStyle.SUITES -> listOf(ceiling, beam, fascia, suiteWall, suiteGlass, ledge)
            DomeStyle.FABRIC -> fabricShades + listOf(fabricShadow, fabricSeam, fascia)
            DomeStyle.WINDOWED -> listOf(roofUnderside, wall, wallCourse, windowFrame)
        }

    fun windows(
        area: BackdropArea,
        springY: Int,
    ): List<Rectangle> {
        val bottom = springY - area.scaled(WINDOW_SILL)
        val height = area.scaled(WINDOW_HEIGHT)
        val step = area.scaled(WINDOW_SPACING)
        return generateSequence(area.scaled(WINDOW_MARGIN)) { it + step }
            .takeWhile { it < area.width }
            .map { Rectangle(it, bottom - height, area.scaled(WINDOW_WIDTH), height) }
            .toList()
    }

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        style: DomeStyle,
        springY: Int,
    ) {
        if (style == DomeStyle.NONE || springY <= 0) return
        val g2 = g.create() as Graphics2D
        g2.clip(clipFor(area, style, springY))
        when (style) {
            DomeStyle.SUITES -> paintSuites(g2, area, springY)
            DomeStyle.FABRIC -> paintFabric(g2, area, springY)
            DomeStyle.WINDOWED -> paintWindowedWall(g2, area, springY)
            DomeStyle.NONE -> Unit
        }
        g2.dispose()
        if (style == DomeStyle.WINDOWED) paintWindowFrames(g, area, springY)
    }

    private fun clipFor(
        area: BackdropArea,
        style: DomeStyle,
        springY: Int,
    ): Area =
        Area(Rectangle(0, 0, area.width, springY)).apply {
            if (style == DomeStyle.WINDOWED) windows(area, springY).forEach { subtract(Area(it)) }
        }

    private fun paintSteppedBands(
        g: Graphics2D,
        area: BackdropArea,
        bottom: Int,
        bandHeight: Int,
        shades: List<Color>,
    ) {
        var band = 0
        var top = bottom
        while (top > 0) {
            g.color = shades[minOf(band, shades.lastIndex)]
            g.fillRect(0, top - area.scaled(bandHeight), area.width, area.scaled(bandHeight))
            top -= area.scaled(bandHeight)
            band++
        }
    }

    private fun paintSuites(
        g: Graphics2D,
        area: BackdropArea,
        springY: Int,
    ) {
        val levelTop = springY - area.scaled(FASCIA_HEIGHT + SUITE_LEVEL_HEIGHT)
        g.color = ceiling
        g.fillRect(0, 0, area.width, levelTop)
        paintLines(g, area, levelTop, BEAM_SPACING, BEAM_HEIGHT, beam)
        g.color = suiteWall
        g.fillRect(0, levelTop, area.width, area.scaled(SUITE_LEVEL_HEIGHT))
        g.color = suiteGlass
        var x = area.scaled(SUITE_SPACING - SUITE_WIDTH) / 2
        while (x < area.width) {
            g.fillRect(x, levelTop + area.scaled(SUITE_WINDOW_TOP), area.scaled(SUITE_WIDTH), area.scaled(SUITE_WINDOW_HEIGHT))
            x += area.scaled(SUITE_SPACING)
        }
        g.color = ledge
        g.fillRect(0, levelTop + area.scaled(SUITE_LEVEL_HEIGHT - LEDGE_HEIGHT), area.width, area.scaled(LEDGE_HEIGHT))
        g.color = fascia
        g.fillRect(0, springY - area.scaled(FASCIA_HEIGHT), area.width, area.scaled(FASCIA_HEIGHT))
    }

    private fun paintFabric(
        g: Graphics2D,
        area: BackdropArea,
        springY: Int,
    ) {
        val fabricBottom = springY - area.scaled(FASCIA_HEIGHT)
        paintSteppedBands(g, area, fabricBottom, FABRIC_BAND, fabricShades)
        var x = area.scaled(FABRIC_PANEL) / 2
        while (x < area.width) {
            g.color = fabricShadow
            g.fillRect(x, 0, area.scaled(FABRIC_SHADOW), fabricBottom)
            g.color = fabricSeam
            g.fillRect(x, 0, area.scaled(FABRIC_SEAM), fabricBottom)
            x += area.scaled(FABRIC_PANEL)
        }
        paintLines(g, area, fabricBottom, FABRIC_CABLE_SPACING, FABRIC_CABLE_HEIGHT, fabricSeam)
        g.color = fascia
        g.fillRect(0, fabricBottom, area.width, area.scaled(FASCIA_HEIGHT))
    }

    private fun paintLines(
        g: Graphics2D,
        area: BackdropArea,
        bottom: Int,
        spacing: Int,
        thickness: Int,
        color: Color,
    ) {
        g.color = color
        var y = bottom - area.scaled(spacing)
        while (y > 0) {
            g.fillRect(0, y, area.width, area.scaled(thickness))
            y -= area.scaled(spacing)
        }
    }

    private fun paintWindowedWall(
        g: Graphics2D,
        area: BackdropArea,
        springY: Int,
    ) {
        g.color = roofUnderside
        g.fillRect(0, 0, area.width, springY)
        val wallTop = springY - area.scaled(WINDOW_SILL + WINDOW_HEIGHT + WALL_ABOVE_WINDOWS)
        g.color = wall
        g.fillRect(0, wallTop, area.width, springY - wallTop)
        g.color = wallCourse
        val course = area.scaled(COURSE_HEIGHT)
        var row = 0
        var y = springY - course
        while (y + course > wallTop) {
            g.drawLine(0, y, area.width, y)
            val offset = if (row % 2 == 0) 0 else area.scaled(BLOCK_WIDTH) / 2
            var x = offset
            while (x < area.width) {
                g.drawLine(x, y, x, y + course)
                x += area.scaled(BLOCK_WIDTH)
            }
            y -= course
            row++
        }
        g.fillRect(0, wallTop, area.width, area.scaled(3))
    }

    private fun paintWindowFrames(
        g: Graphics2D,
        area: BackdropArea,
        springY: Int,
    ) {
        g.color = windowFrame
        g.stroke = BasicStroke((area.scale * 3).coerceAtLeast(1f))
        windows(area, springY).forEach { window ->
            g.drawRect(window.x, window.y, window.width, window.height)
            g.drawLine(window.centerX.toInt(), window.y, window.centerX.toInt(), window.y + window.height)
        }
    }
}
