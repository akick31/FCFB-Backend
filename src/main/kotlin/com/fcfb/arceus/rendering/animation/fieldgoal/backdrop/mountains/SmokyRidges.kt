package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

object SmokyRidges : MountainStyle {
    private const val STEP = 6
    private const val CANOPY_SPACING = 7

    private class Layer(
        val color: Color,
        val baseHeight: Float,
        val amplitude: Float,
        val waves: Double,
    )

    private val layers =
        listOf(
            Layer(Color(136, 160, 180), 0.74f, 0.13f, 1.4),
            Layer(Color(96, 128, 136), 0.56f, 0.11f, 1.9),
            Layer(Color(66, 104, 88), 0.38f, 0.1f, 2.5),
            Layer(Color(48, 84, 60), 0.22f, 0.08f, 3.1),
        )
    private val canopyShades = listOf(Color(58, 94, 74), Color(40, 72, 50))

    override val palette: List<Color> = layers.map { it.color } + canopyShades

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        layers.forEachIndexed { index, layer ->
            val phase = random.nextDouble() * 2 * PI
            val ridge = ridgeHeights(area, layer, phase)
            val shape = Path2D.Float()
            shape.moveTo(0.0, area.groundY.toDouble())
            ridge.forEach { (x, y) -> shape.lineTo(x.toDouble(), y.toDouble()) }
            shape.lineTo(area.width.toDouble(), area.groundY.toDouble())
            shape.closePath()
            g.color = layer.color
            g.fill(shape)
            if (index >= layers.size - 2) paintCanopy(g, area, ridge, canopyShades[index - (layers.size - 2)])
        }
    }

    private fun ridgeHeights(
        area: BackdropArea,
        layer: Layer,
        phase: Double,
    ): List<Pair<Int, Int>> =
        (0..area.width step STEP).map { x ->
            val t = x.toDouble() / area.width * 2 * PI
            val wave = sin(t * layer.waves + phase) * 0.7 + sin(t * layer.waves * 2.3 + phase * 1.7) * 0.3
            x to (area.groundY - area.height * (layer.baseHeight + layer.amplitude * wave)).toInt()
        }

    private fun paintCanopy(
        g: Graphics2D,
        area: BackdropArea,
        ridge: List<Pair<Int, Int>>,
        canopy: Color,
    ) {
        val bump = area.scaled(CANOPY_SPACING)
        g.color = canopy
        var x = 0
        while (x < area.width) {
            val y = ridge[(x / STEP).coerceAtMost(ridge.lastIndex)].second
            g.fillOval(x - bump / 2, y - bump / 3, bump, bump)
            x += bump * 2 / 3
        }
    }
}
