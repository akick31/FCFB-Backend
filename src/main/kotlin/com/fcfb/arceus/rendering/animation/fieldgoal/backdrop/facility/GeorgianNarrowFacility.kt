package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Polygon

object GeorgianNarrowFacility : FacilityStyle {
    private const val HEIGHT = 150
    private const val BASE = 12
    private const val CORNICE = 8
    private const val PAVILION_HALF = 70
    private const val PEDIMENT_RISE = 30
    private const val PROJECTION = 3
    private const val WINDOW_WIDTH = 13
    private const val WINDOW_HEIGHT = 30
    private val UPPER_FLOORS = listOf(62, 106)
    private const val PAVILION_WINDOW = 30
    private const val FIRST_WINDOW = 110
    private const val WINDOW_PITCH = 60
    private const val GROUND_WINDOW = 170
    private const val GROUND_WINDOW_PITCH = 120
    private const val EDGE_MARGIN = 30
    private const val DOOR_HALF = 15
    private const val DOOR_HEIGHT = 44
    private const val BOARD_SPAN = 1030

    private val brick = GeorgianColors.brick
    private val brickShadow = GeorgianColors.brickShadow
    private val trim = GeorgianColors.trim
    private val trimShadow = GeorgianColors.trimShadow

    override val palette: List<Color> = GeorgianColors.PALETTE + Masonry.PALETTE
    override val fillsView: Boolean = false

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
        fullWidth: Boolean,
    ) {
        val frame = FacilityFrame(area, footY)
        if (fullWidth) {
            GeorgianWings.paint(g, frame)
            return
        }
        val halfWidth = LowBuildingLayout.CENTER_HALF
        val left = frame.x(-halfWidth)
        val right = frame.x(halfWidth)
        val top = frame.y(HEIGHT)
        Masonry.band(g, brick, left, right, top, frame.footY - top)
        Masonry.band(g, brickShadow, frame.x(-PAVILION_HALF) - frame.length(PROJECTION), frame.x(-PAVILION_HALF), top, frame.footY - top)
        Masonry.band(g, brickShadow, frame.x(PAVILION_HALF), frame.x(PAVILION_HALF) + frame.length(PROJECTION), top, frame.footY - top)
        Masonry.band(g, trimShadow, left, right, frame.y(BASE), frame.length(BASE))
        paintWindows(g, frame, halfWidth)
        paintDoor(g, frame)
        Masonry.band(g, trim, left - frame.length(CORNICE), right + frame.length(CORNICE), top, frame.length(CORNICE))
        paintPediment(g, frame, top)
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount {
        val frame = FacilityFrame(area, footY)
        return FacilityGround.pylonsOver(area, footY, frame.y(HEIGHT + PEDIMENT_RISE), BOARD_SPAN)
    }

    private fun paintWindows(
        g: Graphics2D,
        frame: FacilityFrame,
        halfWidth: Int,
    ) {
        val columns =
            listOf(PAVILION_WINDOW, -PAVILION_WINDOW) + LowBuildingLayout.repeating(FIRST_WINDOW, WINDOW_PITCH, halfWidth, EDGE_MARGIN)
        UPPER_FLOORS.forEach { floor ->
            columns.forEach { center ->
                Masonry.window(g, frame.x(center), frame.y(floor + WINDOW_HEIGHT), frame.length(WINDOW_WIDTH), frame.length(WINDOW_HEIGHT))
            }
        }
        LowBuildingLayout.repeating(GROUND_WINDOW, GROUND_WINDOW_PITCH, halfWidth, EDGE_MARGIN).forEach { center ->
            Masonry.window(g, frame.x(center), frame.y(BASE + 8 + WINDOW_HEIGHT), frame.length(WINDOW_WIDTH), frame.length(WINDOW_HEIGHT))
        }
    }

    private fun paintDoor(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        Masonry.band(g, trim, frame.x(-DOOR_HALF - 3), frame.x(DOOR_HALF + 3), frame.y(DOOR_HEIGHT + 3), frame.length(DOOR_HEIGHT + 3))
        Masonry.band(g, Masonry.glass, frame.x(-DOOR_HALF), frame.x(DOOR_HALF), frame.y(DOOR_HEIGHT), frame.length(DOOR_HEIGHT))
    }

    private fun paintPediment(
        g: Graphics2D,
        frame: FacilityFrame,
        top: Int,
    ) {
        val left = frame.x(-PAVILION_HALF - CORNICE)
        val right = frame.x(PAVILION_HALF + CORNICE)
        val peak = frame.y(HEIGHT + PEDIMENT_RISE)
        g.color = trim
        g.fillPolygon(Polygon(intArrayOf(left, frame.x(0), right), intArrayOf(top, peak, top), 3))
        val inset = frame.length(CORNICE)
        g.color = brick
        g.fillPolygon(Polygon(intArrayOf(left + inset * 2, frame.x(0), right - inset * 2), intArrayOf(top - 1, peak + inset, top - 1), 3))
    }
}
