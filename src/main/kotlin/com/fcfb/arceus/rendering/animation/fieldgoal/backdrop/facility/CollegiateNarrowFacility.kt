package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.abs

object CollegiateNarrowFacility : FacilityStyle {
    private const val HEIGHT = 128
    private const val FIRST_BAY = 240
    private const val BAY_HALF = 30
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
    private const val FIRST_GATE = 110
    private const val GATE_PITCH = 55
    private const val GATE_WIDTH = 22
    private const val GATE_HEIGHT = 30
    private const val GATE_CLEARANCE = 45
    private const val BOARD_SPAN = 1030

    override val palette: List<Color> = Masonry.PALETTE
    override val fillsView: Boolean = false

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
        fullWidth: Boolean,
    ) {
        val frame = FacilityFrame(area, footY)
        if (fullWidth) CollegiateWings.paint(g, frame)
        val bays = listOf(FIRST_BAY, -FIRST_BAY)
        val outer = FIRST_BAY + BAY_HALF
        val top = frame.y(HEIGHT)
        Masonry.band(g, Masonry.brick, frame.x(-outer), frame.x(outer), top, footY - top)
        Masonry.ribbon(
            g,
            frame.x(-outer),
            frame.x(outer),
            frame.y(RIBBON_TOP),
            frame.length(RIBBON_TOP - RIBBON_BOTTOM),
            frame.length(MULLION_PITCH),
        )
        Masonry.band(g, Masonry.stone, frame.x(-outer), frame.x(outer), top, frame.length(PARAPET))
        bays.forEach { paintBay(g, frame, it) }
        BANDS.forEach {
                level ->
            Masonry.band(g, Masonry.stone, frame.x(-outer), frame.x(outer), frame.y(level + BAND), frame.length(BAND))
        }
        Masonry.band(g, Masonry.stone, frame.x(-outer), frame.x(outer), frame.y(BASE), frame.length(BASE))
        paintGates(g, frame, outer, bays)
        Masonry.archedWindow(g, frame.x(0), frame.y(ENTRY_HEIGHT), frame.length(ENTRY_WIDTH), frame.length(ENTRY_HEIGHT))
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount = FacilityGround.pylonsOver(area, footY, FacilityFrame(area, footY).y(BAY_HEIGHT + PARAPET), BOARD_SPAN)

    private fun paintGates(
        g: Graphics2D,
        frame: FacilityFrame,
        outer: Int,
        bays: List<Int>,
    ) {
        LowBuildingLayout.repeating(FIRST_GATE, GATE_PITCH, outer, BAY_HALF)
            .filter { gate -> bays.none { abs(it - gate) < GATE_CLEARANCE } }
            .forEach { center ->
                Masonry.band(
                    g,
                    Masonry.glass,
                    frame.x(center - GATE_WIDTH / 2),
                    frame.x(center + GATE_WIDTH / 2),
                    frame.y(GATE_HEIGHT),
                    frame.length(GATE_HEIGHT),
                )
            }
    }

    private fun paintBay(
        g: Graphics2D,
        frame: FacilityFrame,
        center: Int,
    ) {
        val left = frame.x(center - BAY_HALF)
        val right = frame.x(center + BAY_HALF)
        val top = frame.y(BAY_HEIGHT)
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
