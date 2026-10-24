package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.JumbotronMount
import java.awt.Color
import java.awt.Graphics2D

object BrickFacility : FacilityStyle {
    private const val GROUND_STORY = 34
    private const val GATE_WIDTH = 34
    private const val GATE_HEIGHT = 26
    private const val GATE_PITCH = 145
    private const val FIRST_TOWER = 305
    private const val TOWER_PITCH = 290
    private const val TOWER_HALF = 36
    private const val TOWER_SHADOW = 6
    private const val MID_BAND = 104
    private const val RIBBON_TOP = 140
    private const val RIBBON_BOTTOM = 112
    private const val MULLION_PITCH = 18
    private const val UPPER_BAND = 150
    private const val FRIEZE_TOP = 200
    private const val BAND = 6
    private const val WINDOW_BOTTOM = 52
    private const val WINDOW_TOP = 92
    private const val WINDOW_WIDTH = 12
    private const val OCULUS_Y = 124
    private const val OCULUS = 20

    override val palette: List<Color> = Masonry.PALETTE
    override val fillsView: Boolean = true

    override fun paint(
        g: Graphics2D,
        area: BackdropArea,
        footY: Int,
        fullWidth: Boolean,
    ) {
        val frame = FacilityFrame(area, area.groundY)
        Masonry.band(g, Masonry.brick, 0, area.width, area.topY, area.groundY - area.topY)
        paintFrieze(g, frame)
        Masonry.band(g, Masonry.stone, 0, area.width, frame.y(MID_BAND + BAND), frame.length(BAND))
        Masonry.ribbon(
            g,
            0,
            area.width,
            frame.y(RIBBON_TOP),
            frame.length(RIBBON_TOP - RIBBON_BOTTOM),
            frame.length(MULLION_PITCH),
        )
        Masonry.band(g, Masonry.stone, 0, area.width, frame.y(UPPER_BAND + BAND), frame.length(BAND))
        towerCenters(frame).forEach { paintTower(g, frame, it) }
        paintGroundStory(g, frame)
    }

    override fun boardOver(
        area: BackdropArea,
        footY: Int,
    ): JumbotronMount = FacilityGround.onFace(area)

    private fun towerCenters(frame: FacilityFrame): List<Int> {
        val right = generateSequence(FIRST_TOWER) { it + TOWER_PITCH }.takeWhile { it - TOWER_HALF < frame.rightEdgeReference() }.toList()
        return right + right.map { -it }
    }

    private fun paintFrieze(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val top = frame.y(FRIEZE_TOP)
        Masonry.band(g, Masonry.stone, 0, frame.area.width, top, frame.y(UPPER_BAND + BAND) - top)
        Masonry.band(g, Masonry.stoneShadow, 0, frame.area.width, top, frame.length(2))
    }

    private fun paintTower(
        g: Graphics2D,
        frame: FacilityFrame,
        center: Int,
    ) {
        val left = frame.x(center - TOWER_HALF)
        val right = frame.x(center + TOWER_HALF)
        Masonry.band(g, Masonry.brick, left, right, frame.area.topY, frame.footY - frame.area.topY)
        Masonry.band(g, Masonry.brickShadow, right - frame.length(TOWER_SHADOW), right, frame.area.topY, frame.footY - frame.area.topY)
        listOf(MID_BAND, UPPER_BAND, FRIEZE_TOP).forEach { level ->
            Masonry.band(g, Masonry.stone, left - 1, right + 1, frame.y(level + BAND), frame.length(BAND))
        }
        Masonry.window(g, frame.x(center), frame.y(WINDOW_TOP), frame.length(WINDOW_WIDTH), frame.length(WINDOW_TOP - WINDOW_BOTTOM))
        Masonry.oculus(g, frame.x(center), frame.y(OCULUS_Y), frame.length(OCULUS))
    }

    private fun paintGroundStory(
        g: Graphics2D,
        frame: FacilityFrame,
    ) {
        val top = frame.y(GROUND_STORY)
        Masonry.band(g, Masonry.stone, 0, frame.area.width, top, frame.footY - top)
        Masonry.band(g, Masonry.stoneShadow, 0, frame.area.width, top, frame.length(2))
        var gate = GATE_PITCH / 2
        while (gate < frame.rightEdgeReference()) {
            listOf(gate, -gate).forEach { center ->
                Masonry.band(
                    g,
                    Masonry.glass,
                    frame.x(center - GATE_WIDTH / 2),
                    frame.x(center + GATE_WIDTH / 2),
                    frame.y(GATE_HEIGHT),
                    frame.length(GATE_HEIGHT),
                )
            }
            gate += GATE_PITCH
        }
    }
}
