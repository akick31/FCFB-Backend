package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D

object ConcreteFacility : FacilityStyle {
    private const val BASE = 34
    private const val OPENING_WIDTH = 40
    private const val OPENING_HEIGHT = 24
    private const val OPENING_PITCH = 160
    private const val PIER_PITCH = 120
    private const val PIER_HALF = 9
    private const val PRESS_BOTTOM = 116
    private const val PRESS_TOP = 144
    private const val MULLION_PITCH = 20
    private const val CORNICE = 150
    private const val CORNICE_HEIGHT = 8
    private const val UPPER_BAND = 220

    private val concrete = Color(186, 182, 174)
    private val concreteShadow = Color(150, 146, 138)
    private val base = Color(122, 118, 112)

    override val palette: List<Color> = listOf(concrete, concreteShadow, base) + Masonry.PALETTE
    override val fillsView: Boolean = true

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
        fullWidth: Boolean,
    ) {
        val frame = FacilityFrame(area, area.groundY)
        Masonry.band(g, concrete, 0, area.width, area.topY, area.groundY - area.topY)
        Masonry.ribbon(g, 0, area.width, frame.y(PRESS_TOP), frame.length(PRESS_TOP - PRESS_BOTTOM), frame.length(MULLION_PITCH))
        Masonry.band(g, concreteShadow, 0, area.width, frame.y(CORNICE + CORNICE_HEIGHT), frame.length(CORNICE_HEIGHT))
        Masonry.band(g, concreteShadow, 0, area.width, frame.y(UPPER_BAND), frame.length(2))
        paintPiers(g, frame)
        paintBase(g, frame)
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount = FacilityGround.onFace(area)

    private fun paintPiers(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        var center = PIER_PITCH / 2
        while (center - PIER_HALF < frame.rightEdgeReference()) {
            listOf(center, -center).forEach { pier ->
                val left = frame.x(pier - PIER_HALF)
                val right = frame.x(pier + PIER_HALF)
                Masonry.band(g, concrete, left, right, frame.area.topY, frame.footY - frame.area.topY)
                Masonry.band(g, concreteShadow, right - frame.length(3), right, frame.area.topY, frame.footY - frame.area.topY)
            }
            center += PIER_PITCH
        }
    }

    private fun paintBase(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val top = frame.y(BASE)
        Masonry.band(g, base, 0, frame.area.width, top, frame.footY - top)
        var opening = OPENING_PITCH / 2
        while (opening < frame.rightEdgeReference()) {
            listOf(opening, -opening).forEach { center ->
                Masonry.band(
                    g,
                    Masonry.glass,
                    frame.x(center - OPENING_WIDTH / 2),
                    frame.x(center + OPENING_WIDTH / 2),
                    frame.y(OPENING_HEIGHT),
                    frame.length(OPENING_HEIGHT),
                )
            }
            opening += OPENING_PITCH
        }
    }
}
