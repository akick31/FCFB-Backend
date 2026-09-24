package com.fcfb.arceus.service.fcfb.animation

import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

object FieldGoalPlayerPainter {
    private const val HELMET_RADIUS = 10f
    private const val TORSO_WIDTH = 30f
    private const val TORSO_HEIGHT = 32f
    private const val TORSO_CORNER = 10f
    private const val LEG_LENGTH = 26f
    private const val KNEELING_HIP_HEIGHT = 12f
    private const val HIP_HALF_WIDTH = 7f
    private const val FOOT_SPREAD = 4f
    private const val ARM_LENGTH = 24f
    private const val SHOULDER_DROP = 6f
    private const val LIMB_WIDTH = 7f
    private const val OUTLINE_WIDTH = 1.5f
    private const val NUMBER_SIZE = 18f
    private const val MASK_WIDTH = 2.2f
    private const val STRIPE_HALF_WIDTH = 0.22f
    private const val STRIPE_BAND_HEIGHT = 0.5f

    fun draw(
        image: BufferedImage,
        figure: FieldGoalFigure,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val scale = figure.scale
        val x = figure.x
        val hipY = figure.footY - (if (figure.pose == PlayerPose.KNEELING) KNEELING_HIP_HEIGHT else LEG_LENGTH) * scale
        val shoulderY = hipY - TORSO_HEIGHT * scale

        g.stroke = BasicStroke(LIMB_WIDTH * scale, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.color = figure.uniform.pants
        drawLegs(g, figure, hipY)
        g.color = figure.uniform.jersey
        drawArms(g, figure, shoulderY)

        val torso =
            RoundRectangle2D.Float(
                x - TORSO_WIDTH * scale / 2,
                shoulderY,
                TORSO_WIDTH * scale,
                TORSO_HEIGHT * scale,
                TORSO_CORNER * scale,
                TORSO_CORNER * scale,
            )
        g.color = figure.uniform.jersey
        g.fill(torso)
        g.color = GoalPostScenePainter.DEFENDER_COLOR
        g.stroke = BasicStroke(OUTLINE_WIDTH)
        g.draw(torso)
        drawNumber(g, figure, shoulderY)

        val helmetRadius = HELMET_RADIUS * scale
        val helmetCenterY = shoulderY - helmetRadius + 3 * scale
        val helmet = Ellipse2D.Float(x - helmetRadius, helmetCenterY - helmetRadius, helmetRadius * 2, helmetRadius * 2)
        g.color = figure.uniform.helmet
        g.fill(helmet)
        drawHelmetStripe(g, figure, helmet, helmetCenterY, helmetRadius)
        g.color = GoalPostScenePainter.DEFENDER_COLOR
        g.stroke = BasicStroke(OUTLINE_WIDTH)
        g.draw(helmet)
        drawFacemask(g, figure, helmetCenterY, helmetRadius)
        g.dispose()
    }

    /** Clipped to the shell so the band follows the helmet edge: down the middle head-on, over the crown side-on. */
    private fun drawHelmetStripe(
        g: Graphics2D,
        figure: FieldGoalFigure,
        helmet: Ellipse2D.Float,
        centerY: Float,
        radius: Float,
    ) {
        val stripe = figure.uniform.stripe ?: return
        val clip = g.clip
        g.clip(helmet)
        g.color = stripe
        if (figure.facingCamera) {
            val halfWidth = radius * STRIPE_HALF_WIDTH
            g.fill(Rectangle2D.Float(figure.x - halfWidth, centerY - radius, halfWidth * 2, radius * 2))
        } else {
            val bandTop = centerY - radius
            g.fill(Rectangle2D.Float(figure.x - radius, bandTop, radius * 2, radius * STRIPE_BAND_HEIGHT))
        }
        g.clip = clip
    }

    private fun drawFacemask(
        g: Graphics2D,
        figure: FieldGoalFigure,
        centerY: Float,
        radius: Float,
    ) {
        val x = figure.x
        g.color = figure.uniform.facemask
        g.stroke = BasicStroke(maxOf(1f, MASK_WIDTH * figure.scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        if (figure.facingCamera) {
            g.draw(Line2D.Float(x - radius * 0.65f, centerY + radius * 0.15f, x + radius * 0.65f, centerY + radius * 0.15f))
            g.draw(Line2D.Float(x - radius * 0.5f, centerY + radius * 0.5f, x + radius * 0.5f, centerY + radius * 0.5f))
            g.draw(Line2D.Float(x, centerY - radius * 0.05f, x, centerY + radius * 0.8f))
        } else {
            g.draw(Line2D.Float(x - radius * 0.95f, centerY + radius * 0.1f, x - radius * 0.7f, centerY + radius * 0.8f))
            g.draw(Line2D.Float(x + radius * 0.95f, centerY + radius * 0.1f, x + radius * 0.7f, centerY + radius * 0.8f))
        }
    }

    private fun drawNumber(
        g: Graphics2D,
        figure: FieldGoalFigure,
        shoulderY: Float,
    ) {
        g.font = Font("Arial", Font.BOLD, (NUMBER_SIZE * figure.scale).toInt().coerceAtLeast(6))
        val text = figure.number.toString()
        val metrics = g.fontMetrics
        val textX = figure.x - metrics.stringWidth(text) / 2f
        val textY = shoulderY + (TORSO_HEIGHT * figure.scale + metrics.ascent * 0.8f) / 2f
        figure.uniform.numberOutline?.let { outline ->
            g.color = outline
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    g.drawString(text, textX + dx, textY + dy)
                }
            }
        }
        g.color = figure.uniform.number
        g.drawString(text, textX, textY)
    }

    private fun drawLegs(
        g: Graphics2D,
        figure: FieldGoalFigure,
        hipY: Float,
    ) {
        val x = figure.x
        val scale = figure.scale
        val hip = HIP_HALF_WIDTH * scale
        val foot = (HIP_HALF_WIDTH + FOOT_SPREAD) * scale
        when (figure.pose) {
            PlayerPose.KNEELING -> {
                g.draw(Line2D.Float(x - hip, hipY, x - foot - 4 * scale, figure.footY))
                g.draw(Line2D.Float(x + hip, hipY, x + foot + 6 * scale, figure.footY))
            }
            PlayerPose.KICKING -> {
                g.draw(Line2D.Float(x - hip, hipY, x - foot, figure.footY))
                g.draw(Line2D.Float(x + hip, hipY, x + 10 * scale, hipY - 14 * scale))
            }
            else -> {
                g.draw(Line2D.Float(x - hip, hipY, x - foot, figure.footY))
                g.draw(Line2D.Float(x + hip, hipY, x + foot, figure.footY))
            }
        }
    }

    private fun drawArms(
        g: Graphics2D,
        figure: FieldGoalFigure,
        shoulderY: Float,
    ) {
        val x = figure.x
        val scale = figure.scale
        val shoulder = (TORSO_WIDTH / 2 - 3f) * scale
        val armTop = shoulderY + SHOULDER_DROP * scale
        if (figure.pose == PlayerPose.KNEELING) {
            g.draw(Line2D.Float(x - shoulder, armTop, x - 18 * scale, shoulderY + 20 * scale))
            g.draw(Line2D.Float(x + shoulder, armTop, x - 10 * scale, shoulderY + 10 * scale))
            return
        }
        val (handX, handY) =
            when (figure.pose) {
                PlayerPose.ARMS_UP -> (TORSO_WIDTH / 2 + 2f) * scale to shoulderY - ARM_LENGTH * scale
                PlayerPose.KICKING -> (TORSO_WIDTH / 2 + ARM_LENGTH) * scale to shoulderY + 2 * scale
                else -> (TORSO_WIDTH / 2 + 4f) * scale to shoulderY + ARM_LENGTH * scale
            }
        g.draw(Line2D.Float(x - shoulder, armTop, x - handX, handY))
        g.draw(Line2D.Float(x + shoulder, armTop, x + handX, handY))
    }
}
