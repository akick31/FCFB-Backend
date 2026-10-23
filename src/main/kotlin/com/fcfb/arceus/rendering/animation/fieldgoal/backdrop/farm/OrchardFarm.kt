package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random
import kotlin.math.pow

object OrchardFarm : FarmStyle {
    private const val HORIZON_HEIGHT = 0.62f
    private const val ROWS = 6
    private const val TREE_HEIGHT = 0.32f
    private const val TREE_SPACING = 1.1f
    private const val PERSPECTIVE_EXPONENT = 1.6
    private const val FRUIT_PER_TREE = 6
    private const val BARN_X_SHARE = 0.4f

    private val grass = Color(122, 164, 86)
    private val grassFar = Color(138, 176, 100)
    private val alley = Color(104, 146, 72)
    private val canopy = Color(62, 116, 52)
    private val canopyDark = Color(48, 96, 42)
    private val trunk = Color(92, 70, 52)
    private val fruit = Color(208, 44, 40)

    override val palette: List<Color> = listOf(grass, grassFar, alley, canopy, canopyDark, trunk, fruit) + FarmScenery.PALETTE

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        val horizonY = area.yAt(HORIZON_HEIGHT)
        FarmScenery.treeline(g, random, area, horizonY)
        FarmScenery.barn(g, area, (area.width * BARN_X_SHARE).toInt(), horizonY + 2, withSilo = true)
        val depth = area.groundY - horizonY
        g.color = grassFar
        g.fillRect(0, horizonY, area.width, depth)
        g.color = grass
        g.fillRect(0, horizonY + depth / 3, area.width, depth - depth / 3)
        repeat(ROWS) { row ->
            val nearness = ((row + 1).toDouble() / ROWS).pow(PERSPECTIVE_EXPONENT).toFloat()
            val groundY = horizonY + (depth * nearness).toInt()
            val height = (area.height * TREE_HEIGHT * (0.25f + 0.75f * nearness)).toInt().coerceAtLeast(3)
            g.color = alley
            g.fillRect(0, groundY - height / 12, area.width, (height / 8).coerceAtLeast(1))
            var x = (random.nextInt(height.coerceAtLeast(1)) - height / 2)
            while (x < area.width + height) {
                paintFruitTree(g, random, x, groundY, height)
                x += (height * TREE_SPACING).toInt().coerceAtLeast(3)
            }
        }
    }

    private fun paintFruitTree(
        g: Graphics2D,
        random: Random,
        centerX: Int,
        groundY: Int,
        height: Int,
    ) {
        val trunkHeight = height / 3
        val trunkWidth = (height / 10).coerceAtLeast(1)
        g.color = trunk
        g.fillRect(centerX - trunkWidth / 2, groundY - trunkHeight, trunkWidth, trunkHeight)
        val crown = height - trunkHeight / 2
        g.color = canopyDark
        g.fillOval(centerX - crown / 2, groundY - height, crown, crown * 4 / 5)
        g.color = canopy
        g.fillOval(centerX - crown / 2 + crown / 8, groundY - height, crown * 3 / 4, crown * 3 / 5)
        val dot = (height / 14).coerceAtLeast(1)
        g.color = fruit
        repeat(FRUIT_PER_TREE) {
            val fx = centerX - crown / 3 + random.nextInt((crown * 2 / 3).coerceAtLeast(1))
            val fy = groundY - height + crown / 6 + random.nextInt((crown / 2).coerceAtLeast(1))
            g.fillRect(fx, fy, dot, dot)
        }
    }
}
