package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object HayFarm : FarmStyle {
    private const val HORIZON_HEIGHT = 0.58f
    private const val BALE_ROWS = 3
    private const val BALE_SIZE = 0.13f
    private const val STUBBLE_LINES = 9
    private const val BARN_X_SHARE = 0.08f
    private const val WINDMILL_X_SHARE = 0.84f

    private val fieldGold = Color(222, 190, 112)
    private val fieldFar = Color(206, 176, 104)
    private val stubble = Color(196, 160, 90)
    private val baleFace = Color(200, 158, 78)
    private val baleSide = Color(168, 128, 60)
    private val baleSpiral = Color(146, 108, 50)

    override val palette: List<Color> = listOf(fieldGold, fieldFar, stubble, baleFace, baleSide, baleSpiral) + FarmScenery.PALETTE

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        val horizonY = area.yAt(HORIZON_HEIGHT)
        FarmScenery.treeline(g, random, area, horizonY)
        FarmScenery.barn(g, area, (area.width * BARN_X_SHARE).toInt(), horizonY + 2, withSilo = false)
        val depth = area.groundY - horizonY
        g.color = fieldFar
        g.fillRect(0, horizonY, area.width, depth)
        g.color = fieldGold
        g.fillRect(0, horizonY + depth / 3, area.width, depth - depth / 3)
        g.color = stubble
        repeat(STUBBLE_LINES) { line ->
            val y = horizonY + depth * (line + 1) / (STUBBLE_LINES + 1)
            g.fillRect(0, y, area.width, 1)
        }
        FarmScenery.windmill(g, area, (area.width * WINDMILL_X_SHARE).toInt(), horizonY + depth / 4)
        repeat(BALE_ROWS) { row ->
            val nearness = (row + 1f) / BALE_ROWS
            val y = horizonY + (depth * (0.2f + 0.72f * nearness)).toInt()
            val size = (area.height * BALE_SIZE * (0.4f + 0.8f * nearness)).toInt().coerceAtLeast(3)
            var x = random.nextInt(size * 3) - size
            while (x < area.width) {
                paintBale(g, x, y, size)
                x += size * (8 + random.nextInt(6))
            }
        }
    }

    private fun paintBale(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        size: Int,
    ) {
        val depth = size / 3
        g.color = baleSide
        g.fillRect(left + size / 2, groundY - size, depth + size / 2, size)
        g.fillOval(left + depth, groundY - size, size, size)
        g.color = baleFace
        g.fillOval(left, groundY - size, size, size)
        g.color = baleSpiral
        g.drawOval(left + size / 4, groundY - size * 3 / 4, size / 2, size / 2)
        g.drawOval(left + size * 3 / 8, groundY - size * 5 / 8, size / 4, size / 4)
    }
}
