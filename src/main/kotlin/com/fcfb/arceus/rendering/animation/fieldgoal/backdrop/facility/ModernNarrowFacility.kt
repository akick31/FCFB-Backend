package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D

object ModernNarrowFacility : FacilityStyle {
    private const val HEIGHT = 140
    private const val LOBBY = 34
    private const val COLUMN_PITCH = 60
    private const val COLUMN_HALF = 3
    private val BANDS = listOf(52 to 76, 96 to 120)
    private const val MULLION_PITCH = 22
    private const val CANOPY = 8
    private const val OVERHANG = 22
    private const val BOARD_SPAN = 1030

    private val panel = ModernColors.panel
    private val panelShadow = ModernColors.panelShadow
    private val lobby = ModernColors.lobby

    override val palette: List<Color> = ModernColors.PALETTE + Masonry.PALETTE
    override val fillsView: Boolean = false

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
        fullWidth: Boolean,
    ) {
        val frame = FacilityFrame(area, footY)
        if (fullWidth) {
            ModernWings.paint(g, frame)
            return
        }
        val halfWidth = LowBuildingLayout.CENTER_HALF
        val left = frame.x(-halfWidth)
        val right = frame.x(halfWidth)
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
        paintLobby(g, frame, halfWidth)
        Masonry.band(
            g,
            lobby,
            frame.x(-halfWidth - OVERHANG),
            frame.x(halfWidth + OVERHANG),
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
        halfWidth: Int,
    ) {
        val top = frame.y(LOBBY)
        Masonry.band(g, lobby, frame.x(-halfWidth), frame.x(halfWidth), top, frame.footY - top)
        LowBuildingLayout.repeating(COLUMN_PITCH / 2, COLUMN_PITCH, halfWidth, 0).forEach { center ->
            Masonry.band(g, panel, frame.x(center - COLUMN_HALF), frame.x(center + COLUMN_HALF), top, frame.footY - top)
        }
    }
}
