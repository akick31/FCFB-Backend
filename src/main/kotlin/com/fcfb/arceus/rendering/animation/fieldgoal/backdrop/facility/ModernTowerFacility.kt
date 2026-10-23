package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.TOWER_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.TOWER_HEIGHT
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
    private const val SLOT_HALF = 9
    private const val SLOT_BOTTOM = 34
    private const val CROWN_BOTTOM = 140
    private const val CROWN_OVERHANG = 22
    private const val CROWN_GLASS_BOTTOM = 148
    private const val CROWN_GLASS_TOP = 168
    private const val FIN_PITCH = 40
    private const val FIN_HALF = 3

    private val concrete = Color(196, 194, 188)
    private val concreteShadow = Color(158, 156, 150)
    private val lobby = Color(30, 34, 40)

    override val palette: List<Color> = listOf(concrete, concreteShadow, lobby) + Masonry.PALETTE
    override val fillsView: Boolean = false

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
    ) {
        val frame = FacilityFrame(area, footY)
        paintWings(g, frame)
        paintTower(g, frame)
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount = TowerMassing.boardOver(area, footY, TOWER_HEIGHT)

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

    private fun paintTower(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val left = frame.x(-TOWER_HALF)
        val right = frame.x(TOWER_HALF)
        val crown = frame.y(CROWN_BOTTOM)
        Masonry.band(g, concrete, left, right, crown, frame.footY - crown)
        Masonry.band(g, concreteShadow, right - frame.length(6), right, crown, frame.footY - crown)
        Masonry.ribbon(g, frame.x(-SLOT_HALF), frame.x(SLOT_HALF), crown, frame.y(SLOT_BOTTOM) - crown, frame.length(MULLION_PITCH))
        val crownLeft = frame.x(-TOWER_HALF - CROWN_OVERHANG)
        val crownRight = frame.x(TOWER_HALF + CROWN_OVERHANG)
        val top = frame.y(TOWER_HEIGHT)
        Masonry.band(g, concrete, crownLeft, crownRight, top, crown - top)
        Masonry.ribbon(
            g,
            crownLeft,
            crownRight,
            frame.y(CROWN_GLASS_TOP),
            frame.length(CROWN_GLASS_TOP - CROWN_GLASS_BOTTOM),
            frame.length(MULLION_PITCH),
        )
        var x = -TOWER_HALF - CROWN_OVERHANG + FIN_PITCH / 2
        while (x < TOWER_HALF + CROWN_OVERHANG) {
            Masonry.band(g, concreteShadow, frame.x(x - FIN_HALF), frame.x(x + FIN_HALF), top, crown - top)
            x += FIN_PITCH
        }
        Masonry.band(g, concreteShadow, crownLeft, crownRight, crown, frame.length(3))
    }
}
