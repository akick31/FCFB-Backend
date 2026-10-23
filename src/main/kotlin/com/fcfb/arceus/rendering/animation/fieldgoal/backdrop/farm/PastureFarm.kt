package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object PastureFarm : FarmStyle {
    private const val HORIZON_HEIGHT = 0.55f
    private const val COWS = 9
    private const val COW_WIDTH = 0.12f
    private const val FENCE_HEIGHT = 0.12f
    private const val BARN_X_SHARE = 0.72f
    private const val HOUSE_X_SHARE = 0.86f

    private val pastureShades = listOf(Color(132, 176, 92), Color(116, 164, 80), Color(100, 150, 70))
    private val holsteinWhite = Color(242, 240, 234)
    private val holsteinBlack = Color(34, 32, 32)
    private val angusBrown = Color(110, 70, 46)
    private val hoof = Color(60, 52, 48)
    private val whiteFence = Color(236, 234, 228)

    override val palette: List<Color> =
        pastureShades + listOf(holsteinWhite, holsteinBlack, angusBrown, hoof, whiteFence) + FarmScenery.PALETTE

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        val horizonY = area.yAt(HORIZON_HEIGHT)
        FarmScenery.treeline(g, random, area, horizonY)
        FarmScenery.barn(g, area, (area.width * BARN_X_SHARE).toInt(), horizonY + 2, withSilo = true)
        FarmScenery.farmhouse(g, area, (area.width * HOUSE_X_SHARE).toInt(), horizonY + 2)
        val depth = area.groundY - horizonY
        pastureShades.forEachIndexed { index, shade ->
            val top = horizonY + depth * index / pastureShades.size
            g.color = shade
            g.fillRect(0, top, area.width, area.groundY - top)
        }
        repeat(COWS) { index ->
            val nearness = (index + 1f) / COWS
            val y = horizonY + (depth * (0.15f + 0.65f * nearness)).toInt()
            val x = (area.width * ((index * 0.37f + random.nextFloat() * 0.2f) % 1f)).toInt()
            val width = (area.height * COW_WIDTH * (0.45f + 0.75f * nearness)).toInt().coerceAtLeast(4)
            paintCow(g, x, y, width, random.nextBoolean(), random.nextBoolean())
        }
        if (!area.behindWall) FarmScenery.fence(g, area, area.groundY, (area.height * FENCE_HEIGHT).toInt().coerceAtLeast(3), whiteFence)
    }

    private fun paintCow(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        width: Int,
        holstein: Boolean,
        facingLeft: Boolean,
    ) {
        val bodyHeight = width / 2
        val legHeight = width / 4
        val bodyTop = groundY - legHeight - bodyHeight
        val coat = if (holstein) holsteinWhite else angusBrown
        g.color = hoof
        val leg = (width / 10).coerceAtLeast(1)
        listOf(left + width / 8, left + width / 3, left + width * 3 / 5, left + width * 4 / 5).forEach {
            g.fillRect(it, groundY - legHeight, leg, legHeight)
        }
        g.color = coat
        g.fillRoundRect(left, bodyTop, width, bodyHeight, bodyHeight / 2, bodyHeight / 2)
        val headSize = bodyHeight * 3 / 4
        val headX = if (facingLeft) left - headSize / 2 else left + width - headSize / 2
        g.fillOval(headX, bodyTop - headSize / 4, headSize, headSize)
        if (holstein) {
            g.color = holsteinBlack
            g.fillOval(left + width / 5, bodyTop + bodyHeight / 6, width / 4, bodyHeight / 2)
            g.fillOval(left + width / 2, bodyTop + bodyHeight / 3, width / 5, bodyHeight / 2)
        }
    }
}
