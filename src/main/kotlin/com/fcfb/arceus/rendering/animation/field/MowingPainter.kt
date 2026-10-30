package com.fcfb.arceus.rendering.animation.field

import com.fcfb.arceus.enums.animation.FieldSurface
import com.fcfb.arceus.enums.animation.MowingPattern
import com.fcfb.arceus.model.animation.FieldTheme
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import kotlin.math.abs

object MowingPainter {
    private const val SHADE = 0.09f

    fun shades(turf: Color): Pair<Color, Color> = adjust(turf, SHADE) to adjust(turf, -SHADE)

    fun paintOverhead(
        g: Graphics2D,
        theme: FieldTheme,
        field: Rectangle,
        bandWidth: Float,
    ) {
        if (!striped(theme)) return
        val (light, dark) = shades(theme.turf)
        var band = 0
        var x = field.x.toFloat()
        while (x < field.x + field.width) {
            g.color = if (band % 2 == 0) light else dark
            g.fillRect(x.toInt(), field.y, minOf(bandWidth, field.x + field.width - x).toInt() + 1, field.height)
            x += bandWidth
            band++
        }
    }

    fun paintRows(
        g: Graphics2D,
        theme: FieldTheme,
        width: Int,
        bandEdges: List<Int>,
    ) {
        if (!striped(theme)) return
        val (light, dark) = shades(theme.turf)
        bandEdges.zipWithNext().forEachIndexed { band, (top, bottom) ->
            g.color = if (band % 2 == 0) light else dark
            g.fillRect(0, minOf(top, bottom), width, abs(bottom - top))
        }
    }

    private fun striped(theme: FieldTheme) = theme.surface == FieldSurface.GRASS && theme.mowingPattern == MowingPattern.STRIPES

    private fun adjust(
        color: Color,
        amount: Float,
    ): Color {
        val hsb = Color.RGBtoHSB(color.red, color.green, color.blue, null)
        return Color.getHSBColor(hsb[0], hsb[1], (hsb[2] * (1 + amount)).coerceIn(0f, 1f))
    }
}
