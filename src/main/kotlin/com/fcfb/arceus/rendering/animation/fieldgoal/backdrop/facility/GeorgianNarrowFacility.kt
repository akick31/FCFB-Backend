package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Polygon

object GeorgianNarrowFacility : FacilityStyle {
    private const val HALF_WIDTH = 270
    private const val HEIGHT = 150
    private const val BASE = 12
    private const val CORNICE = 8
    private const val PAVILION_HALF = 70
    private const val PEDIMENT_RISE = 30
    private const val PROJECTION = 3
    private const val WINDOW_WIDTH = 13
    private const val WINDOW_HEIGHT = 30
    private val UPPER_FLOORS = listOf(62, 106)
    private val WINDOW_COLUMNS = listOf(30, 110, 170, 230)
    private const val DOOR_HALF = 15
    private const val DOOR_HEIGHT = 44
    private const val BOARD_SPAN = 1030

    private val brick = Color(156, 58, 42)
    private val brickShadow = Color(128, 46, 34)
    private val trim = Color(240, 236, 226)
    private val trimShadow = Color(204, 198, 186)

    override val palette: List<Color> = listOf(brick, brickShadow, trim, trimShadow) + Masonry.PALETTE
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
        Masonry.band(g, brick, left, right, top, frame.footY - top)
        Masonry.band(g, brickShadow, frame.x(-PAVILION_HALF) - frame.length(PROJECTION), frame.x(-PAVILION_HALF), top, frame.footY - top)
        Masonry.band(g, brickShadow, frame.x(PAVILION_HALF), frame.x(PAVILION_HALF) + frame.length(PROJECTION), top, frame.footY - top)
        Masonry.band(g, trimShadow, left, right, frame.y(BASE), frame.length(BASE))
        paintWindows(g, frame)
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
    ) {
        UPPER_FLOORS.forEach { floor ->
            WINDOW_COLUMNS.forEach { column ->
                listOf(column, -column).forEach { center ->
                    Masonry.window(
                        g,
                        frame.x(center),
                        frame.y(floor + WINDOW_HEIGHT),
                        frame.length(WINDOW_WIDTH),
                        frame.length(WINDOW_HEIGHT),
                    )
                }
            }
        }
        listOf(170, -170).forEach { center ->
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
