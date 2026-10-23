package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.lake

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.util.Random

object OpenHorizon : LakeShore {
    private const val SHIP_X_SHARE = 0.72f
    private const val SHIP_DRIFT = 0.015f
    private const val SHIP_LENGTH = 0.22f

    private val ship = Color(84, 94, 108)

    override val horizonShare = 0.3f
    override val reflection: Color? = null
    override val palette: List<Color> = listOf(ship)

    override fun paintFarShore(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        horizonY: Int,
    ) {
        val length = (area.height * SHIP_LENGTH).toInt().coerceAtLeast(6)
        val left = (area.width * (SHIP_X_SHARE + SHIP_DRIFT * area.time)).toInt()
        val hullHeight = (length / 7).coerceAtLeast(2)
        g.color = ship
        g.fillRect(left, horizonY - hullHeight, length, hullHeight)
        g.fillRect(left + length * 2 / 3, horizonY - hullHeight * 3, length / 5, hullHeight * 2)
        g.fillRect(left + length / 5, horizonY - hullHeight * 2, length / 3, hullHeight)
    }
}
