package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.WING_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.WING_HEIGHT
import java.awt.Color
import java.awt.Graphics2D

object ModernTowerFacility : FacilityStyle {
    private const val LOBBY = 30
    private const val COLUMN_PITCH = 52
    private const val COLUMN_HALF = 3
    private val WING_RIBBONS = listOf(40 to 58, 66 to 84)
    private const val MULLION_PITCH = 16

    private val concrete = Color(196, 194, 188)
    private val concreteShadow = Color(158, 156, 150)
    private val lobby = Color(30, 34, 40)

    override val palette: List<Color> = listOf(concrete, concreteShadow, lobby) + Masonry.PALETTE
    override val fillsView: Boolean = false

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
        fullWidth: Boolean,
    ) {
        val frame = FacilityFrame(area, footY)
        paintWings(g, frame)
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount = TowerMassing.boardOver(area, footY, WING_HEIGHT)

    private fun paintWings(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val left = frame.x(-WING_HALF)
        val right = frame.x(WING_HALF)
        val top = frame.y(WING_HEIGHT)
        Masonry.band(g, concrete, left, right, top, frame.footY - top)
        WING_RIBBONS.forEach { (bottom, ribbonTop) ->
            Masonry.ribbon(
                g,
                left + frame.length(8),
                right - frame.length(8),
                frame.y(ribbonTop),
                frame.length(ribbonTop - bottom),
                frame.length(MULLION_PITCH),
            )
        }
        Masonry.band(g, concreteShadow, left, right, top, frame.length(3))
        val lobbyTop = frame.y(LOBBY)
        Masonry.band(g, lobby, left, right, lobbyTop, frame.footY - lobbyTop)
        var offset = COLUMN_PITCH / 2
        while (offset < WING_HALF) {
            listOf(offset, -offset).forEach { x ->
                Masonry.band(g, concrete, frame.x(x - COLUMN_HALF), frame.x(x + COLUMN_HALF), lobbyTop, frame.footY - lobbyTop)
            }
            offset += COLUMN_PITCH
        }
    }
}
