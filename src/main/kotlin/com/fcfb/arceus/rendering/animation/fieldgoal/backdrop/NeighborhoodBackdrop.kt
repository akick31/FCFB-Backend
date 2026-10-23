package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random

object NeighborhoodBackdrop {
    private const val SEED = 42L
    private const val TREELINE_HEIGHT = 0.42f
    private const val HOUSE_MIN_WIDTH = 0.42f
    private const val HOUSE_MAX_WIDTH = 0.62f
    private const val GAP_MIN = 0.12f
    private const val GAP_MAX = 0.3f
    private const val FENCE_HEIGHT = 0.13f
    private const val FENCE_BOARD = 5

    private val sidings =
        listOf(Color(244, 242, 234), Color(236, 220, 168), Color(176, 198, 214), Color(170, 186, 162), Color(176, 176, 172))
    private val roofs = listOf(Color(74, 72, 72), Color(96, 72, 60), Color(62, 70, 84))
    private val window = Color(70, 84, 100)
    private val trim = Color(250, 250, 248)
    private val fence = Color(156, 122, 86)
    private val fenceBoard = Color(126, 96, 66)
    private val chimney = Color(140, 76, 58)

    val PALETTE: List<Color> = sidings + roofs + listOf(window, trim, chimney, fence, fenceBoard) + TreePainter.PALETTE

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
    ) {
        val random = Random(SEED)
        val unit = area.height.toFloat()
        val treeHeight = (unit * TREELINE_HEIGHT).toInt()
        var x = 0
        while (x < area.width) {
            TreePainter.deciduous(g, random, x, area.groundY, (treeHeight * (0.6f + random.nextFloat() * 0.5f)).toInt())
            x += (treeHeight / 3).coerceAtLeast(3)
        }
        x = -random.nextInt((unit * GAP_MAX).toInt().coerceAtLeast(1))
        while (x < area.width) {
            val width = (unit * (HOUSE_MIN_WIDTH + random.nextFloat() * (HOUSE_MAX_WIDTH - HOUSE_MIN_WIDTH))).toInt()
            paintHouse(g, random, x, area.groundY, width, unit)
            val gap = (unit * (GAP_MIN + random.nextFloat() * (GAP_MAX - GAP_MIN))).toInt()
            if (gap > unit * 0.2f) TreePainter.deciduous(g, random, x + width + gap / 2, area.groundY, (unit * 0.4f).toInt())
            x += width + gap
        }
        if (!area.behindWall) paintFence(g, area, unit)
    }

    private fun paintFence(
        g: Graphics2D,
        area: BackdropArea,
        unit: Float,
    ) {
        val height = (unit * FENCE_HEIGHT).toInt().coerceAtLeast(3)
        val top = area.groundY - height
        g.color = fence
        g.fillRect(0, top, area.width, height)
        g.color = fenceBoard
        val board = area.scaled(FENCE_BOARD)
        var x = 0
        while (x < area.width) {
            g.fillRect(x, top, 1, height)
            x += board
        }
        g.fillRect(0, top + height / 3, area.width, 1)
    }

    private fun paintHouse(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        width: Int,
        unit: Float,
    ) {
        val twoStory = random.nextBoolean()
        val bodyHeight = (unit * if (twoStory) 0.3f else 0.18f).toInt()
        val roofHeight = (width * 0.32f).toInt()
        val top = groundY - bodyHeight
        g.color = chimney
        g.fillRect(left + width * 2 / 3, top - roofHeight * 3 / 4, (width / 10).coerceAtLeast(2), roofHeight * 3 / 4)
        g.color = roofs[random.nextInt(roofs.size)]
        val roof = Path2D.Float()
        roof.moveTo((left - width / 14).toDouble(), top.toDouble())
        roof.lineTo((left + width / 2).toDouble(), (top - roofHeight).toDouble())
        roof.lineTo((left + width + width / 14).toDouble(), top.toDouble())
        roof.closePath()
        g.fill(roof)
        g.color = sidings[random.nextInt(sidings.size)]
        g.fillRect(left, top, width, bodyHeight)
        val windowSize = (unit * 0.055f).toInt().coerceAtLeast(2)
        val floors = if (twoStory) 2 else 1
        repeat(floors) { floor ->
            val y = top + bodyHeight * (floor * 2 + 1) / (floors * 2) - windowSize / 2
            listOf(left + width / 5, left + width * 4 / 5 - windowSize).forEach { x ->
                g.color = trim
                g.fillRect(x - 1, y - 1, windowSize + 2, windowSize + 2)
                g.color = window
                g.fillRect(x, y, windowSize, windowSize)
            }
        }
    }
}
