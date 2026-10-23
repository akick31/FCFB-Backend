package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object PineShore : LakeShore {
    private const val TREE_MIN = 0.2f
    private const val TREE_MAX = 0.32f
    private const val CABIN_X_SHARE = 0.3f
    private const val CABIN_WIDTH = 0.2f

    private val backPines = Color(26, 58, 40)
    private val logs = Color(118, 82, 54)
    private val cabinRoof = Color(70, 56, 48)
    private val cabinWindow = Color(240, 214, 140)

    override val horizonShare = 0.36f
    override val reflection: Color = Color(30, 72, 66)
    override val palette: List<Color> = listOf(backPines, logs, cabinRoof, cabinWindow, reflection) + TreePainter.PALETTE

    override fun paintFarShore(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
    ) {
        paintPineRow(g, random, area, horizonY, backPines, 1.15f)
        paintCabin(g, area, horizonY)
        paintPineRow(g, random, area, horizonY, null, 0.9f)
    }

    private fun paintPineRow(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
        shade: Color?,
        heightFactor: Float,
    ) {
        var x = random.nextInt(8)
        while (x < area.width) {
            val height = (area.height * (TREE_MIN + random.nextFloat() * (TREE_MAX - TREE_MIN)) * heightFactor).toInt().coerceAtLeast(4)
            val skip = x > area.width * CABIN_X_SHARE - area.height * 0.02f && x < area.width * CABIN_X_SHARE + area.height * CABIN_WIDTH
            if (shade != null || !skip) TreePainter.pine(g, random, x, horizonY + 1, height, shade)
            x += (height / 4).coerceAtLeast(2)
        }
    }

    private fun paintCabin(
        g: Graphics2D,
        area: BackdropArea,
        horizonY: Int,
    ) {
        val width = (area.height * CABIN_WIDTH).toInt().coerceAtLeast(6)
        val left = (area.width * CABIN_X_SHARE).toInt()
        val wallHeight = width / 2
        g.color = logs
        g.fillRect(left, horizonY - wallHeight, width, wallHeight)
        g.color = cabinRoof
        g.fillPolygon(
            intArrayOf(left - width / 10, left + width / 2, left + width + width / 10),
            intArrayOf(horizonY - wallHeight, horizonY - wallHeight - width / 3, horizonY - wallHeight),
            3,
        )
        g.color = cabinWindow
        val window = (width / 7).coerceAtLeast(1)
        g.fillRect(left + width / 5, horizonY - wallHeight * 2 / 3, window, window)
        g.fillRect(left + width * 3 / 5, horizonY - wallHeight * 2 / 3, window, window)
    }
}
