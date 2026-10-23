package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D

object CollegiateNarrowFacility : FacilityStyle {
    private const val HALF_WIDTH = 270
    private const val HEIGHT = 128
    private const val BAY_INNER = 210
    private const val BAY_HEIGHT = 150
    private const val BASE = 16
    private const val BAND = 4
    private val BANDS = listOf(48, 96)
    private const val PARAPET = 8
    private const val RIBBON_BOTTOM = 102
    private const val RIBBON_TOP = 120
    private const val MULLION_PITCH = 14
    private const val BAY_OCULUS_Y = 126
    private const val OCULUS = 18
    private const val BAY_WINDOW_BOTTOM = 58
    private const val BAY_WINDOW_TOP = 90
    private const val WINDOW_WIDTH = 11
    private const val ENTRY_WIDTH = 46
    private const val ENTRY_HEIGHT = 58
    private val GATES = listOf(110, 165)
    private const val GATE_WIDTH = 22
    private const val GATE_HEIGHT = 30
    private const val BOARD_SPAN = 1030

    override val palette: List<Color> = Masonry.PALETTE
    override val fillsView: Boolean = false

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
    ) {
        val frame = FacilityFrame(area, footY)
        val top = frame.y(HEIGHT)
        Masonry.band(g, Masonry.brick, frame.x(-BAY_INNER), frame.x(BAY_INNER), top, footY - top)
        Masonry.ribbon(
            g,
            frame.x(-BAY_INNER),
            frame.x(BAY_INNER),
            frame.y(RIBBON_TOP),
            frame.length(RIBBON_TOP - RIBBON_BOTTOM),
            frame.length(MULLION_PITCH),
        )
        Masonry.band(g, Masonry.stone, frame.x(-BAY_INNER), frame.x(BAY_INNER), top, frame.length(PARAPET))
        listOf(-HALF_WIDTH, BAY_INNER).forEach { paintBay(g, frame, it) }
        BANDS.forEach {
                level ->
            Masonry.band(g, Masonry.stone, frame.x(-HALF_WIDTH), frame.x(HALF_WIDTH), frame.y(level + BAND), frame.length(BAND))
        }
        Masonry.band(g, Masonry.stone, frame.x(-HALF_WIDTH), frame.x(HALF_WIDTH), frame.y(BASE), frame.length(BASE))
        GATES.flatMap { listOf(it, -it) }.forEach { center ->
            Masonry.band(
                g,
                Masonry.glass,
                frame.x(center - GATE_WIDTH / 2),
                frame.x(center + GATE_WIDTH / 2),
                frame.y(GATE_HEIGHT),
                frame.length(GATE_HEIGHT),
            )
        }
        Masonry.archedWindow(g, frame.x(0), frame.y(ENTRY_HEIGHT), frame.length(ENTRY_WIDTH), frame.length(ENTRY_HEIGHT))
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount = FacilityGround.pylonsOver(area, footY, FacilityFrame(area, footY).y(BAY_HEIGHT + PARAPET), BOARD_SPAN)

    private fun paintBay(
        g: Graphics2D,
        frame: FacilityFrame,
        leftReference: Int,
    ) {
        val width = HALF_WIDTH - BAY_INNER
        val left = frame.x(leftReference)
        val right = frame.x(leftReference + width)
        val top = frame.y(BAY_HEIGHT)
        val center = leftReference + width / 2
        Masonry.band(g, Masonry.brick, left, right, top, frame.footY - top)
        Masonry.band(g, Masonry.brickShadow, right - frame.length(4), right, top, frame.footY - top)
        Masonry.band(g, Masonry.stone, left - frame.length(3), right + frame.length(3), top, frame.length(PARAPET))
        Masonry.window(
            g,
            frame.x(center),
            frame.y(BAY_WINDOW_TOP),
            frame.length(WINDOW_WIDTH),
            frame.length(BAY_WINDOW_TOP - BAY_WINDOW_BOTTOM),
        )
        Masonry.oculus(g, frame.x(center), frame.y(BAY_OCULUS_Y), frame.length(OCULUS))
    }
}
