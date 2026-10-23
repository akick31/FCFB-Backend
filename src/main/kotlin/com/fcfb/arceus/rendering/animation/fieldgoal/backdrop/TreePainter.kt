package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random

object TreePainter {
    private const val TRUNK_COLOR_RGB = 0x3A2C20
    private const val DECIDUOUS_TRUNK_SHARE = 0.32f
    private const val DECIDUOUS_TRUNK_WIDTH_SHARE = 0.1f
    private const val DECIDUOUS_CANOPY_WIDTH_SHARE = 0.62f
    private const val CANOPY_BLOBS = 5
    private const val PINE_TRUNK_SHARE = 0.1f
    private const val PINE_WIDTH_SHARE = 0.38f
    private const val PINE_TIERS = 4
    private const val PINE_TIER_OVERLAP = 0.45f

    private val trunkColor = Color(TRUNK_COLOR_RGB)

    val DECIDUOUS_SHADES: List<Color> = listOf(Color(46, 92, 44), Color(58, 108, 50), Color(38, 80, 38), Color(70, 120, 56))
    val PINE_SHADES: List<Color> = listOf(Color(24, 60, 40), Color(30, 72, 46), Color(20, 52, 36))
    val PALETTE: List<Color> = DECIDUOUS_SHADES + PINE_SHADES + trunkColor

    fun deciduous(
        g: Graphics2D,
        random: Random,
        centerX: Int,
        groundY: Int,
        height: Int,
        shade: Color? = null,
    ) {
        val trunkHeight = (height * DECIDUOUS_TRUNK_SHARE).toInt()
        val trunkWidth = (height * DECIDUOUS_TRUNK_WIDTH_SHARE).toInt().coerceAtLeast(1)
        g.color = trunkColor
        g.fillRect(centerX - trunkWidth / 2, groundY - trunkHeight, trunkWidth, trunkHeight)

        val canopyHeight = height - trunkHeight / 2
        val canopyWidth = (height * DECIDUOUS_CANOPY_WIDTH_SHARE).toInt().coerceAtLeast(2)
        val canopyTop = groundY - height
        val blob = (canopyWidth * 0.62f).toInt().coerceAtLeast(2)
        g.color = shade ?: DECIDUOUS_SHADES[random.nextInt(DECIDUOUS_SHADES.size)]
        g.fillOval(centerX - canopyWidth / 2, canopyTop + canopyHeight / 4, canopyWidth, canopyHeight * 3 / 4)
        repeat(CANOPY_BLOBS) {
            val bx = centerX - canopyWidth / 2 + random.nextInt((canopyWidth - blob).coerceAtLeast(1))
            val by = canopyTop + random.nextInt((canopyHeight / 2).coerceAtLeast(1))
            g.fillOval(bx, by, blob, blob)
        }
    }

    fun pine(
        g: Graphics2D,
        random: Random,
        centerX: Int,
        groundY: Int,
        height: Int,
        shade: Color? = null,
    ) {
        val trunkHeight = (height * PINE_TRUNK_SHARE).toInt().coerceAtLeast(1)
        val trunkWidth = (height * DECIDUOUS_TRUNK_WIDTH_SHARE * 0.7f).toInt().coerceAtLeast(1)
        g.color = trunkColor
        g.fillRect(centerX - trunkWidth / 2, groundY - trunkHeight, trunkWidth, trunkHeight)

        val crownBottom = groundY - trunkHeight
        val crownHeight = height - trunkHeight
        val baseHalfWidth = height * PINE_WIDTH_SHARE / 2f
        val tierHeight = crownHeight / (PINE_TIERS - (PINE_TIERS - 1) * PINE_TIER_OVERLAP)
        g.color = shade ?: PINE_SHADES[random.nextInt(PINE_SHADES.size)]
        for (tier in 0 until PINE_TIERS) {
            val tierBottom = crownBottom - tier * tierHeight * (1f - PINE_TIER_OVERLAP)
            val tierTop = tierBottom - tierHeight
            val halfWidth = baseHalfWidth * (1f - tier.toFloat() / (PINE_TIERS + 1))
            val triangle = Path2D.Float()
            triangle.moveTo((centerX - halfWidth).toDouble(), tierBottom.toDouble())
            triangle.lineTo(centerX.toDouble(), tierTop.toDouble())
            triangle.lineTo((centerX + halfWidth).toDouble(), tierBottom.toDouble())
            triangle.closePath()
            g.fill(triangle)
        }
    }
}
