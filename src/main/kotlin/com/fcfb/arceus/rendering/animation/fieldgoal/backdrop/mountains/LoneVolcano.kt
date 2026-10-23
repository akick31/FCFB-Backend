package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random

object LoneVolcano : MountainStyle {
    private const val PEAK_X_SHARE = 0.6f
    private const val PEAK_HEIGHT = 0.92f
    private const val BASE_HALF_WIDTH = 2.7f
    private const val SNOW_HEIGHT = 0.46f
    private const val GLACIERS = 9
    private const val CLEAVERS = 5
    private const val FOOTHILL_TREE_HEIGHT = 0.1f

    private val rock = Color(118, 126, 146)
    private val rockShadow = Color(90, 98, 120)
    private val snowShadow = Color(194, 206, 224)
    private val cleaver = Color(104, 110, 128)
    private val foothills = MountainRange(Color(54, 80, 60), 0.14f, 0.24f, 0.08f, 0.13f)

    override val palette: List<Color> =
        listOf(rock, rockShadow, snowShadow, cleaver, RidgePainter.SNOW_COLOR) + foothills.palette + TreePainter.PINE_SHADES

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        val peakX = area.width * PEAK_X_SHARE
        val massif = massifShape(area, peakX)
        g.color = rock
        g.fill(massif)
        val previousClip = g.clip
        g.clip(massif)
        val shadow = shadowFlank(area, peakX)
        g.color = rockShadow
        g.fill(shadow)
        g.clip(snowfield(random, area, peakX))
        g.color = RidgePainter.SNOW_COLOR
        g.fill(massif)
        g.color = snowShadow
        g.fill(shadow)
        paintCleavers(g, random, area, peakX)
        g.clip = previousClip
        val ridge = RidgePainter.paintRange(g, random, area, foothills)
        RidgePainter.paintPinesAlong(g, random, area, ridge, FOOTHILL_TREE_HEIGHT)
    }

    private fun yAt(
        area: BackdropArea,
        share: Double,
    ): Double = area.groundY - area.height * share

    private fun massifShape(
        area: BackdropArea,
        peakX: Float,
    ): Path2D.Float {
        val h = area.height.toDouble()
        val half = h * BASE_HALF_WIDTH
        val path = Path2D.Float()
        path.moveTo(peakX - half, area.groundY.toDouble())
        path.quadTo(peakX - half * 0.45, yAt(area, 0.18), peakX - h * 0.55, yAt(area, 0.66))
        path.quadTo(peakX - h * 0.3, yAt(area, PEAK_HEIGHT - 0.04), peakX - h * 0.12, yAt(area, PEAK_HEIGHT - 0.01))
        path.lineTo(peakX + h * 0.02, yAt(area, PEAK_HEIGHT.toDouble()))
        path.lineTo(peakX + h * 0.16, yAt(area, PEAK_HEIGHT - 0.03))
        path.quadTo(peakX + h * 0.35, yAt(area, PEAK_HEIGHT - 0.08), peakX + h * 0.6, yAt(area, 0.62))
        path.quadTo(peakX + half * 0.45, yAt(area, 0.16), peakX + half, area.groundY.toDouble())
        path.closePath()
        return path
    }

    private fun shadowFlank(
        area: BackdropArea,
        peakX: Float,
    ): Path2D.Float {
        val h = area.height.toDouble()
        val flank = Path2D.Float()
        flank.moveTo(peakX + h * 0.02, yAt(area, PEAK_HEIGHT.toDouble()))
        flank.lineTo(peakX + h * BASE_HALF_WIDTH, area.groundY.toDouble())
        flank.lineTo(peakX + h * 0.3, area.groundY.toDouble())
        flank.closePath()
        return flank
    }

    private fun snowfield(
        random: Random,
        area: BackdropArea,
        peakX: Float,
    ): Path2D.Float {
        val h = area.height.toDouble()
        val left = peakX - h * 1.4
        val step = h * 2.8 / (GLACIERS * 2)
        val snow = Path2D.Float()
        snow.moveTo(left, (area.topY - area.height).toDouble())
        for (index in 0..GLACIERS * 2) {
            val tongue = if (index % 2 == 1) 0.08 + random.nextDouble() * 0.16 else 0.0
            snow.lineTo(left + step * index, yAt(area, SNOW_HEIGHT - tongue))
        }
        snow.lineTo(left + h * 2.8, (area.topY - area.height).toDouble())
        snow.closePath()
        return snow
    }

    private fun paintCleavers(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
        peakX: Float,
    ) {
        val h = area.height.toDouble()
        g.color = cleaver
        repeat(CLEAVERS) { index ->
            val startX = peakX + h * (-0.3 + 0.15 * index)
            val startY = yAt(area, PEAK_HEIGHT - 0.1)
            val endX = startX + h * (-0.5 + index * 0.25) + random.nextDouble() * h * 0.1
            val endY = yAt(area, SNOW_HEIGHT - 0.15)
            val width = (h * 0.025).coerceAtLeast(1.0)
            val streak = Path2D.Float()
            streak.moveTo(startX, startY)
            streak.lineTo(startX + width, startY)
            streak.lineTo(endX + width * 2, endY)
            streak.lineTo(endX, endY)
            streak.closePath()
            g.fill(streak)
        }
    }
}
