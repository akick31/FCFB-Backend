package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.Logger
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * Recolors a marker-painted helmet template. The markers are pure red for the facemask, pure green for the stripe and
 * white for the shell; every other pixel passes through, which is what keeps the shading and outlines intact.
 */
object HelmetSprite {
    const val NUMBER_DECAL = "88"

    private const val TEMPLATE = "/images/helmet-template.png"
    private const val OPAQUE = 128
    private const val MARKER_HIGH = 200
    private const val MARKER_LOW = 60

    private const val LOGO_FRACTION = 0.389f
    private const val LOGO_CENTER_X = 0.332f
    private const val LOGO_CENTER_Y = 0.367f
    private const val NUMBER_FRACTION = 0.24f

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
        uniform: Uniform,
        logo: BufferedImage?,
        size: Int,
    ): HelmetSprites {
        val key = cacheKey(uniform, logo, size)
        return cache.getOrPut(key) {
            HelmetSprites(
                draw(uniform, logo, size, facingRight = true),
                draw(uniform, logo, size, facingRight = false),
            )
        }
    }

    /** Every input that changes a pixel belongs here: two teams sharing a shell color would otherwise share a sprite. */
    private fun cacheKey(
        uniform: Uniform,
        logo: BufferedImage?,
        size: Int,
    ): String =
        listOf(
            uniform.helmet.rgb,
            uniform.facemask.rgb,
            uniform.stripe?.rgb ?: 0,
            uniform.helmetNumber.rgb,
            uniform.helmetLogoMode.name,
            logo?.hashCode() ?: 0,
            size,
        ).joinToString(":")

    private fun draw(
        uniform: Uniform,
        logo: BufferedImage?,
        size: Int,
        facingRight: Boolean,
    ): BufferedImage {
        val sprite = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val source = template ?: return sprite
        val g = sprite.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        val shell = scaleDown(recolor(source, uniform), size)
        g.drawImage(if (facingRight) shell else mirror(shell), 0, 0, null)
        val centerX = ((if (facingRight) LOGO_CENTER_X else 1f - LOGO_CENTER_X) * size).toInt()
        val centerY = (LOGO_CENTER_Y * size).toInt()
        when {
            uniform.helmetLogoMode == HelmetLogoMode.NUMBERS -> drawNumberDecal(g, uniform, centerX, centerY, size)
            uniform.helmetLogoMode.drawsLogo && logo != null ->
                LogoFit.draw(g, logo, centerX, centerY, (LOGO_FRACTION * size).toInt())
        }
        g.dispose()
        return sprite
    }

    /** A true block face would have to be a bundled TTF registered through [Font.createFont]; the JVM only guarantees its own. */
    private fun drawNumberDecal(
        g: Graphics2D,
        uniform: Uniform,
        centerX: Int,
        centerY: Int,
        size: Int,
    ) {
        val font = Font(Font.SANS_SERIF, Font.BOLD, (NUMBER_FRACTION * size).toInt().coerceAtLeast(6))
        val glyphs = font.createGlyphVector(g.fontRenderContext, NUMBER_DECAL)
        val bounds = glyphs.visualBounds
        val centered = AffineTransform.getTranslateInstance(centerX - bounds.centerX, centerY - bounds.centerY)
        g.color = uniform.helmetNumber
        g.fill(centered.createTransformedShape(glyphs.outline))
    }

    /** A team with no stripe gets the shell color in the stripe region, so the marker simply disappears. */
    private fun recolor(
        source: BufferedImage,
        uniform: Uniform,
    ): BufferedImage {
        val recolored = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
        val stripe = uniform.stripe ?: uniform.helmet
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
                        red > MARKER_HIGH && green < MARKER_LOW && blue < MARKER_LOW -> uniform.facemask.rgb
                        green > MARKER_HIGH && red < MARKER_LOW && blue < MARKER_LOW -> stripe.rgb
                        red > MARKER_HIGH && green > MARKER_HIGH && blue > MARKER_HIGH -> uniform.helmet.rgb
                        else -> argb
                    }
                recolored.setRGB(x, y, (alpha shl 24) or (replacement and 0xFFFFFF))
            }
        }
        return recolored
    }

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
