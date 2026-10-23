package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

object FarmScenery {
    private const val TREELINE_HEIGHT = 0.16f
    private const val TREELINE_SEGMENTS = 5
    private const val TREELINE_SEGMENT_SHARE = 0.12f
    private const val TREELINE_SPACING_SHARE = 0.45f
    private const val BARN_WIDTH = 64
    private const val BARN_HEIGHT_SHARE = 0.45f
    private const val FENCE_POST_SPACING = 30
    private const val WINDMILL_HEIGHT = 0.5f
    private const val WINDMILL_BLADES = 12

    private val barnRed = Color(158, 40, 34)
    private val barnRoof = Color(62, 46, 42)
    private val trim = Color(238, 234, 226)
    private val silo = Color(170, 174, 178)
    private val siloDome = Color(132, 138, 144)
    private val houseWall = Color(236, 232, 220)
    private val houseRoof = Color(84, 76, 72)
    private val houseWindow = Color(70, 80, 94)
    private val steel = Color(104, 108, 114)
    private val treeline = Color(48, 86, 46)

    val PALETTE: List<Color> =
        listOf(barnRed, barnRoof, trim, silo, siloDome, houseWall, houseRoof, houseWindow, steel, treeline) + TreePainter.PALETTE

    fun treeline(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
    ) {
        val treeHeight = (area.height * TREELINE_HEIGHT).toInt().coerceAtLeast(3)
        val spacing = (treeHeight * TREELINE_SPACING_SHARE).toInt().coerceAtLeast(2)
        val segmentWidth = (area.width * TREELINE_SEGMENT_SHARE).toInt()
        val slotWidth = area.width / TREELINE_SEGMENTS
        repeat(TREELINE_SEGMENTS) { segment ->
            val start = slotWidth * segment + random.nextInt((slotWidth - segmentWidth).coerceAtLeast(1))
            var x = start
            while (x < start + segmentWidth) {
                val height = (treeHeight * (0.7f + random.nextFloat() * 0.3f)).toInt()
                TreePainter.deciduous(g, random, x, horizonY + 1, height, treeline)
                x += spacing
            }
        }
    }

    fun barnWidth(area: BackdropArea): Int = minOf(area.scaled(BARN_WIDTH), (area.height * BARN_HEIGHT_SHARE).toInt()).coerceAtLeast(6)

    fun barn(
        g: Graphics2D,
        area: BackdropArea,
        left: Int,
        groundY: Int,
        withSilo: Boolean,
    ) {
        val width = barnWidth(area)
        val height = (width * 0.9f).toInt()
        val bodyHeight = (height * 0.62f).toInt()
        val bodyTop = groundY - bodyHeight
        val roofTop = groundY - height
        if (withSilo) {
            val siloWidth = (width * 0.3f).toInt().coerceAtLeast(3)
            val siloHeight = (height * 1.62f).toInt()
            val siloLeft = left + width + siloWidth / 3
            g.color = silo
            g.fillRect(siloLeft, groundY - siloHeight, siloWidth, siloHeight)
            g.color = siloDome
            g.fillArc(siloLeft, groundY - siloHeight - siloWidth / 2, siloWidth, siloWidth, 0, 180)
        }
        g.color = barnRed
        g.fillRect(left, bodyTop, width, bodyHeight)
        val shoulder = width / 6
        val shoulderY = bodyTop - (bodyTop - roofTop) * 0.6
        val roof = Path2D.Float()
        roof.moveTo(left.toDouble(), bodyTop.toDouble())
        roof.lineTo((left + shoulder).toDouble(), shoulderY)
        roof.lineTo((left + width / 2).toDouble(), roofTop.toDouble())
        roof.lineTo((left + width - shoulder).toDouble(), shoulderY)
        roof.lineTo((left + width).toDouble(), bodyTop.toDouble())
        roof.closePath()
        g.color = barnRoof
        g.fill(roof)
        val doorWidth = width / 3
        val doorHeight = bodyHeight * 2 / 3
        val doorLeft = left + (width - doorWidth) / 2
        val doorTop = groundY - doorHeight
        g.color = trim
        g.drawRect(doorLeft, doorTop, doorWidth, doorHeight)
        g.drawLine(doorLeft, doorTop, doorLeft + doorWidth, groundY)
        g.drawLine(doorLeft + doorWidth, doorTop, doorLeft, groundY)
    }

    fun farmhouse(
        g: Graphics2D,
        area: BackdropArea,
        left: Int,
        groundY: Int,
    ) {
        val width = (barnWidth(area) * 0.85f).toInt()
        val bodyHeight = (width * 0.5f).toInt()
        val roofHeight = (width * 0.35f).toInt()
        g.color = houseRoof
        val roof = Path2D.Float()
        roof.moveTo((left - width / 12).toDouble(), (groundY - bodyHeight).toDouble())
        roof.lineTo((left + width / 2).toDouble(), (groundY - bodyHeight - roofHeight).toDouble())
        roof.lineTo((left + width + width / 12).toDouble(), (groundY - bodyHeight).toDouble())
        roof.closePath()
        g.fill(roof)
        g.color = houseWall
        g.fillRect(left, groundY - bodyHeight, width, bodyHeight)
        g.color = houseWindow
        val window = (width / 8).coerceAtLeast(1)
        listOf(left + width / 5, left + width * 3 / 5).forEach { g.fillRect(it, groundY - bodyHeight * 3 / 4, window, window) }
        g.fillRect(left + width * 2 / 5, groundY - bodyHeight / 2, window, bodyHeight / 2)
    }

    fun windmill(
        g: Graphics2D,
        area: BackdropArea,
        centerX: Int,
        groundY: Int,
    ) {
        val height = (area.height * WINDMILL_HEIGHT).toInt()
        val hubY = groundY - height
        val baseHalf = height / 6
        val previousStroke = g.stroke
        g.stroke = BasicStroke(area.scaled(2).toFloat())
        g.color = steel
        g.drawLine(centerX - baseHalf, groundY, centerX - 1, hubY)
        g.drawLine(centerX + baseHalf, groundY, centerX + 1, hubY)
        listOf(0.3f, 0.55f, 0.8f).forEach { level ->
            val y = groundY - (height * level).toInt()
            val half = (baseHalf * (1f - level)).toInt()
            g.drawLine(centerX - half, y, centerX + half, y)
        }
        g.stroke = BasicStroke(1f)
        val radius = height / 4
        repeat(WINDMILL_BLADES) { blade ->
            val angle = blade * 2 * Math.PI / WINDMILL_BLADES
            g.drawLine(centerX, hubY, centerX + (cos(angle) * radius).toInt(), hubY + (sin(angle) * radius).toInt())
        }
        g.drawOval(centerX - radius, hubY - radius, radius * 2, radius * 2)
        g.fillRect(centerX, hubY - 2, radius, 3)
        g.stroke = previousStroke
    }

    fun fence(
        g: Graphics2D,
        area: BackdropArea,
        groundY: Int,
        height: Int,
        color: Color,
    ) {
        val postWidth = area.scaled(3)
        g.color = color
        g.fillRect(0, groundY - height, area.width, area.scaled(2))
        g.fillRect(0, groundY - height / 2, area.width, area.scaled(2))
        var x = 0
        while (x < area.width) {
            g.fillRect(x, groundY - height - 1, postWidth, height + 2)
            x += area.scaled(FENCE_POST_SPACING)
        }
    }
}
