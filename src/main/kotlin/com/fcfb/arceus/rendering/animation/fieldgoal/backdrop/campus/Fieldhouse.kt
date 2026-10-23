package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import java.awt.Color
import java.awt.Graphics2D

object Fieldhouse : LandmarkBuilding {
    private val brick = Color(150, 76, 56)
    private val pilaster = Color(124, 62, 46)
    private val roof = Color(110, 132, 124)
    private val roofRib = Color(90, 110, 104)
    private val glass = Color(60, 72, 88)
    private val stone = Color(210, 198, 172)

    override val palette: List<Color> = listOf(brick, pilaster, roof, roofRib, glass, stone)

    override fun paint(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        width: Int,
        unit: Float,
    ) {
        val wallHeight = (unit * 0.42f).toInt()
        val vault = (unit * 0.24f).toInt()
        val top = groundY - wallHeight
        g.color = roof
        g.fillArc(left, top - vault, width, vault * 2, 0, 180)
        g.color = roofRib
        val ribs = 9
        for (rib in 1 until ribs) {
            val x = left + width * rib / ribs
            g.fillRect(x, top - (vault * 0.95f).toInt() + (vault * kotlin.math.abs(rib - ribs / 2f) / (ribs / 2f) * 0.6f).toInt(), 1, vault)
        }
        g.color = brick
        g.fillRect(left, top, width, wallHeight)
        g.color = stone
        g.fillRect(left, top, width, (unit * 0.03f).toInt().coerceAtLeast(2))
        g.fillRect(left, groundY - (unit * 0.04f).toInt(), width, (unit * 0.04f).toInt())
        g.color = pilaster
        val bay = (unit * 0.3f).toInt().coerceAtLeast(6)
        var x = left
        while (x < left + width) {
            g.fillRect(x, top, (bay / 8).coerceAtLeast(2), wallHeight)
            x += bay
        }
        val fanlight = (unit * 0.5f).toInt()
        val centerX = left + width / 2
        g.color = stone
        g.fillArc(centerX - fanlight / 2 - 3, top - fanlight / 2 + wallHeight / 3 - 3, fanlight + 6, fanlight + 6, 0, 180)
        g.color = glass
        g.fillArc(centerX - fanlight / 2, top - fanlight / 2 + wallHeight / 3, fanlight, fanlight, 0, 180)
        val arch = (unit * 0.1f).toInt().coerceAtLeast(3)
        listOf(-1, 0, 1).forEach { offset ->
            CampusShapes.roundArch(g, centerX + offset * arch * 2, groundY - arch * 2, arch, arch * 2, glass)
        }
    }
}
