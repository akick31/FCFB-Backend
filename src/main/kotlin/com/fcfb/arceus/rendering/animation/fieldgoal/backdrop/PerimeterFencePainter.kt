package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints

object PerimeterFencePainter {
    private const val HEIGHT = 16
    private const val POST_SPACING = 46
    private const val MESH_SPACING = 4
    private const val SINK = 2

    private val post = Color(112, 116, 120)
    private val mesh = Color(176, 180, 184)

    val PALETTE: List<Color> = listOf(post, mesh)

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
    ) {
        val bottom = area.groundY + area.scaled(SINK)
        val height = area.scaled(HEIGHT)
        val top = bottom - height
        val previousAntialias = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        paintMesh(g, area.width, top, height)
        g.color = post
        g.fillRect(0, top, area.width, 1)
        val spacing = area.scaled(POST_SPACING).coerceAtLeast(12)
        var x = spacing / 2
        while (x < area.width) {
            g.fillRect(x, top, 2, height)
            x += spacing
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAntialias)
    }

    private fun paintMesh(
        g: Graphics2D,
        width: Int,
        top: Int,
        height: Int,
    ) {
        g.color = mesh
        var x = -height
        while (x < width + height) {
            g.drawLine(x, top, x + height, top + height)
            g.drawLine(x + height, top, x, top + height)
            x += MESH_SPACING
        }
    }
}
