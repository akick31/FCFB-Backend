package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.service.fcfb.animation.HelmetSprites
import com.fcfb.arceus.service.fcfb.animation.MatchupHelmets
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.atan2

object ChoreographyPainter {
    private const val HEIGHT_PIXELS_PER_YARD = 6f
    private const val SHADOW_MIN_HEIGHT = 0.5f
    private const val SHADOW_WIDTH = 18
    private const val SHADOW_HEIGHT = 9
    private const val SPIN_CYCLES = 16f
    private const val TUMBLE_CYCLES = 6f
    private const val BALL_SCALE = 1f
    private const val CALLOUT_FONT_SIZE = 30
    private const val CALLOUT_OUTLINE = 2
    private const val CALLOUT_RISE = 26
    private const val FACING_WINDOW = 0.03f
    private const val FACING_MIN_STRIDE = 0.15f
    private const val FLIGHT_ANGLE_WINDOW = 0.01f
    private const val MIN_FLIGHT_PIXELS = 1f
    private val SHADOW_COLOR = Color(0, 0, 0, 90)

    fun paint(
        field: BufferedImage,
        camera: FieldCamera,
        choreography: Choreography,
        progress: Float,
        helmets: MatchupHelmets,
    ): BufferedImage {
        val offsetX = camera.offsetX(progress)
        val offsetY = camera.offsetY(progress)
        val frame = BufferedImage(FieldCamera.VIEW_WIDTH, FieldCamera.VIEW_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val graphics = frame.createGraphics()
        graphics.drawImage(field, -offsetX, -offsetY, null)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val view = View(camera, offsetX, offsetY)
        choreography.defense.forEach { drawHelmet(graphics, view, it, progress, helmets.defense, !helmets.offenseFacesRight, false) }
        choreography.offense.forEachIndexed { index, track ->
            val locked = index in choreography.facingLocked
            drawHelmet(graphics, view, track, progress, helmets.offense, helmets.offenseFacesRight, locked)
        }

        val ball = choreography.ball.at(progress)
        val (groundX, groundY) = view.toFrame(ball.position)
        if (choreography.showBall && ball.height > SHADOW_MIN_HEIGHT) {
            graphics.color = SHADOW_COLOR
            graphics.fillOval(groundX - SHADOW_WIDTH / 2, groundY - SHADOW_HEIGHT / 2, SHADOW_WIDTH, SHADOW_HEIGHT)
        }
        graphics.dispose()

        val ballY = groundY - (ball.height * HEIGHT_PIXELS_PER_YARD).toInt()
        val angle = flightAngle(choreography.ball, view, progress)
        when {
            !choreography.showBall -> Unit
            ball.spinning -> FieldBackgroundPainter.drawSpiralingBall(frame, groundX, ballY, progress * SPIN_CYCLES, BALL_SCALE, angle)
            ball.tumbling -> FieldBackgroundPainter.drawTumblingBall(frame, groundX, ballY, progress * TUMBLE_CYCLES, BALL_SCALE, angle)
            else -> FieldBackgroundPainter.drawBall(frame, groundX, ballY, BALL_SCALE)
        }
        choreography.callout
            ?.takeIf { progress >= it.start && progress <= it.end }
            ?.let { drawCallout(frame, it.text, groundX, ballY - CALLOUT_RISE) }
        return frame
    }

    private fun drawHelmet(
        graphics: Graphics2D,
        view: View,
        track: Track,
        progress: Float,
        sprites: HelmetSprites,
        facesRightAtRest: Boolean,
        facingLocked: Boolean,
    ) {
        val position = track.at(progress)
        val stride = position.along - track.at(maxOf(0f, progress - FACING_WINDOW)).along
        val movingFacing = if (abs(stride) >= FACING_MIN_STRIDE) stride > 0f else facesRightAtRest
        val facingRight = if (facingLocked) facesRightAtRest else movingFacing
        val sprite = sprites.facing(facingRight)
        val (x, y) = view.toFrame(position)
        graphics.drawImage(sprite, x - sprite.width / 2, y - sprite.height / 2, null)
    }

    private fun flightAngle(
        ball: BallTrack,
        view: View,
        progress: Float,
    ): Double {
        val (fromX, fromY) = view.toScreen(ball.at(maxOf(0f, progress - FLIGHT_ANGLE_WINDOW)))
        val (toX, toY) = view.toScreen(ball.at(progress))
        val dx = toX - fromX
        val dy = toY - fromY
        return if (abs(dx) + abs(dy) < MIN_FLIGHT_PIXELS) 0.0 else atan2(dy, dx).toDouble()
    }

    private fun drawCallout(
        frame: BufferedImage,
        text: String,
        centerX: Int,
        baselineY: Int,
    ) {
        val graphics = frame.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.font = Font("Arial", Font.BOLD, CALLOUT_FONT_SIZE)
        val width = graphics.fontMetrics.stringWidth(text)
        val x = (centerX - width / 2).coerceIn(0, frame.width - width)
        val y = baselineY.coerceAtLeast(CALLOUT_FONT_SIZE)
        graphics.color = Color.BLACK
        for (dx in -CALLOUT_OUTLINE..CALLOUT_OUTLINE) {
            for (dy in -CALLOUT_OUTLINE..CALLOUT_OUTLINE) {
                graphics.drawString(text, x + dx, y + dy)
            }
        }
        graphics.color = Color.WHITE
        graphics.drawString(text, x, y)
        graphics.dispose()
    }

    private class View(
        private val camera: FieldCamera,
        private val offsetX: Int,
        private val offsetY: Int,
    ) {
        fun toFrame(point: FieldPoint): Pair<Int, Int> =
            (camera.fieldX(point.along) - offsetX).toInt() to (camera.fieldY(point.lateral) - offsetY).toInt()

        fun toScreen(ball: BallState): Pair<Float, Float> {
            val groundY = camera.fieldY(ball.position.lateral) - offsetY
            return (camera.fieldX(ball.position.along) - offsetX) to (groundY - ball.height * HEIGHT_PIXELS_PER_YARD)
        }
    }
}
