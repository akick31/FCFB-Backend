package com.fcfb.arceus.service.fcfb.scorebug

import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChewBadgeOverlayTest {
    private val overlay = ChewBadgeOverlay()

    private fun solidImage(
        width: Int,
        height: Int,
        color: Color,
    ): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = color
        g.fillRect(0, 0, width, height)
        g.dispose()
        return image
    }

    private fun hasAmberPixel(image: BufferedImage): Boolean {
        for (y in image.height * 3 / 4 until image.height) {
            for (x in image.width / 2 until image.width) {
                val c = Color(image.getRGB(x, y))
                if (c.red in 200..255 && c.green in 130..190 && c.blue in 30..100) return true
            }
        }
        return false
    }

    @Test
    fun `draws an amber badge in the bottom-right and leaves the top-left untouched`() {
        val source = solidImage(180, 200, Color.WHITE)

        val result = overlay.apply(source)

        assertEquals(source.width, result.width)
        assertEquals(source.height, result.height)
        assertEquals(Color.WHITE.rgb, result.getRGB(5, 5), "Top-left corner should be untouched")
        assertTrue(hasAmberPixel(result), "Bottom-right should contain the amber chew badge")
    }

    @Test
    fun `does not mutate the source image`() {
        val source = solidImage(180, 200, Color.WHITE)

        overlay.apply(source)

        assertEquals(Color.WHITE.rgb, source.getRGB(175, 195), "Source image must be left unchanged")
    }
}
