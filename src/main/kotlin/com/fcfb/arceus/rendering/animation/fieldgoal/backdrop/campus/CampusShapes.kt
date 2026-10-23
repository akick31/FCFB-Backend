package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D

object CampusShapes {
    fun trapezoidRoof(
        g: Graphics2D,
        left: Int,
        baseY: Int,
        width: Int,
        height: Int,
        inset: Int,
        color: Color,
    ) {
        val roof = Path2D.Float()
        roof.moveTo(left.toDouble(), baseY.toDouble())
        roof.lineTo((left + inset).toDouble(), (baseY - height).toDouble())
        roof.lineTo((left + width - inset).toDouble(), (baseY - height).toDouble())
        roof.lineTo((left + width).toDouble(), baseY.toDouble())
        roof.closePath()
        g.color = color
        g.fill(roof)
    }

    fun gable(
        g: Graphics2D,
        left: Int,
        baseY: Int,
        width: Int,
        height: Int,
        color: Color,
    ) = trapezoidRoof(g, left, baseY, width, height, width / 2, color)

    fun framedWindow(
        g: Graphics2D,
        centerX: Int,
        centerY: Int,
        width: Int,
        height: Int,
        frame: Color,
        pane: Color,
    ) {
        g.color = frame
        g.fillRect(centerX - width / 2 - 1, centerY - height / 2 - 1, width + 2, height + 2)
        g.color = pane
        g.fillRect(centerX - width / 2, centerY - height / 2, width, height)
    }

    fun windowGrid(
        g: Graphics2D,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        pitch: Int,
        floor: Int,
        paint: (centerX: Int, centerY: Int) -> Unit,
    ) {
        val floors = (height / floor).coerceAtLeast(1)
        val columns = ((width - pitch) / pitch).coerceAtLeast(1)
        val startX = left + (width - (columns - 1) * pitch) / 2
        repeat(floors) { floorIndex ->
            val centerY = top + floor * floorIndex + floor / 2
            repeat(columns) { column -> paint(startX + column * pitch, centerY) }
        }
    }

    fun columns(
        g: Graphics2D,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        count: Int,
        columnWidth: Int,
        color: Color,
    ) {
        g.color = color
        repeat(count) { index ->
            val centerX = left + width * (index * 2 + 1) / (count * 2)
            g.fillRect(centerX - columnWidth / 2, top, columnWidth, height)
        }
    }

    fun pointedArch(
        g: Graphics2D,
        centerX: Int,
        top: Int,
        width: Int,
        height: Int,
        color: Color,
    ) {
        val arch = Path2D.Float()
        arch.moveTo((centerX - width / 2).toDouble(), (top + height).toDouble())
        arch.lineTo((centerX - width / 2).toDouble(), (top + width).toDouble())
        arch.lineTo(centerX.toDouble(), top.toDouble())
        arch.lineTo((centerX + width / 2).toDouble(), (top + width).toDouble())
        arch.lineTo((centerX + width / 2).toDouble(), (top + height).toDouble())
        arch.closePath()
        g.color = color
        g.fill(arch)
    }

    fun roundArch(
        g: Graphics2D,
        centerX: Int,
        top: Int,
        width: Int,
        height: Int,
        color: Color,
    ) {
        g.color = color
        g.fillArc(centerX - width / 2, top, width, width, 0, 180)
        g.fillRect(centerX - width / 2, top + width / 2, width, height - width / 2)
    }
}
