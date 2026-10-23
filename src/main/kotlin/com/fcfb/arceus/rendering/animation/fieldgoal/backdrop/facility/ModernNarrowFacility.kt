package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D

object ModernNarrowFacility : FacilityStyle {
    private const val HALF_WIDTH = 270
    private const val HEIGHT = 140
    private const val LOBBY = 34
    private const val COLUMN_PITCH = 60
    private const val COLUMN_HALF = 3
    private val BANDS = listOf(52 to 76, 96 to 120)
    private const val MULLION_PITCH = 22
    private const val CANOPY = 8
    private const val OVERHANG = 22
    private const val BOARD_SPAN = 1030

    private val panel = Color(226, 226, 222)
    private val panelShadow = Color(190, 192, 190)
    private val lobby = Color(30, 34, 40)

    override val palette: List<Color> = listOf(panel, panelShadow, lobby) + Masonry.PALETTE
    override val fillsView: Boolean = false

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
    ) {
        val frame = FacilityFrame(area, footY)
        val left = frame.x(-HALF_WIDTH)
        val right = frame.x(HALF_WIDTH)
        val top = frame.y(HEIGHT)
        Masonry.band(g, panel, left, right, top, frame.footY - top)
        BANDS.forEach { (bottom, bandTop) ->
            Masonry.ribbon(
                g,
                left + frame.length(12),
                right - frame.length(12),
                frame.y(bandTop),
                frame.length(bandTop - bottom),
                frame.length(MULLION_PITCH),
            )
        }
        Masonry.band(g, panelShadow, right - frame.length(4), right, top, frame.footY - top)
        paintLobby(g, frame, left, right)
        Masonry.band(
            g,
            lobby,
            frame.x(-HALF_WIDTH - OVERHANG),
            frame.x(HALF_WIDTH + OVERHANG),
            top - frame.length(CANOPY),
            frame.length(CANOPY),
        )
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount {
        val frame = FacilityFrame(area, footY)
        return FacilityGround.pylonsOver(area, footY, frame.y(HEIGHT + CANOPY), BOARD_SPAN)
    }

    private fun paintLobby(
        g: Graphics2D,
        frame: FacilityFrame,
        left: Int,
        right: Int,
    ) {
        val top = frame.y(LOBBY)
        Masonry.band(g, lobby, left, right, top, frame.footY - top)
        var offset = COLUMN_PITCH / 2
        while (offset < HALF_WIDTH) {
            listOf(offset, -offset).forEach { center ->
                Masonry.band(g, panel, frame.x(center - COLUMN_HALF), frame.x(center + COLUMN_HALF), top, frame.footY - top)
            }
            offset += COLUMN_PITCH
        }
    }
}
