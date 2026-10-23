package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object ForestBackdrop {
    private const val SEED = 42L
    private const val BACK_ROW_MIN_HEIGHT = 0.78f
    private const val BACK_ROW_MAX_HEIGHT = 1.0f
    private const val FRONT_ROW_MIN_HEIGHT = 0.55f
    private const val FRONT_ROW_MAX_HEIGHT = 0.82f
    private const val BACK_ROW_SPACING = 0.16f
    private const val FRONT_ROW_SPACING = 0.22f
    private const val PINE_CHANCE = 0.5f
    private const val UNDERSTORY_HEIGHT = 0.3f
    private const val MIN_SPACING = 6

    private val understoryColor = Color(22, 46, 30)
    private val backRowShade = Color(26, 56, 36)

    val PALETTE: List<Color> = listOf(understoryColor, backRowShade) + TreePainter.PALETTE

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
    ) {
        val random = Random(SEED)
        g.color = understoryColor
        val understoryTop = area.yAt(UNDERSTORY_HEIGHT)
        g.fillRect(0, understoryTop, area.width, area.groundY - understoryTop)
        paintRow(g, random, area, BACK_ROW_MIN_HEIGHT, BACK_ROW_MAX_HEIGHT, BACK_ROW_SPACING, backRowShade)
        paintRow(g, random, area, FRONT_ROW_MIN_HEIGHT, FRONT_ROW_MAX_HEIGHT, FRONT_ROW_SPACING, null)
    }

    private fun paintRow(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        minHeight: Float,
        maxHeight: Float,
        spacingShare: Float,
        uniformShade: Color?,
    ) {
        val spacing = (area.height * spacingShare).toInt().coerceAtLeast(MIN_SPACING)
        var x = random.nextInt(spacing)
        while (x < area.width + spacing) {
            val height = (area.height * (minHeight + random.nextFloat() * (maxHeight - minHeight))).toInt()
            if (random.nextFloat() < PINE_CHANCE) {
                TreePainter.pine(g, random, x, area.groundY, height, uniformShade)
            } else {
                TreePainter.deciduous(g, random, x, area.groundY, height, uniformShade)
            }
            x += spacing / 2 + random.nextInt(spacing)
        }
    }
}
