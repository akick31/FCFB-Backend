package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.mountains

import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.TreePainter
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Path2D
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

object RollingHills : MountainStyle {
    private const val STEP = 6
    private const val LONE_TREES = 6

    private class Layer(
        val color: Color,
        val baseHeight: Float,
        val amplitude: Float,
        val waves: Double,
    )

    private val layers =
        listOf(
            Layer(Color(160, 190, 140), 0.6f, 0.12f, 1.2),
            Layer(Color(134, 174, 106), 0.46f, 0.12f, 1.6),
            Layer(Color(112, 158, 84), 0.32f, 0.1f, 2.0),
            Layer(Color(92, 142, 66), 0.18f, 0.08f, 2.4),
        )

    override val palette: List<Color> = layers.map { it.color } + TreePainter.PALETTE

    override fun paint(
        g: Graphics2D,
        random: Random,
        area: BackdropArea,
    ) {
        layers.forEach { layer ->
            val phase = random.nextDouble() * 2 * PI
            val ridge =
                (0..area.width step STEP).map { x ->
                    val t = x.toDouble() / area.width * 2 * PI
                    val wave = sin(t * layer.waves + phase) * 0.75 + sin(t * layer.waves * 2.1 + phase) * 0.25
                    x to (area.groundY - area.height * (layer.baseHeight + layer.amplitude * wave)).toInt()
                }
            val shape = Path2D.Float()
            shape.moveTo(0.0, area.groundY.toDouble())
            ridge.forEach { (x, y) -> shape.lineTo(x.toDouble(), y.toDouble()) }
            shape.lineTo(area.width.toDouble(), area.groundY.toDouble())
            shape.closePath()
            g.color = layer.color
            g.fill(shape)
        }
        repeat(LONE_TREES) {
            val x = random.nextInt(area.width)
            val height = (area.height * (0.1f + random.nextFloat() * 0.08f)).toInt()
            TreePainter.deciduous(g, random, x, area.groundY - (area.height * random.nextFloat() * 0.15f).toInt(), height)
        }
    }
}
