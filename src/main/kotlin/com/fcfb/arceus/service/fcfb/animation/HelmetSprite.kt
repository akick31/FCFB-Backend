package com.fcfb.arceus.service.fcfb.animation

import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.QuadCurve2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/** Clip-art side-view football helmet: a round shell with an ear hole, a jaw pad, and a gray cage facemask. */
object HelmetSprite {
    private const val LOGO_FRACTION = 0.4f
    private const val LOGO_CENTER_X = 0.4f
    private const val LOGO_CENTER_Y = 0.38f
    private const val OUTLINE_FRACTION = 0.04f
    private const val MASK_OUTLINE_FRACTION = 0.075f
    private const val MASK_FRACTION = 0.05f
    private const val STRIPE_FRACTION = 0.06f
    private val MASK_COLOR = Color(200, 200, 205)
    private val STRIPE_COLOR = Color(255, 255, 255, 150)

    fun render(
        shellColor: Color,
        logo: BufferedImage?,
        size: Int,
    ): HelmetSprites = HelmetSprites(draw(shellColor, logo, size, facingRight = true), draw(shellColor, logo, size, facingRight = false))

    private fun draw(
        shellColor: Color,
        logo: BufferedImage?,
        size: Int,
        facingRight: Boolean,
    ): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        val s = size.toFloat()
        val mirror = if (facingRight) AffineTransform() else AffineTransform(-1.0, 0.0, 0.0, 1.0, s.toDouble(), 0.0)
        val outline = GoalPostScenePainter.DEFENDER_COLOR

        val shell = mirror.createTransformedShape(shellShape(s))
        g.color = shellColor
        g.fill(shell)
        g.color = STRIPE_COLOR
        g.stroke = BasicStroke(STRIPE_FRACTION * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(mirror.createTransformedShape(QuadCurve2D.Float(0.14f * s, 0.4f * s, 0.26f * s, 0.12f * s, 0.6f * s, 0.14f * s)))
        g.color = outline
        g.stroke = BasicStroke(maxOf(1f, OUTLINE_FRACTION * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(shell)

        logo?.let {
            val centerX = (if (facingRight) LOGO_CENTER_X else 1f - LOGO_CENTER_X) * s
            LogoFit.draw(g, it, centerX.toInt(), (LOGO_CENTER_Y * s).toInt(), (LOGO_FRACTION * s).toInt())
        }

        g.color = outline
        g.fill(mirror.createTransformedShape(Ellipse2D.Float(0.5f * s, 0.52f * s, 0.1f * s, 0.1f * s)))

        val mask = mirror.createTransformedShape(facemaskShape(s))
        g.color = outline
        g.stroke = BasicStroke(MASK_OUTLINE_FRACTION * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(mask)
        g.color = MASK_COLOR
        g.stroke = BasicStroke(MASK_FRACTION * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(mask)
        g.dispose()
        return image
    }

    /** A round shell with a jaw pad blended into its lower front, the way clip-art helmets are drawn. */
    private fun shellShape(s: Float): Area =
        Area(Ellipse2D.Float(0.08f * s, 0.08f * s, 0.7f * s, 0.7f * s)).apply {
            add(Area(RoundRectangle2D.Float(0.42f * s, 0.5f * s, 0.34f * s, 0.34f * s, 0.22f * s, 0.22f * s)))
            subtract(Area(Ellipse2D.Float(0.6f * s, 0.34f * s, 0.34f * s, 0.3f * s)))
        }

    /** Two face bars and a chin bar tied together by a front upright, clipped back into the shell at the brow and jaw. */
    private fun facemaskShape(s: Float): Path2D.Float =
        Path2D.Float().apply {
            moveTo(0.62f * s, 0.44f * s)
            lineTo(0.9f * s, 0.5f * s)
            moveTo(0.66f * s, 0.6f * s)
            lineTo(0.92f * s, 0.62f * s)
            moveTo(0.68f * s, 0.74f * s)
            lineTo(0.9f * s, 0.74f * s)
            moveTo(0.9f * s, 0.48f * s)
            quadTo(0.95f * s, 0.62f * s, 0.9f * s, 0.76f * s)
            moveTo(0.62f * s, 0.44f * s)
            quadTo(0.6f * s, 0.5f * s, 0.66f * s, 0.6f * s)
        }
}
