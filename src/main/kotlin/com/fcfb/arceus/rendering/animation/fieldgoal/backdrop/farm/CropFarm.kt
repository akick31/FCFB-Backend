package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.farm

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.util.Random
import kotlin.math.pow

object CropFarm : FarmStyle {
    private const val HORIZON_HEIGHT = 0.6f
    private const val VANISHING_LIFT = 0.8f
    private const val STRIP_COUNT = 4
    private const val PERSPECTIVE_EXPONENT = 1.7
    private const val ROW_SPACING = 16
    private const val MAX_ROW_STROKE = 3f
    private const val BARN_X_SHARE = 0.16f
    private const val FENCE_HEIGHT_SHARE = 0.32f
    private const val CORN_TIP_HEIGHT_SHARE = 0.35f
    private const val CORN_TIP_WIDTH = 5

    private enum class Crop(
        val base: Color,
        val row: Color,
    ) {
        WHEAT(Color(214, 178, 92), Color(184, 146, 66)),
        CORN(Color(78, 128, 52), Color(52, 98, 38)),
        SOIL(Color(122, 90, 62), Color(96, 70, 46)),
    }

    private val stripCrops = listOf(Crop.WHEAT, Crop.SOIL, Crop.WHEAT, Crop.CORN)
    private val fenceColor = Color(118, 98, 76)

    override val palette: List<Color> = Crop.entries.flatMap { listOf(it.base, it.row) } + fenceColor + FarmScenery.PALETTE

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        val horizonY = area.yAt(HORIZON_HEIGHT)
        FarmScenery.treeline(g, random, area, horizonY)
        FarmScenery.barn(g, area, (area.width * BARN_X_SHARE).toInt(), horizonY + 2, withSilo = true)
        val boundaries =
            (0..STRIP_COUNT).map { index ->
                horizonY + ((area.groundY - horizonY) * (index.toDouble() / STRIP_COUNT).pow(PERSPECTIVE_EXPONENT)).toInt()
            }
        val vanishingY = horizonY - area.height * VANISHING_LIFT
        stripCrops.forEachIndexed { index, crop ->
            paintStrip(g, area, crop, boundaries[index], boundaries[index + 1], area.width / 2f, vanishingY)
        }
        val fenceGround = boundaries[STRIP_COUNT - 1]
        if (!area.behindWall) {
            FarmScenery.fence(
                g,
                area,
                fenceGround,
                ((area.groundY - fenceGround) * FENCE_HEIGHT_SHARE).toInt().coerceAtLeast(3),
                fenceColor,
            )
        }
    }

    private fun paintStrip(
        g: Graphics2D,
        area: BackdropArea,
        crop: Crop,
        top: Int,
        bottom: Int,
        vanishingX: Float,
        vanishingY: Float,
    ) {
        if (bottom <= top) return
        g.color = crop.base
        g.fillRect(0, top, area.width, bottom - top)
        val previousClip = g.clip
        val previousStroke = g.stroke
        g.clipRect(0, top, area.width, bottom - top)
        val nearness = (bottom - area.groundY + area.height).toFloat() / area.height
        g.stroke = BasicStroke((MAX_ROW_STROKE * area.scale * nearness).coerceAtLeast(1f))
        g.color = crop.row
        val spacing = area.scaled(ROW_SPACING)
        var x = -area.width
        while (x < area.width * 2) {
            g.draw(Line2D.Float(x.toFloat(), area.groundY.toFloat(), vanishingX, vanishingY))
            x += spacing
        }
        g.stroke = previousStroke
        g.clip = previousClip
        if (crop == Crop.CORN) paintCornTips(g, area, top, bottom)
    }

    private fun paintCornTips(
        g: Graphics2D,
        area: BackdropArea,
        top: Int,
        bottom: Int,
    ) {
        val tipHeight = ((bottom - top) * CORN_TIP_HEIGHT_SHARE).toInt().coerceAtLeast(2)
        val tipWidth = area.scaled(CORN_TIP_WIDTH)
        g.color = Crop.CORN.row
        var x = 0
        while (x < area.width) {
            val tip = Path2D.Float()
            tip.moveTo(x.toDouble(), top.toDouble())
            tip.lineTo((x + tipWidth / 2).toDouble(), (top - tipHeight).toDouble())
            tip.lineTo((x + tipWidth).toDouble(), top.toDouble())
            tip.closePath()
            g.fill(tip)
            x += tipWidth
        }
    }
}
