package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D

object GrassPainter {
    private const val STRIPE_WIDTH = 40

    private val grass = Color(88, 140, 62)
    private val grassStripe = Color(78, 128, 54)

    val PALETTE: List<Color> = listOf(grass, grassStripe)

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
        topY: Int,
    ) {
        if (area.groundY <= topY) return
        g.color = grass
        g.fillRect(0, topY, area.width, area.groundY - topY)
    }

    fun paintStriped(
        g: Graphics2D,
        area: BackdropArea,
        topY: Int,
    ) {
        paint(g, area, topY)
        val stripe = area.scaled(STRIPE_WIDTH)
        g.color = grassStripe
        var x = stripe
        while (x < area.width) {
            g.fillRect(x, topY, stripe, area.groundY - topY)
            x += stripe * 2
        }
    }
}
