package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.util.Random

object SkylineBackdrop {
    private const val SEED = 42L
    private const val MIN_SIZE = 3
    private const val MIN_GAP_SHARE = 0.25f

    private enum class Kind { GLASS, CONCRETE, BRICK, DECO }

    private class Layer(
        val minHeight: Float,
        val maxHeight: Float,
        val minWidth: Float,
        val maxWidth: Float,
        val gapShare: Float,
        val kinds: List<Kind>,
        val hazy: Boolean,
    )

    private val farLayer = Layer(0.62f, 1.0f, 0.16f, 0.3f, 0.2f, listOf(Kind.GLASS, Kind.DECO, Kind.CONCRETE), hazy = true)
    private val midLayer = Layer(0.48f, 0.84f, 0.24f, 0.4f, 0.12f, listOf(Kind.GLASS, Kind.CONCRETE, Kind.DECO), hazy = false)
    private val nearLayer = Layer(0.24f, 0.44f, 0.42f, 0.75f, 0.04f, listOf(Kind.BRICK, Kind.BRICK, Kind.CONCRETE), hazy = false)

    private val hazeShades = listOf(Color(160, 180, 198), Color(148, 168, 188), Color(170, 188, 204))
    private val glassShades = listOf(Color(88, 116, 140), Color(104, 132, 154))
    private val glassReflection = Color(150, 178, 198)
    private val mullion = Color(62, 82, 100)
    private val concreteShades = listOf(Color(178, 170, 154), Color(160, 156, 146))
    private val brickShades = listOf(Color(140, 82, 64), Color(122, 72, 58), Color(150, 96, 74))
    private val decoStone = Color(198, 188, 166)
    private val windowGlass = Color(66, 86, 104)
    private val cornice = Color(204, 194, 174)
    private val roofDark = Color(70, 72, 76)
    private val tankWood = Color(112, 84, 60)

    val PALETTE: List<Color> =
        StreetPainter.PALETTE + hazeShades + glassShades + concreteShades + brickShades +
            listOf(glassReflection, mullion, decoStone, windowGlass, cornice, roofDark, tankWood)

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
    ) {
        val random = Random(SEED)
        val streetLevel = if (area.behindWall) area else area.copy(groundY = StreetPainter.streetTop(area))
        listOf(farLayer, midLayer, nearLayer).forEach { paintLayer(g, random, streetLevel, it) }
        if (!area.behindWall) StreetPainter.paint(g, area)
    }

    private fun paintLayer(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        layer: Layer,
    ) {
        val unit = area.height.toFloat()
        var x = -random.nextInt((unit * layer.minWidth).toInt().coerceAtLeast(1))
        while (x < area.width) {
            val width = (unit * (layer.minWidth + random.nextFloat() * (layer.maxWidth - layer.minWidth))).toInt().coerceAtLeast(MIN_SIZE)
            val height = (unit * (layer.minHeight + random.nextFloat() * (layer.maxHeight - layer.minHeight))).toInt()
            val bounds = Rectangle(x, area.groundY - height, width, height)
            if (layer.hazy) {
                paintHazy(g, random, bounds)
            } else {
                paintBuilding(g, random, bounds, layer.kinds[random.nextInt(layer.kinds.size)], unit)
            }
            x += width + (unit * layer.gapShare * (MIN_GAP_SHARE + random.nextFloat())).toInt()
        }
    }

    private fun paintHazy(
        g: Graphics2D,
        random: Random,
        bounds: Rectangle,
    ) {
        g.color = hazeShades[random.nextInt(hazeShades.size)]
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        if (random.nextFloat() < 0.35f) {
            val spireWidth = (bounds.width / 10).coerceAtLeast(1)
            g.fillRect(bounds.x + bounds.width / 2 - spireWidth / 2, bounds.y - bounds.height / 8, spireWidth, bounds.height / 8)
        }
    }

    private fun paintBuilding(
        g: Graphics2D,
        random: Random,
        bounds: Rectangle,
        kind: Kind,
        unit: Float,
    ) {
        when (kind) {
            Kind.GLASS -> paintGlassTower(g, random, bounds, unit)
            Kind.CONCRETE -> paintConcrete(g, random, bounds, unit)
            Kind.BRICK -> paintBrick(g, random, bounds, unit)
            Kind.DECO -> paintDeco(g, bounds, unit)
        }
    }

    private fun paintGlassTower(
        g: Graphics2D,
        random: Random,
        bounds: Rectangle,
        unit: Float,
    ) {
        g.color = glassShades[random.nextInt(glassShades.size)]
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        g.color = glassReflection
        g.fillRect(bounds.x + bounds.width / 8, bounds.y, bounds.width / 5, bounds.height)
        g.color = mullion
        val floor = (unit * 0.035f).toInt().coerceAtLeast(MIN_SIZE)
        var y = bounds.y + floor
        while (y < bounds.y + bounds.height) {
            g.fillRect(bounds.x, y, bounds.width, 1)
            y += floor
        }
        val columns = 6
        for (column in 1 until columns) g.fillRect(bounds.x + bounds.width * column / columns, bounds.y, 1, bounds.height)
        if (random.nextBoolean()) {
            g.fillRect(bounds.x + bounds.width / 2, bounds.y - bounds.height / 10, 1.coerceAtLeast(bounds.width / 30), bounds.height / 10)
        }
    }

    private fun paintConcrete(
        g: Graphics2D,
        random: Random,
        bounds: Rectangle,
        unit: Float,
    ) {
        g.color = concreteShades[random.nextInt(concreteShades.size)]
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        paintWindowGrid(g, bounds, unit * 0.045f, unit * 0.03f, 0.55f)
        g.color = roofDark
        g.fillRect(
            bounds.x + bounds.width / 4,
            bounds.y - (unit * 0.02f).toInt(),
            bounds.width / 3,
            (unit * 0.02f).toInt().coerceAtLeast(1),
        )
    }

    private fun paintBrick(
        g: Graphics2D,
        random: Random,
        bounds: Rectangle,
        unit: Float,
    ) {
        g.color = brickShades[random.nextInt(brickShades.size)]
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        g.color = cornice
        val corniceHeight = (unit * 0.018f).toInt().coerceAtLeast(1)
        g.fillRect(bounds.x - corniceHeight / 2, bounds.y, bounds.width + corniceHeight, corniceHeight)
        paintWindowGrid(
            g,
            Rectangle(bounds.x, bounds.y + corniceHeight * 2, bounds.width, bounds.height),
            unit * 0.065f,
            unit * 0.05f,
            0.45f,
        )
        if (random.nextFloat() < 0.6f) paintWaterTank(g, bounds, unit)
    }

    private fun paintWaterTank(
        g: Graphics2D,
        bounds: Rectangle,
        unit: Float,
    ) {
        val tankWidth = (unit * 0.06f).toInt().coerceAtLeast(MIN_SIZE)
        val tankHeight = (tankWidth * 1.1f).toInt()
        val legHeight = tankHeight / 2
        val left = bounds.x + bounds.width * 2 / 3
        g.color = roofDark
        g.fillRect(left + 1, bounds.y - legHeight, 1, legHeight)
        g.fillRect(left + tankWidth - 2, bounds.y - legHeight, 1, legHeight)
        g.color = tankWood
        g.fillRect(left, bounds.y - legHeight - tankHeight, tankWidth, tankHeight)
        g.color = roofDark
        g.fillPolygon(
            intArrayOf(left - 1, left + tankWidth / 2, left + tankWidth + 1),
            intArrayOf(
                bounds.y - legHeight - tankHeight,
                bounds.y - legHeight - tankHeight - tankWidth / 3,
                bounds.y - legHeight - tankHeight,
            ),
            3,
        )
    }

    private fun paintDeco(
        g: Graphics2D,
        bounds: Rectangle,
        unit: Float,
    ) {
        g.color = decoStone
        val tiers = 3
        var top = bounds.y + bounds.height
        var inset = 0
        val tierHeight = bounds.height / tiers
        repeat(tiers) {
            top -= tierHeight
            g.fillRect(bounds.x + inset, top, bounds.width - inset * 2, tierHeight)
            inset += bounds.width / 8
        }
        val spireWidth = (bounds.width / 12).coerceAtLeast(1)
        g.fillRect(bounds.x + bounds.width / 2 - spireWidth / 2, top - bounds.height / 6, spireWidth, bounds.height / 6)
        g.color = windowGlass
        val stripeWidth = (unit * 0.012f).toInt().coerceAtLeast(1)
        val stripes = 5
        for (stripe in 1..stripes) {
            val x = bounds.x + bounds.width * stripe / (stripes + 1)
            g.fillRect(x, top + tierHeight / 4, stripeWidth, bounds.y + bounds.height - top - tierHeight / 4)
        }
    }

    private fun paintWindowGrid(
        g: Graphics2D,
        bounds: Rectangle,
        floorHeight: Float,
        columnWidth: Float,
        windowShare: Float,
    ) {
        val floor = floorHeight.toInt().coerceAtLeast(MIN_SIZE)
        val column = columnWidth.toInt().coerceAtLeast(MIN_SIZE)
        val windowWidth = (column * windowShare).toInt().coerceAtLeast(1)
        val windowHeight = (floor * 0.6f).toInt().coerceAtLeast(1)
        g.color = windowGlass
        var y = bounds.y + floor / 2
        while (y + windowHeight < bounds.y + bounds.height) {
            var x = bounds.x + (bounds.width % column) / 2 + (column - windowWidth) / 2
            while (x + windowWidth <= bounds.x + bounds.width - 1) {
                g.fillRect(x, y, windowWidth, windowHeight)
                x += column
            }
            y += floor
        }
    }
}
