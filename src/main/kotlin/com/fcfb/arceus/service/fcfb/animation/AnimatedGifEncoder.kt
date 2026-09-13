package com.fcfb.arceus.service.fcfb.animation

import org.springframework.stereotype.Component
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.MemoryCacheImageOutputStream

@Component
class AnimatedGifEncoder {
    fun encode(
        frames: List<BufferedImage>,
        paletteColors: List<Color>,
        frameDelayCentiseconds: Int = 10,
        loopCount: Int = 0,
    ): ByteArray {
        require(frames.isNotEmpty()) { "Cannot encode an animated GIF with no frames" }

        val colorModel = buildColorModel(paletteColors)
        val indexedFrames = frames.map { toIndexed(it, colorModel) }
        val typeSpecifier = ImageTypeSpecifier(colorModel, colorModel.createCompatibleSampleModel(frames[0].width, frames[0].height))

        val writer = ImageIO.getImageWritersBySuffix("gif").next()
        val outputStream = ByteArrayOutputStream()
        val imageOutputStream = MemoryCacheImageOutputStream(outputStream)
        writer.output = imageOutputStream

        writer.prepareWriteSequence(null)
        indexedFrames.forEachIndexed { index, frame ->
            val frameMetadata = writer.getDefaultImageMetadata(typeSpecifier, writer.defaultWriteParam)
            configureFrameDelay(frameMetadata, frameDelayCentiseconds)
            if (index == 0) {
                configureLoopCount(frameMetadata, loopCount)
            }
            writer.writeToSequence(IIOImage(frame, null, frameMetadata), null)
        }
        writer.endWriteSequence()
        imageOutputStream.close()
        writer.dispose()

        return outputStream.toByteArray()
    }

    private fun buildColorModel(colors: List<Color>): IndexColorModel {
        val size = colors.size
        val reds = ByteArray(size) { colors[it].red.toByte() }
        val greens = ByteArray(size) { colors[it].green.toByte() }
        val blues = ByteArray(size) { colors[it].blue.toByte() }
        return IndexColorModel(8, size, reds, greens, blues)
    }

    private fun toIndexed(
        source: BufferedImage,
        colorModel: IndexColorModel,
    ): BufferedImage {
        val indexed = BufferedImage(source.width, source.height, BufferedImage.TYPE_BYTE_INDEXED, colorModel)
        val g = indexed.createGraphics()
        g.drawImage(source, 0, 0, null)
        g.dispose()
        return indexed
    }

    private fun configureFrameDelay(
        metadata: IIOMetadata,
        delayCentiseconds: Int,
    ) {
        val formatName = metadata.nativeMetadataFormatName
        val root = metadata.getAsTree(formatName) as IIOMetadataNode

        val graphicControlExtensionNode = getOrCreateChild(root, "GraphicControlExtension")
        graphicControlExtensionNode.setAttribute("disposalMethod", "none")
        graphicControlExtensionNode.setAttribute("userInputFlag", "FALSE")
        graphicControlExtensionNode.setAttribute("transparentColorFlag", "FALSE")
        graphicControlExtensionNode.setAttribute("delayTime", delayCentiseconds.toString())
        graphicControlExtensionNode.setAttribute("transparentColorIndex", "0")

        metadata.setFromTree(formatName, root)
    }

    private fun configureLoopCount(
        metadata: IIOMetadata,
        loopCount: Int,
    ) {
        val formatName = metadata.nativeMetadataFormatName
        val root = metadata.getAsTree(formatName) as IIOMetadataNode

        val applicationExtensionsNode = getOrCreateChild(root, "ApplicationExtensions")
        val applicationExtensionNode = IIOMetadataNode("ApplicationExtension")
        applicationExtensionNode.setAttribute("applicationID", "NETSCAPE")
        applicationExtensionNode.setAttribute("authenticationCode", "2.0")
        applicationExtensionNode.userObject =
            byteArrayOf(0x1, (loopCount and 0xFF).toByte(), ((loopCount shr 8) and 0xFF).toByte())
        applicationExtensionsNode.appendChild(applicationExtensionNode)

        metadata.setFromTree(formatName, root)
    }

    private fun getOrCreateChild(
        root: IIOMetadataNode,
        nodeName: String,
    ): IIOMetadataNode {
        for (i in 0 until root.length) {
            val node = root.item(i)
            if (node.nodeName == nodeName) {
                return node as IIOMetadataNode
            }
        }
        val node = IIOMetadataNode(nodeName)
        root.appendChild(node)
        return node
    }
}
