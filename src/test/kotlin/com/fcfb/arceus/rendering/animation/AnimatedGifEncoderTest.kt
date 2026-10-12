package com.fcfb.arceus.rendering.animation

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageWriter
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.ImageOutputStream

class AnimatedGifEncoderTest {
    private val encoder = AnimatedGifEncoder()
    private val palette = listOf(Color.RED, Color.BLUE)
    private val frames = listOf(frame(Color.RED), frame(Color.BLUE))

    @Test
    fun `encoded GIF retains pixels frame timing and loop count`() {
        val bytes = encoder.encode(frames, palette, frameDelayCentiseconds = 15, finalFrameHoldCentiseconds = 250, loopCount = 2)
        val reader = ImageIO.getImageReadersBySuffix("gif").next()
        try {
            ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
                reader.input = input
                assertEquals(2, reader.getNumImages(true))
                assertEquals(Color.RED.rgb, reader.read(0).getRGB(0, 0))
                assertEquals(Color.BLUE.rgb, reader.read(1).getRGB(0, 0))
                val firstMetadata = reader.getImageMetadata(0)
                val firstRoot = firstMetadata.getAsTree(firstMetadata.nativeMetadataFormatName) as IIOMetadataNode
                val lastMetadata = reader.getImageMetadata(1)
                val lastRoot = lastMetadata.getAsTree(lastMetadata.nativeMetadataFormatName) as IIOMetadataNode
                val firstControl = firstRoot.getElementsByTagName("GraphicControlExtension").item(0)
                val lastControl = lastRoot.getElementsByTagName("GraphicControlExtension").item(0)
                assertEquals("15", firstControl.attributes.getNamedItem("delayTime").nodeValue)
                assertEquals("250", lastControl.attributes.getNamedItem("delayTime").nodeValue)
                val loop = firstRoot.getElementsByTagName("ApplicationExtension").item(0) as IIOMetadataNode
                assertEquals(listOf<Byte>(1, 2, 0), (loop.userObject as ByteArray).toList())
            }
        } finally {
            reader.dispose()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["output", "prepare", "frame", "end"])
    fun `encoding failure closes the stream disposes the writer and propagates the failure`(stage: String) {
        val realWriter = ImageIO.getImageWritersBySuffix("gif").next()
        val metadata =
            try {
                realWriter.getDefaultImageMetadata(javax.imageio.ImageTypeSpecifier.createFromRenderedImage(frames.first()), null)
            } finally {
                realWriter.dispose()
            }
        val writer = mockk<ImageWriter>(relaxed = true)
        val stream = slot<ImageOutputStream>()
        val failure = IOException("Encoding failed at $stage")
        every { writer.output = capture(stream) } answers { if (stage == "output") throw failure }
        every { writer.getDefaultImageMetadata(any(), any()) } returns metadata
        when (stage) {
            "prepare" -> every { writer.prepareWriteSequence(any()) } throws failure
            "frame" -> every { writer.writeToSequence(any(), any()) } throws failure
            "end" -> every { writer.endWriteSequence() } throws failure
        }
        every { writer.dispose() } just Runs
        mockkStatic(ImageIO::class)
        try {
            every { ImageIO.getImageWritersBySuffix("gif") } returns listOf(writer).iterator()

            val thrown = assertThrows(IOException::class.java) { encoder.encode(frames, palette) }

            assertSame(failure, thrown)
            assertAll(
                { verify(exactly = 1) { writer.dispose() } },
                { assertThrows(IOException::class.java) { stream.captured.flush() } },
            )
        } finally {
            unmockkStatic(ImageIO::class)
        }
    }

    private fun frame(color: Color): BufferedImage =
        BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB).apply {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    setRGB(x, y, color.rgb)
                }
            }
        }
}
