package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random

object TownBackdrop {
    private const val SEED = 42L
    private const val TREELINE_HEIGHT = 0.3f
    private const val STEEPLE_X_SHARE = 0.17f
    private const val WATER_TOWER_X_SHARE = 0.8f
    private const val STOREFRONT_MIN_HEIGHT = 0.3f
    private const val STOREFRONT_MAX_HEIGHT = 0.5f
    private const val STOREFRONT_MIN_WIDTH = 0.42f
    private const val STOREFRONT_MAX_WIDTH = 0.7f

    private val facades =
        listOf(Color(156, 78, 58), Color(132, 66, 50), Color(222, 210, 184), Color(156, 176, 150), Color(170, 190, 204))
    private val cornice = Color(236, 230, 214)
    private val storeGlass = Color(58, 70, 84)
    private val upperWindow = Color(74, 88, 104)
    private val awnings = listOf(Color(176, 46, 42), Color(46, 104, 72), Color(46, 78, 140))
    private val churchWhite = Color(244, 242, 236)
    private val churchRoof = Color(84, 88, 96)
    private val tank = Color(178, 196, 208)
    private val tankLegs = Color(120, 130, 140)

    val PALETTE: List<Color> =
        StreetPainter.PALETTE + facades + awnings + TreePainter.PALETTE +
            listOf(cornice, storeGlass, upperWindow, churchWhite, churchRoof, tank, tankLegs)

    fun paint(
        g: Graphics2D,
        area: BackdropArea,
    ) {
        val random = Random(SEED)
        val streetLevel = if (area.behindWall) area else area.copy(groundY = StreetPainter.streetTop(area))
        paintTreeline(g, random, streetLevel)
        paintSteeple(g, streetLevel, (area.width * STEEPLE_X_SHARE).toInt())
        paintWaterTower(g, streetLevel, (area.width * WATER_TOWER_X_SHARE).toInt())
        paintMainStreet(g, random, streetLevel)
        if (!area.behindWall) StreetPainter.paint(g, area)
    }

    private fun paintTreeline(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        val height = (area.height * TREELINE_HEIGHT).toInt()
        var x = 0
        while (x < area.width) {
            TreePainter.deciduous(g, random, x, area.groundY, (height * (0.7f + random.nextFloat() * 0.4f)).toInt())
            x += (height / 3).coerceAtLeast(3)
        }
    }

    private fun paintSteeple(
        g: Graphics2D,
        area: BackdropArea,
        centerX: Int,
    ) {
        val unit = area.height.toFloat()
        val bodyWidth = (unit * 0.32f).toInt()
        val bodyHeight = (unit * 0.42f).toInt()
        val towerWidth = (unit * 0.1f).toInt().coerceAtLeast(3)
        val towerHeight = (unit * 0.62f).toInt()
        g.color = churchWhite
        g.fillRect(centerX - bodyWidth / 2, area.groundY - bodyHeight, bodyWidth, bodyHeight)
        g.fillRect(centerX - towerWidth / 2, area.groundY - towerHeight, towerWidth, towerHeight)
        g.color = churchRoof
        val roof = Path2D.Float()
        roof.moveTo((centerX - bodyWidth / 2 - 2).toDouble(), (area.groundY - bodyHeight).toDouble())
        roof.lineTo(centerX.toDouble(), (area.groundY - bodyHeight - bodyWidth / 3).toDouble())
        roof.lineTo((centerX + bodyWidth / 2 + 2).toDouble(), (area.groundY - bodyHeight).toDouble())
        roof.closePath()
        g.fill(roof)
        val spire = Path2D.Float()
        spire.moveTo((centerX - towerWidth / 2).toDouble(), (area.groundY - towerHeight).toDouble())
        spire.lineTo(centerX.toDouble(), (area.groundY - towerHeight - unit * 0.28f).toDouble())
        spire.lineTo((centerX + towerWidth / 2).toDouble(), (area.groundY - towerHeight).toDouble())
        spire.closePath()
        g.fill(spire)
    }

    private fun paintWaterTower(
        g: Graphics2D,
        area: BackdropArea,
        centerX: Int,
    ) {
        val unit = area.height.toFloat()
        val tankWidth = (unit * 0.3f).toInt()
        val tankHeight = (unit * 0.16f).toInt()
        val legHeight = (unit * 0.58f).toInt()
        val tankBottom = area.groundY - legHeight
        g.color = tankLegs
        g.drawLine(centerX - tankWidth / 3, tankBottom, centerX - tankWidth / 2, area.groundY)
        g.drawLine(centerX + tankWidth / 3, tankBottom, centerX + tankWidth / 2, area.groundY)
        g.drawLine(centerX - tankWidth / 9, tankBottom, centerX - tankWidth / 7, area.groundY)
        g.drawLine(centerX + tankWidth / 9, tankBottom, centerX + tankWidth / 7, area.groundY)
        g.drawLine(centerX - tankWidth / 2 + 2, tankBottom + legHeight / 2, centerX + tankWidth / 2 - 2, tankBottom + legHeight / 2)
        g.color = tank
        g.fillRoundRect(centerX - tankWidth / 2, tankBottom - tankHeight, tankWidth, tankHeight, tankHeight / 2, tankHeight / 2)
        g.fillArc(centerX - tankWidth / 2, tankBottom - tankHeight - tankHeight / 2, tankWidth, tankHeight, 0, 180)
    }

    private fun paintMainStreet(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        val unit = area.height.toFloat()
        var x = -random.nextInt((unit * STOREFRONT_MIN_WIDTH).toInt().coerceAtLeast(1))
        while (x < area.width) {
            val width = (unit * (STOREFRONT_MIN_WIDTH + random.nextFloat() * (STOREFRONT_MAX_WIDTH - STOREFRONT_MIN_WIDTH))).toInt()
            val height = (unit * (STOREFRONT_MIN_HEIGHT + random.nextFloat() * (STOREFRONT_MAX_HEIGHT - STOREFRONT_MIN_HEIGHT))).toInt()
            paintStorefront(g, random, x, area.groundY, width, height, unit)
            x += width
        }
    }

    private fun paintStorefront(
        g: Graphics2D,
        random: Random,
        left: Int,
        groundY: Int,
        width: Int,
        height: Int,
        unit: Float,
    ) {
        val top = groundY - height
        g.color = facades[random.nextInt(facades.size)]
        g.fillRect(left, top, width, height)
        val corniceHeight = (unit * 0.03f).toInt().coerceAtLeast(2)
        g.color = cornice
        g.fillRect(left, top, width, corniceHeight)
        g.fillRect(left, top + corniceHeight * 2, width, 1)
        val storeHeight = (unit * 0.13f).toInt()
        g.color = storeGlass
        g.fillRect(left + width / 10, groundY - storeHeight, width * 4 / 5, storeHeight)
        val awning = (unit * 0.035f).toInt().coerceAtLeast(2)
        g.color = awnings[random.nextInt(awnings.size)]
        g.fillRect(left + width / 12, groundY - storeHeight - awning, width * 5 / 6, awning)
        val windowWidth = (unit * 0.04f).toInt().coerceAtLeast(2)
        val windowHeight = (unit * 0.07f).toInt().coerceAtLeast(3)
        val floor = (unit * 0.12f).toInt().coerceAtLeast(windowHeight + 2)
        g.color = upperWindow
        var y = top + corniceHeight * 3
        while (y + windowHeight < groundY - storeHeight - awning - 2) {
            var x = left + width / 6
            while (x + windowWidth < left + width - width / 8) {
                g.fillRect(x, y, windowWidth, windowHeight)
                x += windowWidth * 2
            }
            y += floor
        }
    }
}
