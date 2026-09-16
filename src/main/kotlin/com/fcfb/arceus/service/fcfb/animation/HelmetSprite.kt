package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.Logger
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * Team helmets are recolored from one clip-art template. The template is painted in flat markers: white is the shell, which
 * takes the team's color, red is the facemask, which is painted white, and everything else is linework left alone.
 */
object HelmetSprite {
    private const val TEMPLATE = "/images/helmet-template.png"
    private const val OPAQUE = 128
    private const val MARKER_HIGH = 200
    private const val MARKER_LOW = 60

    /** The largest circle that fits inside the template's shell, so a logo of any shape lands centered and never overhangs. */
    private const val LOGO_FRACTION = 0.432f
    private const val LOGO_CENTER_X = 0.332f
    private const val LOGO_CENTER_Y = 0.367f

    private val cache = ConcurrentHashMap<String, HelmetSprites>()

    private val template: BufferedImage? by lazy {
        try {
            HelmetSprite::class.java.getResourceAsStream(TEMPLATE)?.use { ImageIO.read(it) }
        } catch (e: IOException) {
            Logger.error("Could not read the helmet template: ${e.message}")
            null
        }
    }

    fun render(
        shellColor: Color,
        logo: BufferedImage?,
        size: Int,
    ): HelmetSprites {
        val key = "${shellColor.rgb}:${logo?.hashCode() ?: 0}:$size"
        return cache.getOrPut(key) {
            val facingRight = draw(shellColor, logo, size, facingRight = true)
            HelmetSprites(facingRight, mirror(facingRight))
        }
    }

    private fun draw(
        shellColor: Color,
        logo: BufferedImage?,
        size: Int,
        facingRight: Boolean,
    ): BufferedImage {
        val sprite = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val source = template ?: return sprite
        val g = sprite.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(scaleDown(recolor(source, shellColor), size), 0, 0, null)
        logo?.let {
            val centerX = (if (facingRight) LOGO_CENTER_X else 1f - LOGO_CENTER_X) * size
            LogoFit.draw(g, it, centerX.toInt(), (LOGO_CENTER_Y * size).toInt(), (LOGO_FRACTION * size).toInt())
        }
        g.dispose()
        return sprite
    }

    private fun recolor(
        source: BufferedImage,
        shellColor: Color,
    ): BufferedImage {
        val recolored = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val argb = source.getRGB(x, y)
                val alpha = argb ushr 24
                if (alpha < OPAQUE) continue
                val red = (argb shr 16) and 0xFF
                val green = (argb shr 8) and 0xFF
                val blue = argb and 0xFF
                val replacement =
                    when {
                        red > MARKER_HIGH && green < MARKER_LOW && blue < MARKER_LOW -> Color.WHITE.rgb
                        red > MARKER_HIGH && green > MARKER_HIGH && blue > MARKER_HIGH -> shellColor.rgb
                        else -> argb
                    }
                recolored.setRGB(x, y, (alpha shl 24) or (replacement and 0xFFFFFF))
            }
        }
        return recolored
    }

    /** Halving repeatedly keeps the linework readable; collapsing the template to sprite size in one step smears it. */
    private fun scaleDown(
        source: BufferedImage,
        size: Int,
    ): BufferedImage {
        var current = source
        while (current.width / 2 > size) {
            val half = BufferedImage(current.width / 2, current.height / 2, BufferedImage.TYPE_INT_ARGB)
            val step = half.createGraphics()
            step.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            step.drawImage(current, 0, 0, half.width, half.height, null)
            step.dispose()
            current = half
        }
        val scaled = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(current, 0, 0, size, size, null)
        g.dispose()
        return scaled
    }

    private fun mirror(sprite: BufferedImage): BufferedImage {
        val flipped = BufferedImage(sprite.width, sprite.height, BufferedImage.TYPE_INT_ARGB)
        val g = flipped.createGraphics()
        g.drawImage(sprite, sprite.width, 0, 0, sprite.height, 0, 0, sprite.width, sprite.height, null)
        g.dispose()
        return flipped
    }
}
