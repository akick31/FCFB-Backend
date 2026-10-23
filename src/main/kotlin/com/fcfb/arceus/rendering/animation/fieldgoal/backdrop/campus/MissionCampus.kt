package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.campus

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random

object MissionCampus : CampusStyle {
    private enum class Kind { ARCADE, BELL_TOWER, CHURCH, WING }

    private val stuccoShades = listOf(Color(238, 228, 208), Color(230, 218, 196))
    private val tile = Color(178, 80, 52)
    private val tileShadow = Color(144, 62, 42)
    private val shade = Color(88, 70, 60)
    private val palmTrunk = Color(128, 100, 72)
    private val palmFrond = Color(60, 112, 58)
    private val cypress = Color(36, 72, 44)

    override val kindCount = Kind.entries.size

    override val palette: List<Color> = stuccoShades + listOf(tile, tileShadow, shade, palmTrunk, palmFrond, cypress) + TreePainter.PALETTE

    override fun paintBuilding(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
        kind: Int,
    ): Int =
        when (Kind.entries[kind]) {
            Kind.ARCADE -> paintArcadeHall(g, random, left, groundY, unit)
            Kind.BELL_TOWER -> paintBellTower(g, left, groundY, unit)
            Kind.CHURCH -> paintChurch(g, left, groundY, unit)
            Kind.WING -> paintWing(g, left, groundY, unit)
        }

    override fun paintTree(
        g: Graphics2D,
        random: Random,
        centerX: Int,
        groundY: Int,
        height: Int,
    ) {
        if (random.nextFloat() < 0.6f) {
            paintPalm(
                g,
                centerX,
                groundY,
                (height * 1.25f).toInt(),
            )
        } else {
            paintCypress(g, centerX, groundY, height)
        }
    }

    private fun tileRoof(
        g: Graphics2D,
        left: Int,
        baseY: Int,
        width: Int,
        height: Int,
    ) {
        val overhang = height / 2
        CampusShapes.trapezoidRoof(g, left - overhang, baseY + height / 3, width + overhang * 2, height + height / 3, width / 6, tile)
        g.color = tileShadow
        g.fillRect(left - overhang, baseY, width + overhang * 2, (height / 3).coerceAtLeast(1))
    }

    private fun paintArcadeHall(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * (1.3f + random.nextFloat() * 0.5f)).toInt()
        val height = (unit * (0.32f + random.nextFloat() * 0.08f)).toInt()
        val top = groundY - height
        tileRoof(g, left, top, width, (unit * 0.08f).toInt().coerceAtLeast(2))
        g.color = stuccoShades[random.nextInt(stuccoShades.size)]
        g.fillRect(left, top, width, height)
        val arch = (unit * 0.09f).toInt().coerceAtLeast(3)
        val pitch = arch * 3 / 2
        var x = left + pitch / 2 + (width % pitch) / 2
        while (x + arch / 2 < left + width) {
            CampusShapes.roundArch(g, x, groundY - height / 2, arch, height / 2, shade)
            x += pitch
        }
        CampusShapes.windowGrid(g, left, top + (unit * 0.04f).toInt(), width, height / 3, pitch, (height / 3).coerceAtLeast(3)) { wx, wy ->
            g.color = shade
            g.fillRect(wx - arch / 5, wy - arch / 4, (arch * 2 / 5).coerceAtLeast(1), (arch / 2).coerceAtLeast(1))
        }
        return width
    }

    private fun paintWing(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 1.0f).toInt()
        val height = (unit * 0.44f).toInt()
        val top = groundY - height
        tileRoof(g, left, top, width, (unit * 0.09f).toInt().coerceAtLeast(2))
        g.color = stuccoShades.last()
        g.fillRect(left, top, width, height)
        val window = (unit * 0.05f).toInt().coerceAtLeast(2)
        CampusShapes.windowGrid(g, left, top, width, height, window * 3, height / 2) { x, y ->
            g.color = shade
            g.fillRect(x - window / 2, y - window, window, window * 2)
        }
        val balcony = (unit * 0.02f).toInt().coerceAtLeast(1)
        g.color = tileShadow
        g.fillRect(left + width / 8, top + height / 2 - balcony, width * 3 / 4, balcony)
        return width
    }

    private fun paintBellTower(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 0.26f).toInt().coerceAtLeast(5)
        val height = (unit * 0.78f).toInt()
        val top = groundY - height
        g.color = stuccoShades.first()
        g.fillRect(left, top, width, height)
        val stage = (unit * 0.16f).toInt()
        g.fillRect(left + width / 8, top - stage, width - width / 4, stage)
        CampusShapes.roundArch(g, left + width / 2, top - stage + stage / 5, width / 3, stage * 3 / 5, shade)
        CampusShapes.roundArch(g, left + width / 2, top + height / 10, width / 3, height / 6, shade)
        val dome = width - width / 4
        g.color = tile
        g.fillArc(left + width / 8, top - stage - dome / 2, dome, dome, 0, 180)
        return width
    }

    private fun paintChurch(
        g: Graphics2D,
        left: Int,
        groundY: Int,
        unit: Float,
    ): Int {
        val width = (unit * 0.75f).toInt()
        val height = (unit * 0.42f).toInt()
        val top = groundY - height
        tileRoof(g, left + width / 8, top, width * 3 / 4, (unit * 0.1f).toInt().coerceAtLeast(2))
        g.color = stuccoShades.last()
        g.fillRect(left, top, width, height)
        val parapet = Path2D.Float()
        parapet.moveTo(left.toDouble(), top.toDouble())
        parapet.curveTo(
            (left + width / 4).toDouble(),
            (top - unit * 0.06).toDouble(),
            (left + width / 3).toDouble(),
            (top - unit * 0.18).toDouble(),
            (left + width / 2).toDouble(),
            (top - unit * 0.2).toDouble(),
        )
        parapet.curveTo(
            (left + width * 2 / 3).toDouble(),
            (top - unit * 0.18).toDouble(),
            (left + width * 3 / 4).toDouble(),
            (top - unit * 0.06).toDouble(),
            (left + width).toDouble(),
            top.toDouble(),
        )
        parapet.closePath()
        g.fill(parapet)
        val bell = (unit * 0.06f).toInt().coerceAtLeast(2)
        CampusShapes.roundArch(g, left + width / 2, top - (unit * 0.15f).toInt(), bell, bell * 3 / 2, shade)
        CampusShapes.roundArch(g, left + width / 2, groundY - height * 3 / 5, width / 5, height * 3 / 5, shade)
        return width
    }

    private fun paintPalm(
        g: Graphics2D,
        centerX: Int,
        groundY: Int,
        height: Int,
    ) {
        val trunk = (height / 16).coerceAtLeast(1)
        g.color = palmTrunk
        g.fillRect(centerX - trunk / 2, groundY - height, trunk, height)
        g.color = palmFrond
        val frond = height / 3
        val topY = groundY - height
        listOf(-1.0 to 0.2, -0.8 to -0.25, 0.0 to -0.4, 0.8 to -0.25, 1.0 to 0.2).forEach { (dx, dy) ->
            val leaf = Path2D.Float()
            leaf.moveTo(centerX.toDouble(), topY.toDouble())
            leaf.quadTo(centerX + dx * frond * 0.6, topY + dy * frond - frond * 0.2, centerX + dx * frond, topY + dy * frond + frond * 0.25)
            leaf.quadTo(centerX + dx * frond * 0.5, topY + dy * frond * 0.5 + frond * 0.1, centerX.toDouble(), topY + frond * 0.1)
            leaf.closePath()
            g.fill(leaf)
        }
    }

    private fun paintCypress(
        g: Graphics2D,
        centerX: Int,
        groundY: Int,
        height: Int,
    ) {
        val width = (height / 4).coerceAtLeast(2)
        g.color = cypress
        g.fillOval(centerX - width / 2, groundY - height, width, height)
    }
}
