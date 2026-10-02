package com.fcfb.arceus.service.fcfb.animation

import java.awt.BasicStroke
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
    private const val MASK_BAR_WIDTH = 1.2f
    private const val FACE_HALF_WIDTH = 0.78f
    private const val FACE_TOP = -0.40f
    private const val FACE_BOTTOM = 0.98f
    private const val MASK_TOP = 0.10f
    private const val MASK_MID = 0.30f
    private const val MASK_BOTTOM = 0.74f
    private const val BAR_REACH = 0.95f
    private const val SIDE_BAR_X = 0.80f
    private const val CENTER_BAR_X = 0.26f
    private const val BUMPER_Y = -0.58f
    private const val BUMPER_WIDTH = 0.22f
    private const val BUMPER_HEIGHT = 0.085f
    private val BUMPER_COLOR = java.awt.Color(0xDD, 0xDD, 0xDD)
    private const val EYE_Y = -0.14f
    private const val EYE_X = 0.30f
    private const val EYE_SIZE = 0.16f
    private val EYE_COLOR = java.awt.Color(0x2A, 0x20, 0x18)
    private const val PROFILE_FRONT = 1.02f
    private val LIGHT_SKIN = java.awt.Color(0xE0, 0xB8, 0x98)
    private val DARK_SKIN = java.awt.Color(0x6B, 0x4A, 0x33)
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

    /**
     * A caged facemask over a recessed face opening, rather than a few loose lines. Head-on it is a grid of bars across
     * the lower face; in profile it is a short cage bowing out past the front of the shell.
     */
    private fun drawFacemask(
        g: Graphics2D,
        figure: FieldGoalFigure,
        centerY: Float,
        radius: Float,
    ) {
        val x = figure.x
        val clip = g.clip
        if (figure.facingCamera) {
            drawFaceOpening(g, figure, x, centerY, radius)
            drawEyes(g, x, centerY, radius)
            drawBumper(g, figure, x, centerY, radius)
            drawCage(g, figure, x, centerY, radius)
        } else {
            drawProfileCage(g, figure, x, centerY, radius)
        }
        g.clip = clip
    }

    /** A plain skin-tone face sits behind the mask; the tone is light or dark, fixed per player by their number. */
    private fun drawFaceOpening(
        g: Graphics2D,
        figure: FieldGoalFigure,
        x: Float,
        centerY: Float,
        radius: Float,
    ) {
        val shell = Ellipse2D.Float(x - radius, centerY - radius, radius * 2, radius * 2)
        g.clip(shell)
        g.color = skinTone(figure)
        val faceWidth = radius * FACE_HALF_WIDTH * 2
        g.fill(
            RoundRectangle2D.Float(
                x - radius * FACE_HALF_WIDTH,
                centerY + radius * FACE_TOP,
                faceWidth,
                radius * (FACE_BOTTOM - FACE_TOP),
                faceWidth * 0.5f,
                faceWidth * 0.5f,
            ),
        )
    }

    /** Two eyes in the upper face, above the mask bars, so the opening reads as a face. */
    private fun drawEyes(
        g: Graphics2D,
        x: Float,
        centerY: Float,
        radius: Float,
    ) {
        g.color = EYE_COLOR
        val size = radius * EYE_SIZE
        val eyeY = centerY + radius * EYE_Y - size / 2
        listOf(-EYE_X, EYE_X).forEach { offset ->
            g.fill(Ellipse2D.Float(x + radius * offset - size / 2, eyeY, size, size * 0.8f))
        }
    }

    /** Three horizontal bars across the face, the way a real facemask reads head-on. */
    private fun drawCage(
        g: Graphics2D,
        figure: FieldGoalFigure,
        x: Float,
        centerY: Float,
        radius: Float,
    ) {
        g.clip(Ellipse2D.Float(x - radius, centerY - radius, radius * 2, radius * 2))
        g.color = figure.uniform.facemask
        g.stroke = BasicStroke(maxOf(1f, MASK_BAR_WIDTH * figure.scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val topY = centerY + radius * MASK_TOP
        val midY = centerY + radius * MASK_MID
        val bottomY = centerY + radius * MASK_BOTTOM
        val reach = radius * BAR_REACH
        val sideX = radius * SIDE_BAR_X
        val centerBarX = radius * CENTER_BAR_X
        listOf(topY, midY, bottomY).forEach { barY -> g.draw(Line2D.Float(x - reach, barY, x + reach, barY)) }
        listOf(-sideX, -centerBarX, centerBarX, sideX).forEach { barX ->
            g.draw(Line2D.Float(x + barX, topY, x + barX, bottomY))
        }
    }

    /** The small square bumper pad on the front of the shell, above the facemask. */
    private fun drawBumper(
        g: Graphics2D,
        figure: FieldGoalFigure,
        x: Float,
        centerY: Float,
        radius: Float,
    ) {
        g.clip(Ellipse2D.Float(x - radius, centerY - radius, radius * 2, radius * 2))
        val width = radius * BUMPER_WIDTH
        val height = radius * BUMPER_HEIGHT
        val pad =
            RoundRectangle2D.Float(
                x - width / 2,
                centerY + radius * BUMPER_Y - height / 2,
                width,
                height,
                height * 0.6f,
                height * 0.6f,
            )
        g.color = BUMPER_COLOR
        g.fill(pad)
        g.color = GoalPostScenePainter.DEFENDER_COLOR
        g.stroke = BasicStroke(maxOf(1f, OUTLINE_WIDTH * figure.scale))
        g.draw(pad)
    }

    private fun skinTone(figure: FieldGoalFigure): java.awt.Color = if (figure.number % 2 == 0) DARK_SKIN else LIGHT_SKIN

    private fun drawProfileCage(
        g: Graphics2D,
        figure: FieldGoalFigure,
        x: Float,
        centerY: Float,
        radius: Float,
    ) {
        g.color = figure.uniform.facemask
        g.stroke = BasicStroke(maxOf(1f, MASK_BAR_WIDTH * figure.scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val front = x + radius * PROFILE_FRONT
        val top = centerY + radius * FACE_TOP
        val bottom = centerY + radius * FACE_BOTTOM
        g.draw(Line2D.Float(x + radius * 0.2f, top, front, top + radius * 0.12f))
        g.draw(Line2D.Float(x + radius * 0.2f, bottom, front, bottom - radius * 0.12f))
        g.draw(Line2D.Float(front, top + radius * 0.12f, front, bottom - radius * 0.12f))
        g.draw(Line2D.Float(x + radius * 0.2f, (top + bottom) / 2f, front, (top + bottom) / 2f))
    }

    private fun drawNumber(
        g: Graphics2D,
        figure: FieldGoalFigure,
        shoulderY: Float,
    ) {
        g.font = AnimationFonts.graduate.deriveFont(java.awt.Font.BOLD, (NUMBER_SIZE * figure.scale).coerceAtLeast(6f))
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
