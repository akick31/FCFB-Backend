package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.CENTER_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.PAVILION_CENTER
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.PAVILION_END
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.PAVILION_HALF
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.LowBuildingLayout.WING_END
import java.awt.Graphics2D

object CollegiateWings {
    private const val BASE = 16
    private const val BAND = 4
    private const val PARAPET = 8
    private const val WING_HEIGHT = 112
    private val WING_BANDS = listOf(48)
    private const val WING_RIBBON_BOTTOM = 76
    private const val WING_RIBBON_TOP = 96
    private const val MULLION_PITCH = 14
    private const val GATE_PITCH = 60
    private const val GATE_WIDTH = 22
    private const val GATE_HEIGHT = 30
    private const val PAVILION_HEIGHT = 172
    private val PAVILION_BANDS = listOf(48, 120)
    private const val TALL_WINDOW_X = 16
    private const val TALL_WINDOW_BOTTOM = 58
    private const val TALL_WINDOW_TOP = 112
    private const val WINDOW_WIDTH = 11
    private const val OCULUS_Y = 146
    private const val OCULUS = 22
    private const val OUTER_HEIGHT = 92
    private const val ARCH_PITCH = 104
    private const val ARCH_WIDTH = 26
    private const val ARCH_BOTTOM = 22
    private const val ARCH_TOP = 70

    fun paint(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val outerEnd = LowBuildingLayout.outerEnd(frame)
        LowBuildingLayout.mirrored(PAVILION_END, outerEnd).forEach { (from, to) -> paintOuter(g, frame, minOf(from, to), maxOf(from, to)) }
        LowBuildingLayout.mirrored(CENTER_HALF, WING_END).forEach { (from, to) -> paintWing(g, frame, minOf(from, to), maxOf(from, to)) }
        listOf(PAVILION_CENTER, -PAVILION_CENTER).forEach { paintPavilion(g, frame, it) }
    }

    private fun paintWing(
        g: Graphics2D,
        frame: FacilityFrame,
        from: Int,
        to: Int,
    ) {
        paintMass(g, frame, from, to, WING_HEIGHT, WING_BANDS)
        Masonry.ribbon(
            g,
            frame.x(from),
            frame.x(to),
            frame.y(WING_RIBBON_TOP),
            frame.length(WING_RIBBON_TOP - WING_RIBBON_BOTTOM),
            frame.length(MULLION_PITCH),
        )
        var x = from + GATE_PITCH / 2
        while (x < to - GATE_WIDTH) {
            Masonry.band(
                g,
                Masonry.glass,
                frame.x(x - GATE_WIDTH / 2),
                frame.x(x + GATE_WIDTH / 2),
                frame.y(GATE_HEIGHT),
                frame.length(GATE_HEIGHT),
            )
            x += GATE_PITCH
        }
    }

    private fun paintOuter(
        g: Graphics2D,
        frame: FacilityFrame,
        from: Int,
        to: Int,
    ) {
        paintMass(g, frame, from, to, OUTER_HEIGHT, emptyList())
        var x = from + ARCH_PITCH / 2
        while (x < to - ARCH_WIDTH) {
            Masonry.archedWindow(g, frame.x(x), frame.y(ARCH_TOP), frame.length(ARCH_WIDTH), frame.length(ARCH_TOP - ARCH_BOTTOM))
            x += ARCH_PITCH
        }
    }

    private fun paintPavilion(
        g: Graphics2D,
        frame: FacilityFrame,
        center: Int,
    ) {
        val from = center - PAVILION_HALF
        val to = center + PAVILION_HALF
        paintMass(g, frame, from, to, PAVILION_HEIGHT, PAVILION_BANDS)
        Masonry.band(
            g,
            Masonry.brickShadow,
            frame.x(to) - frame.length(5),
            frame.x(to),
            frame.y(PAVILION_HEIGHT),
            frame.length(PAVILION_HEIGHT),
        )
        listOf(center - TALL_WINDOW_X, center + TALL_WINDOW_X).forEach { x ->
            Masonry.window(
                g,
                frame.x(x),
                frame.y(TALL_WINDOW_TOP),
                frame.length(WINDOW_WIDTH),
                frame.length(TALL_WINDOW_TOP - TALL_WINDOW_BOTTOM),
            )
        }
        Masonry.oculus(g, frame.x(center), frame.y(OCULUS_Y), frame.length(OCULUS))
    }

    private fun paintMass(
        g: Graphics2D,
        frame: FacilityFrame,
        from: Int,
        to: Int,
        height: Int,
        bands: List<Int>,
    ) {
        val top = frame.y(height)
        Masonry.band(g, Masonry.brick, frame.x(from), frame.x(to), top, frame.footY - top)
        bands.forEach { level -> Masonry.band(g, Masonry.stone, frame.x(from), frame.x(to), frame.y(level + BAND), frame.length(BAND)) }
        Masonry.band(g, Masonry.stone, frame.x(from), frame.x(to), frame.y(BASE), frame.length(BASE))
        Masonry.band(g, Masonry.stone, frame.x(from) - frame.length(3), frame.x(to) + frame.length(3), top, frame.length(PARAPET))
    }
}
