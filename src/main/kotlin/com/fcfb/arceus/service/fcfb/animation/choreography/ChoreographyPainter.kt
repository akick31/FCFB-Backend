package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

object ChoreographyPainter {
    private const val HEIGHT_PIXELS_PER_YARD = 5f
    private const val MARKER_FONT_SIZE = 16
    private const val SHADOW_MIN_HEIGHT = 0.5f
    private const val SHADOW_WIDTH = 14
    private const val SHADOW_HEIGHT = 7
    private const val SPIN_CYCLES = 8f
    private const val BALL_SCALE = 0.75f
    private val SHADOW_COLOR = Color(0, 0, 0, 90)

    fun paint(
        field: BufferedImage,
        camera: FieldCamera,
        choreography: Choreography,
        progress: Float,
    ): BufferedImage {
        val offsetX = camera.offsetX(progress)
        val offsetY = camera.offsetY(progress)
        val frame = BufferedImage(FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.HEIGHT, BufferedImage.TYPE_INT_RGB)
        val graphics = frame.createGraphics()
        graphics.drawImage(field, -offsetX, -offsetY, null)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        graphics.font = Font("Arial", Font.BOLD, MARKER_FONT_SIZE)
        graphics.color = Color.BLACK
        choreography.defense.forEach { drawMarker(graphics, toFrame(camera, it.at(progress), offsetX, offsetY), "O") }
        choreography.offense.forEach { drawMarker(graphics, toFrame(camera, it.at(progress), offsetX, offsetY), "X") }

        val ball = choreography.ball.at(progress)
        val (groundX, groundY) = toFrame(camera, ball.position, offsetX, offsetY)
        if (ball.height > SHADOW_MIN_HEIGHT) {
            graphics.color = SHADOW_COLOR
            graphics.fillOval(groundX - SHADOW_WIDTH / 2, groundY - SHADOW_HEIGHT / 2, SHADOW_WIDTH, SHADOW_HEIGHT)
        }
        graphics.dispose()

        val ballY = groundY - (ball.height * HEIGHT_PIXELS_PER_YARD).toInt()
        if (ball.spinning) {
            FieldBackgroundPainter.drawSpiralingBall(frame, groundX, ballY, progress * SPIN_CYCLES, BALL_SCALE)
        } else {
            FieldBackgroundPainter.drawBall(frame, groundX, ballY, BALL_SCALE)
        }
        return frame
    }

    private fun toFrame(
        camera: FieldCamera,
        point: FieldPoint,
        offsetX: Int,
        offsetY: Int,
    ): Pair<Int, Int> = (camera.fieldX(point.along) - offsetX).toInt() to (camera.fieldY(point.lateral) - offsetY).toInt()

    private fun drawMarker(
        graphics: Graphics2D,
        pixel: Pair<Int, Int>,
        label: String,
    ) {
        val metrics = graphics.fontMetrics
        graphics.drawString(label, pixel.first - metrics.stringWidth(label) / 2, pixel.second + metrics.ascent / 2 - 1)
    }
}
