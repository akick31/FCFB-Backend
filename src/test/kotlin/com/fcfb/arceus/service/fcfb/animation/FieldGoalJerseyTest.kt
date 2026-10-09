package com.fcfb.arceus.service.fcfb.animation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class FieldGoalJerseyTest {
    private val uniform = Uniform(Color(0x800000), Color.WHITE, Color.GRAY, Color.BLACK)

    @Test
    fun `chest text fits the jersey and does not resize or shift the number`() {
        val baseline = render(uniform, 4f)
        val labeled = render(uniform.copy(numberTopText = "A VERY LONG COLLEGE FOOTBALL NAME"), 4f)
        val shoulderY = (180 - 58) * 4
        for (x in 0 until baseline.width) {
            for (y in shoulderY + 9 * 4 until shoulderY + 30 * 4) {
                assertEquals(baseline.getRGB(x, y), labeled.getRGB(x, y))
            }
        }
        for (x in 0 until baseline.width) {
            for (y in shoulderY until shoulderY + 9 * 4) {
                if (baseline.getRGB(x, y) != labeled.getRGB(x, y)) assertTrue(kotlin.math.abs(x - baseline.width / 2) <= 12 * 4)
            }
        }
    }

    private fun render(
        kit: Uniform,
        scale: Float,
        pose: PlayerPose = PlayerPose.STANDING,
    ): BufferedImage {
        val image = BufferedImage((140 * scale).toInt(), (200 * scale).toInt(), BufferedImage.TYPE_INT_ARGB)
        FieldGoalPlayerPainter.draw(image, FieldGoalFigure(image.width / 2f, 180 * scale, scale, kit, 88, pose, true))
        return image
    }
}
