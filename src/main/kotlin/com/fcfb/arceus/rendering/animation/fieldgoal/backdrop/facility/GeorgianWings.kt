package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.GeorgianColors.brick
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.GeorgianColors.brickShadow
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.GeorgianColors.trim
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.CENTER_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.PAVILION_CENTER
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.PAVILION_END
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.PAVILION_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.WING_END
import java.awt.Graphics2D

object GeorgianWings {
    private const val BASE = 14
    private const val BAY = 70
    private const val PIER = 10
    private const val WING_HEIGHT = 122
    private const val GATE_WIDTH = 28
    private const val GATE_HEIGHT = 44
    private const val GATE_EVERY = 3
    private const val BELT_BOTTOM = 54
    private const val BELT = 6
    private const val CLUB_BOTTOM = 68
    private const val CLUB_TOP = 102
    private const val MULLION_PITCH = 12
    private const val COPING = 6
    private const val CANOPY = 5
    private const val CANOPY_OVERHANG = 6
    private const val TOWER_HEIGHT = 164
    private const val TOWER_EDGE = 7
    private const val SLOT_HALF = 8
    private const val SLOT_BOTTOM = 62
    private const val SLOT_TOP = 144
    private const val TOWER_CAP = 8
    private const val OUTER_HEIGHT = 88
    private const val TICKET_PITCH = 100
    private const val TICKET_WIDTH = 16
    private const val TICKET_BOTTOM = 26
    private const val TICKET_HEIGHT = 14
    private const val ENTRY_HEIGHT = 150
    private const val FRIEZE = 24
    private const val NAME_PLATE_HALF = 90
    private const val PORTAL_HALF = 92
    private const val PORTAL_TOP = 64
    private const val PORTAL_ARCH_X = 56
    private const val PORTAL_ARCH_TOP = 58
    private const val PORTAL_ARCH_WIDTH = 34

    fun paint(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val outerEnd = LowBuildingLayout.outerEnd(frame)
        LowBuildingLayout.mirrored(PAVILION_END, outerEnd).forEach {
                (from, to) ->
            paintConcourse(g, frame, minOf(from, to), maxOf(from, to))
        }
        LowBuildingLayout.mirrored(CENTER_HALF, WING_END).forEach {
                (from, to) ->
            paintClubWing(g, frame, minOf(from, to), maxOf(from, to))
        }
        listOf(PAVILION_CENTER, -PAVILION_CENTER).forEach { paintStairTower(g, frame, it) }
        paintMainEntry(g, frame)
    }

    private fun paintMainEntry(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        paintClubWing(g, frame, -CENTER_HALF, CENTER_HALF, ENTRY_HEIGHT)
        val frieze = frame.y(ENTRY_HEIGHT - COPING)
        Masonry.band(g, Masonry.stone, frame.x(-CENTER_HALF), frame.x(CENTER_HALF), frieze, frame.length(FRIEZE))
        Masonry.band(
            g,
            Masonry.stoneShadow,
            frame.x(-NAME_PLATE_HALF),
            frame.x(NAME_PLATE_HALF),
            frieze + frame.length(FRIEZE / 4),
            frame.length(FRIEZE / 2),
        )
        Masonry.band(g, Masonry.stone, frame.x(-PORTAL_HALF), frame.x(PORTAL_HALF), frame.y(PORTAL_TOP), frame.length(PORTAL_TOP))
        listOf(-PORTAL_ARCH_X, 0, PORTAL_ARCH_X).forEach { x ->
            Masonry.archedWindow(g, frame.x(x), frame.y(PORTAL_ARCH_TOP), frame.length(PORTAL_ARCH_WIDTH), frame.length(PORTAL_ARCH_TOP))
        }
    }

    private fun paintClubWing(
        g: Graphics2D,
        frame: FacilityFrame,
        from: Int,
        to: Int,
        height: Int = WING_HEIGHT,
    ) {
        val top = frame.y(height)
        Masonry.band(g, brick, frame.x(from), frame.x(to), top, frame.footY - top)
        Masonry.ribbon(g, frame.x(from), frame.x(to), frame.y(CLUB_TOP), frame.length(CLUB_TOP - CLUB_BOTTOM), frame.length(MULLION_PITCH))
        bays(from, to).forEach { pier ->
            Masonry.band(g, Masonry.stone, frame.x(pier - PIER / 2), frame.x(pier + PIER / 2), top, frame.footY - top)
            Masonry.band(
                g,
                Masonry.stoneShadow,
                frame.x(pier + PIER / 2) - frame.length(2),
                frame.x(pier + PIER / 2),
                top,
                frame.footY - top,
            )
        }
        archedBays(from, to).forEach { (left, right) ->
            Masonry.archedWindow(
                g,
                frame.x((left + right) / 2),
                frame.y(GATE_HEIGHT),
                frame.length(GATE_WIDTH),
                frame.length(GATE_HEIGHT - 2),
            )
        }
        Masonry.band(g, Masonry.stone, frame.x(from), frame.x(to), frame.y(BELT_BOTTOM + BELT), frame.length(BELT))
        Masonry.band(g, Masonry.stone, frame.x(from), frame.x(to), frame.y(BASE), frame.length(BASE))
        Masonry.band(g, Masonry.stone, frame.x(from), frame.x(to), top, frame.length(COPING))
        Masonry.band(
            g,
            Masonry.roof,
            frame.x(from - CANOPY_OVERHANG),
            frame.x(to + CANOPY_OVERHANG),
            frame.y(CLUB_TOP + CANOPY + 2),
            frame.length(CANOPY),
        )
    }

    private fun paintStairTower(
        g: Graphics2D,
        frame: FacilityFrame,
        center: Int,
    ) {
        val left = frame.x(center - PAVILION_HALF)
        val right = frame.x(center + PAVILION_HALF)
        val top = frame.y(TOWER_HEIGHT)
        Masonry.band(g, brick, left, right, top, frame.footY - top)
        Masonry.band(g, brickShadow, right - frame.length(5), right, top, frame.footY - top)
        Masonry.band(g, Masonry.stone, left, left + frame.length(TOWER_EDGE), top, frame.footY - top)
        Masonry.band(g, Masonry.stone, right - frame.length(TOWER_EDGE), right, top, frame.footY - top)
        Masonry.ribbon(
            g,
            frame.x(center - SLOT_HALF),
            frame.x(center + SLOT_HALF),
            frame.y(SLOT_TOP),
            frame.length(SLOT_TOP - SLOT_BOTTOM),
            frame.length(MULLION_PITCH),
        )
        Masonry.archedWindow(g, frame.x(center), frame.y(GATE_HEIGHT), frame.length(GATE_WIDTH + 6), frame.length(GATE_HEIGHT))
        Masonry.band(g, Masonry.stone, left, right, frame.y(BELT_BOTTOM + BELT), frame.length(BELT))
        Masonry.band(g, Masonry.stone, left, right, frame.y(BASE), frame.length(BASE))
        Masonry.band(g, trim, left - frame.length(3), right + frame.length(3), top, frame.length(TOWER_CAP))
    }

    private fun paintConcourse(
        g: Graphics2D,
        frame: FacilityFrame,
        from: Int,
        to: Int,
    ) {
        val top = frame.y(OUTER_HEIGHT)
        Masonry.band(g, brick, frame.x(from), frame.x(to), top, frame.footY - top)
        Masonry.band(g, Masonry.stone, frame.x(from), frame.x(to), frame.y(BASE), frame.length(BASE))
        Masonry.band(g, Masonry.stone, frame.x(from), frame.x(to), frame.y(BELT_BOTTOM + BELT), frame.length(BELT))
        Masonry.band(g, Masonry.stone, frame.x(from), frame.x(to), top, frame.length(COPING))
        var x = from + TICKET_PITCH / 2
        while (x < to - TICKET_WIDTH) {
            Masonry.band(
                g,
                Masonry.glass,
                frame.x(x - TICKET_WIDTH / 2),
                frame.x(x + TICKET_WIDTH / 2),
                frame.y(TICKET_BOTTOM + TICKET_HEIGHT),
                frame.length(TICKET_HEIGHT),
            )
            x += TICKET_PITCH
        }
    }

    private fun archedBays(
        from: Int,
        to: Int,
    ): List<Pair<Int, Int>> = bays(from, to).zipWithNext().filterIndexed { index, _ -> index % GATE_EVERY == GATE_EVERY / 2 }

    private fun bays(
        from: Int,
        to: Int,
    ): List<Int> = generateSequence(from) { it + BAY }.takeWhile { it <= to }.toList()
}
