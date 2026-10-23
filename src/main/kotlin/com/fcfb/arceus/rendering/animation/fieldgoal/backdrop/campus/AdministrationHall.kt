package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import java.awt.Color
import java.awt.Graphics2D

object AdministrationHall : LandmarkBuilding {
    private val stone = Color(214, 204, 182)
    private val stoneShadow = Color(184, 172, 148)
    private val dome = Color(110, 160, 140)
    private val window = Color(56, 66, 80)
    private val trim = Color(240, 236, 224)

    override val palette: List<Color> = listOf(stone, stoneShadow, dome, window, trim)

    override fun paint(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        width: Int,
        unit: Float,
    ) {
        val height = (unit * 0.46f).toInt()
        val top = groundY - height
        val centerX = left + width / 2
        val drumWidth = (unit * 0.42f).toInt()
        val drumHeight = (unit * 0.14f).toInt()
        val drumTop = top - drumHeight
        g.color = dome
        g.fillArc(centerX - drumWidth / 2, drumTop - drumWidth / 2, drumWidth, drumWidth, 0, 180)
        val cupola = (drumWidth / 8).coerceAtLeast(2)
        g.fillRect(centerX - cupola / 2, drumTop - drumWidth / 2 - cupola * 2, cupola, cupola * 2)
        g.color = stone
        g.fillRect(centerX - drumWidth / 2, drumTop, drumWidth, drumHeight)
        CampusShapes.columns(
            g,
            centerX - drumWidth / 2,
            drumTop,
            drumWidth,
            drumHeight,
            8,
            (unit * 0.015f).toInt().coerceAtLeast(1),
            stoneShadow,
        )
        g.color = stone
        g.fillRect(left, top, width, height)
        g.color = trim
        g.fillRect(left, top, width, (unit * 0.03f).toInt().coerceAtLeast(2))
        val balustrade = (unit * 0.02f).toInt().coerceAtLeast(1)
        var x = left
        while (x < left + width) {
            g.fillRect(x, top - balustrade * 2, balustrade, balustrade * 2)
            x += balustrade * 3
        }
        g.fillRect(left, top - balustrade * 2, width, 1)
        CampusShapes.windowGrid(
            g,
            left,
            top + (unit * 0.04f).toInt(),
            width,
            height - (unit * 0.04f).toInt(),
            (unit * 0.08f).toInt().coerceAtLeast(3),
            (unit * 0.13f).toInt().coerceAtLeast(3),
        ) { wx, wy ->
            CampusShapes.framedWindow(
                g,
                wx,
                wy,
                (unit * 0.035f).toInt().coerceAtLeast(1),
                (unit * 0.07f).toInt().coerceAtLeast(2),
                trim,
                window,
            )
        }
        val porticoWidth = (unit * 0.62f).toInt()
        val porticoHeight = (height * 0.86f).toInt()
        g.color = window
        g.fillRect(centerX - porticoWidth / 2, groundY - porticoHeight, porticoWidth, porticoHeight)
        CampusShapes.columns(
            g,
            centerX - porticoWidth / 2,
            groundY - porticoHeight,
            porticoWidth,
            porticoHeight,
            8,
            (unit * 0.03f).toInt().coerceAtLeast(1),
            trim,
        )
        g.fillRect(centerX - porticoWidth / 2, groundY - porticoHeight, porticoWidth, (unit * 0.03f).toInt().coerceAtLeast(1))
        CampusShapes.gable(
            g,
            centerX - porticoWidth / 2 - porticoWidth / 16,
            groundY - porticoHeight,
            porticoWidth + porticoWidth / 8,
            (porticoWidth * 0.2f).toInt(),
            trim,
        )
    }
}
