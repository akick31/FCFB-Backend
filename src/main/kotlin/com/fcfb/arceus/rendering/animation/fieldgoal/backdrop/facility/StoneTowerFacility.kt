package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.TOWER_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.TOWER_HEIGHT
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.WING_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.TowerMassing.WING_HEIGHT
import java.awt.Color
import java.awt.Graphics2D
import kotlin.math.abs

object StoneTowerFacility : FacilityStyle {
    private const val PLINTH = 10
    private const val BUTTRESS = 10
    private const val BUTTRESS_PITCH = 64
    private const val LANCET_WIDTH = 12
    private const val WING_LANCET_BOTTOM = 26
    private const val WING_LANCET_TOP = 76
    private const val TOWER_LANCET_BOTTOM = 100
    private const val TOWER_LANCET_TOP = 156
    private const val TOWER_LANCET_X = 30
    private const val PARAPET = 8
    private const val MERLON = 10
    private const val MERLON_PITCH = 20
    private const val TURRET = 14
    private const val TURRET_RISE = 26
    private const val DOOR_WIDTH = 30
    private const val DOOR_HEIGHT = 52

    override val palette: List<Color> = Masonry.PALETTE
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
    ): JumbotronMount = TowerMassing.boardOver(area, footY, TOWER_HEIGHT + PARAPET + MERLON + TURRET_RISE)

    private fun paintWings(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val top = frame.y(WING_HEIGHT)
        Masonry.band(g, Masonry.stone, frame.x(-WING_HALF), frame.x(WING_HALF), top, frame.footY - top)
        Masonry.band(g, Masonry.stoneShadow, frame.x(-WING_HALF), frame.x(WING_HALF), frame.y(PLINTH), frame.length(PLINTH))
        var offset = TOWER_HALF + BUTTRESS_PITCH / 2
        while (offset < WING_HALF) {
            listOf(offset, -offset).forEach { center ->
                Masonry.lancet(
                    g,
                    frame.x(center),
                    frame.y(WING_LANCET_TOP),
                    frame.length(LANCET_WIDTH),
                    frame.length(WING_LANCET_TOP - WING_LANCET_BOTTOM),
                )
                val buttress = center + BUTTRESS_PITCH / 2 * if (center > 0) 1 else -1
                if (abs(buttress) < WING_HALF) paintButtress(g, frame, buttress, WING_HEIGHT)
            }
            offset += BUTTRESS_PITCH
        }
        paintBattlements(g, frame, -WING_HALF, WING_HALF, WING_HEIGHT)
    }

    private fun paintTower(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val top = frame.y(TOWER_HEIGHT)
        Masonry.band(g, Masonry.stone, frame.x(-TOWER_HALF), frame.x(TOWER_HALF), top, frame.footY - top)
        Masonry.band(g, Masonry.stoneShadow, frame.x(TOWER_HALF) - frame.length(5), frame.x(TOWER_HALF), top, frame.footY - top)
        listOf(-TOWER_HALF, TOWER_HALF - BUTTRESS).forEach { paintButtress(g, frame, it + BUTTRESS / 2, TOWER_HEIGHT) }
        Masonry.lancet(g, frame.x(0), frame.y(DOOR_HEIGHT), frame.length(DOOR_WIDTH), frame.length(DOOR_HEIGHT))
        listOf(-TOWER_LANCET_X, 0, TOWER_LANCET_X).forEach { x ->
            Masonry.lancet(
                g,
                frame.x(x),
                frame.y(TOWER_LANCET_TOP),
                frame.length(LANCET_WIDTH),
                frame.length(TOWER_LANCET_TOP - TOWER_LANCET_BOTTOM),
            )
        }
        paintBattlements(g, frame, -TOWER_HALF, TOWER_HALF, TOWER_HEIGHT)
        listOf(-TOWER_HALF, TOWER_HALF - TURRET).forEach { left ->
            val base = TOWER_HEIGHT + PARAPET
            Masonry.band(g, Masonry.stone, frame.x(left), frame.x(left + TURRET), frame.y(base + TURRET_RISE), frame.length(TURRET_RISE))
            Masonry.band(
                g,
                Masonry.stoneShadow,
                frame.x(left + TURRET) - frame.length(3),
                frame.x(left + TURRET),
                frame.y(base + TURRET_RISE),
                frame.length(TURRET_RISE),
            )
        }
    }

    private fun paintButtress(
        g: Graphics2D,
        frame: FacilityFrame,
        center: Int,
        height: Int,
    ) {
        Masonry.band(
            g,
            Masonry.stoneShadow,
            frame.x(center - BUTTRESS / 2),
            frame.x(center + BUTTRESS / 2),
            frame.y(height),
            frame.length(height),
        )
        Masonry.band(g, Masonry.stone, frame.x(center - BUTTRESS / 2), frame.x(center), frame.y(height), frame.length(height))
    }

    private fun paintBattlements(
        g: Graphics2D,
        frame: FacilityFrame,
        leftReference: Int,
        rightReference: Int,
        height: Int,
    ) {
        Masonry.band(g, Masonry.stone, frame.x(leftReference), frame.x(rightReference), frame.y(height + PARAPET), frame.length(PARAPET))
        Masonry.band(g, Masonry.stoneShadow, frame.x(leftReference), frame.x(rightReference), frame.y(height), frame.length(2))
        var x = leftReference
        while (x + MERLON <= rightReference) {
            Masonry.band(g, Masonry.stone, frame.x(x), frame.x(x + MERLON), frame.y(height + PARAPET + MERLON), frame.length(MERLON))
            x += MERLON_PITCH
        }
    }
}
