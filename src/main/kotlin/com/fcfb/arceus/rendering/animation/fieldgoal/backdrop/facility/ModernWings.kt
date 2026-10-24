package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.CENTER_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.PAVILION_END
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.ModernColors.lobby
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.ModernColors.panel
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.ModernColors.panelShadow
import java.awt.Graphics2D

object ModernWings {
    private const val WING_HEIGHT = 118
    private const val PLAZA = 40
    private const val COLUMN_PITCH = 56
    private const val COLUMN_HALF = 3
    private const val SUITE_BOTTOM = 46
    private const val SUITE_TOP = 98
    private const val SUITE_MULLION = 10
    private const val FIN_PITCH = 48
    private const val FIN_WIDTH = 3
    private const val FASCIA_REVEAL = 2
    private const val ATRIUM_FRAME = 6
    private const val ATRIUM_MULLION = 16
    private const val ATRIUM_TRANSOM = 30
    private const val ENTRY_CANOPY = 52
    private const val CANOPY = 5
    private const val CANOPY_OVERHANG = 14
    private const val OUTER_HEIGHT = 86
    private const val SEAM_PITCH = 40
    private const val GATE_PITCH = 170
    private const val GATE_HALF = 24
    private const val GATE_HEIGHT = 36
    private const val SIGN_BOTTOM = 56
    private const val SIGN_HEIGHT = 10
    private const val MAIN_HEIGHT = 150
    private const val UPPER_SUITE_BOTTOM = 106
    private const val UPPER_SUITE_TOP = 138
    private const val MAIN_ATRIUM_HALF = 88
    private const val MAIN_ATRIUM_RISE = 14

    fun paint(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val outerEnd = LowBuildingLayout.outerEnd(frame)
        LowBuildingLayout.mirrored(PAVILION_END, outerEnd).forEach {
                (from, to) ->
            paintConcourse(g, frame, minOf(from, to), maxOf(from, to))
        }
        LowBuildingLayout.mirrored(CENTER_HALF, PAVILION_END).forEach {
                (from, to) ->
            paintSuiteWing(g, frame, minOf(from, to), maxOf(from, to))
        }
        paintMainBlock(g, frame)
    }

    private fun paintMainBlock(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        paintSuiteWing(g, frame, -CENTER_HALF, CENTER_HALF, MAIN_HEIGHT)
        Masonry.ribbon(
            g,
            frame.x(-CENTER_HALF),
            frame.x(CENTER_HALF),
            frame.y(UPPER_SUITE_TOP),
            frame.length(UPPER_SUITE_TOP - UPPER_SUITE_BOTTOM),
            frame.length(SUITE_MULLION),
        )
        Masonry.band(
            g,
            panelShadow,
            frame.x(-CENTER_HALF),
            frame.x(CENTER_HALF),
            frame.y(UPPER_SUITE_TOP + FASCIA_REVEAL),
            frame.length(FASCIA_REVEAL),
        )
        paintAtrium(g, frame, 0, MAIN_ATRIUM_HALF, MAIN_HEIGHT + MAIN_ATRIUM_RISE)
    }

    private fun paintSuiteWing(
        g: Graphics2D,
        frame: FacilityFrame,
        from: Int,
        to: Int,
        height: Int = WING_HEIGHT,
    ) {
        val top = frame.y(height)
        Masonry.band(g, panel, frame.x(from), frame.x(to), top, frame.footY - top)
        Masonry.ribbon(
            g,
            frame.x(from),
            frame.x(to),
            frame.y(SUITE_TOP),
            frame.length(SUITE_TOP - SUITE_BOTTOM),
            frame.length(SUITE_MULLION),
        )
        var fin = from + FIN_PITCH / 2
        while (fin < to) {
            Masonry.band(g, panelShadow, frame.x(fin), frame.x(fin + FIN_WIDTH), frame.y(SUITE_TOP), frame.length(SUITE_TOP - SUITE_BOTTOM))
            fin += FIN_PITCH
        }
        Masonry.band(g, panelShadow, frame.x(from), frame.x(to), frame.y(SUITE_TOP + FASCIA_REVEAL), frame.length(FASCIA_REVEAL))
        val plazaTop = frame.y(PLAZA)
        Masonry.band(g, lobby, frame.x(from), frame.x(to), plazaTop, frame.footY - plazaTop)
        var column = from + COLUMN_PITCH / 2
        while (column < to) {
            Masonry.band(g, panel, frame.x(column - COLUMN_HALF), frame.x(column + COLUMN_HALF), plazaTop, frame.footY - plazaTop)
            column += COLUMN_PITCH
        }
    }

    private fun paintAtrium(
        g: Graphics2D,
        frame: FacilityFrame,
        center: Int,
        halfWidth: Int,
        height: Int,
    ) {
        val from = center - halfWidth
        val to = center + halfWidth
        val top = frame.y(height)
        Masonry.band(g, panel, frame.x(from), frame.x(to), top, frame.footY - top)
        val inset = frame.length(ATRIUM_FRAME)
        Masonry.band(g, Masonry.glass, frame.x(from) + inset, frame.x(to) - inset, top + inset, frame.footY - top - inset)
        Masonry.band(g, Masonry.glassGlint, frame.x(from) + inset, frame.x(center), top + inset, frame.footY - top - inset)
        var x = from + ATRIUM_MULLION
        while (x < to) {
            Masonry.band(g, panelShadow, frame.x(x), frame.x(x) + 1, top + inset, frame.footY - top - inset)
            x += ATRIUM_MULLION
        }
        var level = ATRIUM_TRANSOM
        while (level < height - ATRIUM_FRAME) {
            Masonry.band(g, panelShadow, frame.x(from) + inset, frame.x(to) - inset, frame.y(level), 1)
            level += ATRIUM_TRANSOM
        }
        Masonry.band(
            g,
            lobby,
            frame.x(from - CANOPY_OVERHANG),
            frame.x(to + CANOPY_OVERHANG),
            frame.y(ENTRY_CANOPY),
            frame.length(CANOPY),
        )
    }

    private fun paintConcourse(
        g: Graphics2D,
        frame: FacilityFrame,
        from: Int,
        to: Int,
    ) {
        val top = frame.y(OUTER_HEIGHT)
        Masonry.band(g, panel, frame.x(from), frame.x(to), top, frame.footY - top)
        var seam = from + SEAM_PITCH
        while (seam < to) {
            Masonry.band(g, panelShadow, frame.x(seam), frame.x(seam) + 1, top, frame.footY - top)
            seam += SEAM_PITCH
        }
        Masonry.band(g, lobby, frame.x(from), frame.x(to), frame.y(SIGN_BOTTOM + SIGN_HEIGHT), frame.length(SIGN_HEIGHT))
        var gate = from + GATE_PITCH / 2
        while (gate < to - GATE_HALF) {
            Masonry.band(g, lobby, frame.x(gate - GATE_HALF), frame.x(gate + GATE_HALF), frame.y(GATE_HEIGHT), frame.length(GATE_HEIGHT))
            gate += GATE_PITCH
        }
        Masonry.band(g, panelShadow, frame.x(from), frame.x(to), top, frame.length(FASCIA_REVEAL * 2))
    }
}
