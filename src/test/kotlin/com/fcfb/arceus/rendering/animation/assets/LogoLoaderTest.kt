package com.fcfb.arceus.rendering.animation.assets

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class LogoLoaderTest {
    @TempDir
    lateinit var imagesDir: File

    private val originalPath = LogoLoader.imagesPath
    private val originalHosted = LogoLoader.hostedImagesUrl

    @AfterEach
    fun restore() {
        LogoLoader.imagesPath = originalPath
        LogoLoader.hostedImagesUrl = originalHosted
    }

    @Test
    fun `hosted image url is read from the local images directory`() {
        val logo = BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB)
        logo.createGraphics().apply {
            color = Color(0x33, 0x66, 0x99)
            fillRect(0, 0, 20, 10)
            dispose()
        }
        File(imagesDir, "team-logos").mkdirs()
        ImageIO.write(logo, "png", File(imagesDir, "team-logos/hosted-test.png"))
        LogoLoader.imagesPath = imagesDir.path
        LogoLoader.hostedImagesUrl = "http://localhost:1313/images/"

        val loaded = LogoLoader.load("http://localhost:1313/images/team-logos/hosted-test.png")

        assertNotNull(loaded)
        assertEquals(0xFF336699.toInt(), loaded!!.getRGB(0, 0))
    }
}
