package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.TOWER_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.TOWER_HEIGHT
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.WING_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.WING_HEIGHT
import java.awt.Color
import java.awt.Graphics2D

object BrickTowerFacility : FacilityStyle {
    private const val BASE = 12
    private const val STRING_COURSE = 46
    private const val CORNICE = 6
    private const val DENTIL = 3
    private const val DENTIL_PITCH = 7
    private const val QUOIN = 8
    private const val QUOIN_COURSE = 10
    private val ARCADE = listOf(145, 215)
    private const val ARCADE_WIDTH = 18
    private const val ARCADE_BOTTOM = 14
    private const val ARCADE_TOP = 40
    private val UPPER_PAIRS = listOf(140, 212)
    private const val PAIR_GAP = 11
    private const val UPPER_BOTTOM = 56
    private const val UPPER_TOP = 80
    private const val WINDOW_WIDTH = 10
    private const val ENTRY_WIDTH = 34
    private const val ENTRY_HEIGHT = 46
    private const val TOWER_WINDOW_X = 28
    private const val TOWER_WINDOW_BOTTOM = 98
    private const val TOWER_WINDOW_TOP = 124
    private const val CLOCK_Y = 44
    private const val CLOCK = 20
    private const val BELFRY_BOTTOM = 154
    private const val BELFRY_TOP = 172
    private val BELFRY = listOf(-56, 0, 56)
    private const val ROOF_RISE = 32
    private const val ROOF_OVERHANG = 8
    private const val FLAGPOLE = 22

    override val palette: List<Color> = Masonry.PALETTE
    override val fillsView: Boolean = false

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
        fullWidth: Boolean,
    ) {
        val frame = FacilityFrame(area, footY)
        paintWings(g, frame)
        paintTower(g, frame)
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount = TowerMassing.boardOver(area, footY, TOWER_HEIGHT + ROOF_RISE)

    private fun paintWings(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val left = frame.x(-WING_HALF)
        val right = frame.x(WING_HALF)
        val top = frame.y(WING_HEIGHT)
        Masonry.band(g, Masonry.brick, left, right, top, frame.footY - top)
        Masonry.band(g, Masonry.stone, left, right, frame.y(BASE), frame.length(BASE))
        Masonry.band(g, Masonry.stone, left, right, frame.y(STRING_COURSE), frame.length(3))
        listOf(-WING_HALF, WING_HALF - QUOIN).forEach { paintQuoins(g, frame, it, WING_HEIGHT) }
        paintCornice(g, frame, -WING_HALF, WING_HALF, WING_HEIGHT)
        ARCADE.flatMap { listOf(it, -it) }.forEach { center ->
            Masonry.archedWindow(
                g,
                frame.x(center),
                frame.y(ARCADE_TOP),
                frame.length(ARCADE_WIDTH),
                frame.length(ARCADE_TOP - ARCADE_BOTTOM),
            )
        }
        UPPER_PAIRS.flatMap { listOf(it, -it) }.forEach { center ->
            listOf(center - PAIR_GAP, center + PAIR_GAP).forEach { x ->
                Masonry.window(g, frame.x(x), frame.y(UPPER_TOP), frame.length(WINDOW_WIDTH), frame.length(UPPER_TOP - UPPER_BOTTOM))
            }
            Masonry.band(
                g,
                Masonry.stone,
                frame.x(center - PAIR_GAP * 2),
                frame.x(center + PAIR_GAP * 2),
                frame.y(UPPER_TOP + 4),
                frame.length(3),
            )
        }
    }

    private fun paintTower(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val left = frame.x(-TOWER_HALF)
        val right = frame.x(TOWER_HALF)
        val top = frame.y(TOWER_HEIGHT)
        Masonry.band(g, Masonry.brick, left, right, top, frame.footY - top)
        Masonry.band(g, Masonry.brickShadow, right - frame.length(5), right, top, frame.footY - top)
        listOf(-TOWER_HALF, TOWER_HALF - QUOIN).forEach { paintQuoins(g, frame, it, TOWER_HEIGHT) }
        listOf(WING_HEIGHT, BELFRY_BOTTOM - 4).forEach {
                level ->
            Masonry.band(g, Masonry.stone, left, right, frame.y(level), frame.length(4))
        }
        Masonry.band(g, Masonry.stone, frame.x(-ENTRY_WIDTH / 2 - 5), frame.x(ENTRY_WIDTH / 2 + 5), frame.y(BASE), frame.length(BASE))
        Masonry.archedWindow(g, frame.x(0), frame.y(ENTRY_HEIGHT), frame.length(ENTRY_WIDTH), frame.length(ENTRY_HEIGHT))
        Masonry.clock(g, frame.x(0), frame.y(WING_HEIGHT + CLOCK_Y), frame.length(CLOCK))
        listOf(-TOWER_WINDOW_X, TOWER_WINDOW_X).forEach { x ->
            Masonry.archedWindow(
                g,
                frame.x(x),
                frame.y(TOWER_WINDOW_TOP),
                frame.length(WINDOW_WIDTH),
                frame.length(TOWER_WINDOW_TOP - TOWER_WINDOW_BOTTOM),
            )
        }
        BELFRY.forEach { x ->
            Masonry.archedWindow(
                g,
                frame.x(x),
                frame.y(BELFRY_TOP),
                frame.length(WINDOW_WIDTH + 6),
                frame.length(BELFRY_TOP - BELFRY_BOTTOM),
            )
        }
        paintCornice(g, frame, -TOWER_HALF, TOWER_HALF, TOWER_HEIGHT)
        paintRoof(g, frame)
    }

    private fun paintRoof(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val eave = frame.y(TOWER_HEIGHT)
        val peak = frame.y(TOWER_HEIGHT + ROOF_RISE)
        g.color = Masonry.roof
        g.fillPolygon(
            intArrayOf(
                frame.x(-TOWER_HALF - ROOF_OVERHANG),
                frame.x(-TOWER_HALF / 3),
                frame.x(TOWER_HALF / 3),
                frame.x(TOWER_HALF + ROOF_OVERHANG),
            ),
            intArrayOf(eave, peak, peak, eave),
            4,
        )
        Masonry.band(g, Masonry.glass, frame.x(0), frame.x(0) + 1, frame.y(TOWER_HEIGHT + ROOF_RISE + FLAGPOLE), frame.length(FLAGPOLE))
    }

    private fun paintQuoins(
        g: Graphics2D,
        frame: FacilityFrame,
        leftReference: Int,
        height: Int,
    ) {
        var level = 0
        var long = true
        while (level < height) {
            val width = if (long) QUOIN + 4 else QUOIN
            val start = if (leftReference < 0) leftReference else leftReference + QUOIN - width
            Masonry.band(
                g,
                Masonry.stone,
                frame.x(start),
                frame.x(start + width),
                frame.y(level + QUOIN_COURSE),
                frame.length(QUOIN_COURSE - 1),
            )
            level += QUOIN_COURSE
            long = !long
        }
    }

    private fun paintCornice(
        g: Graphics2D,
        frame: FacilityFrame,
        leftReference: Int,
        rightReference: Int,
        height: Int,
    ) {
        Masonry.band(g, Masonry.stone, frame.x(leftReference - 4), frame.x(rightReference + 4), frame.y(height), frame.length(CORNICE))
        var x = leftReference
        while (x < rightReference) {
            Masonry.band(g, Masonry.stone, frame.x(x), frame.x(x + DENTIL), frame.y(height - CORNICE), frame.length(DENTIL))
            x += DENTIL_PITCH
        }
    }
}
