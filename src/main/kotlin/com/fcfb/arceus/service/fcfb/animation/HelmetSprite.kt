package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.Logger
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
    private const val RED_MARKER_MIN = 90
    private const val RED_MARKER_DELTA = 45

    private const val LOGO_FRACTION = 0.33f
    private const val LOGO_CENTER_X = 0.332f
    private const val LOGO_CENTER_Y = 0.367f
    private const val NUMBER_FRACTION = 0.24f
    private const val NUMBER_TRACKING = 0.22f
    private const val STRIPE_EDGE_MARGIN = 0.02f
    private const val STRIPE_THICKNESS = 0.1f
    private const val SLOPE_MAX_FRACTION = 0.012f
    private const val TRIPLE_OUTER_THICKNESS = 0.03f
    private const val TRIPLE_INNER_THICKNESS = 0.055f
    private const val TRIPLE_GAP = 0.016f
    private val SHELL_GAP = java.awt.Color(0, 0, 0, 0)

    private val cache = ConcurrentHashMap<String, HelmetSprites>()

    private val template: BufferedImage? by lazy {
        try {
            HelmetSprite::class.java.getResourceAsStream(TEMPLATE)?.use { ImageIO.read(it) }
        } catch (e: IOException) {
            Logger.error("Could not read the helmet template: ${e.message}")
            null
        }
    }

    private val shellCache = ConcurrentHashMap<String, HelmetSprites>()

    fun render(
        uniform: Uniform,
        logo: BufferedImage?,
        size: Int,
    ): HelmetSprites {
        val key = cacheKey(uniform, logo, size)
        return cache.getOrPut(key) {
            val shell = shellSprites(uniform, size)
            HelmetSprites(
                withDecal(shell.facingRight, uniform, logo, size, facingRight = true),
                withDecal(shell.facingLeft, uniform, logo, size, facingRight = false),
            )
        }
    }

    /**
     * The bare recolored shell (with stripe) is the expensive part — a per-pixel recolor and downscale of the template.
     * Caching it apart from the logo means moving or resizing the logo only redraws the cheap decal on a copy.
     */
    private fun shellSprites(
        uniform: Uniform,
        size: Int,
    ): HelmetSprites {
        val key = shellKey(uniform, size)
        return shellCache.getOrPut(key) {
            val source = template ?: return@getOrPut HelmetSprites(blank(size), blank(size))
            val shell = scaleDown(recolor(source, uniform), size)
            HelmetSprites(shellWithStripe(shell, uniform, facingRight = true), shellWithStripe(shell, uniform, facingRight = false))
        }
    }

    private fun shellWithStripe(
        shell: BufferedImage,
        uniform: Uniform,
        facingRight: Boolean,
    ): BufferedImage {
        val sprite = BufferedImage(shell.width, shell.height, BufferedImage.TYPE_INT_ARGB)
        val g = sprite.createGraphics()
        g.drawImage(if (facingRight) shell else mirror(shell), 0, 0, null)
        g.dispose()
        if (uniform.stripe != null) drawCenterStripe(sprite, uniform)
        return sprite
    }

    private fun withDecal(
        baseShell: BufferedImage,
        uniform: Uniform,
        logo: BufferedImage?,
        size: Int,
        facingRight: Boolean,
    ): BufferedImage {
        val sprite = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = sprite.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.drawImage(baseShell, 0, 0, null)
        val offsetX = (if (facingRight) uniform.logoX else -uniform.logoX) * size
        val centerX = ((if (facingRight) LOGO_CENTER_X else 1f - LOGO_CENTER_X) * size + offsetX).toInt()
        val centerY = (LOGO_CENTER_Y * size + uniform.logoY * size).toInt()
        when {
            uniform.helmetLogoMode == HelmetLogoMode.NUMBERS -> drawNumberDecal(g, uniform, centerX, centerY, size)
            uniform.helmetLogoMode.drawsLogo && logo != null ->
                LogoFit.draw(g, logo, centerX, centerY, (LOGO_FRACTION * size * uniform.logoSize).toInt(), uniform.logoRotation)
        }
        g.dispose()
        return sprite
    }

    private fun blank(size: Int): BufferedImage = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

    private fun shellKey(
        uniform: Uniform,
        size: Int,
    ): String =
        listOf(
            uniform.helmet.rgb,
            uniform.facemask.rgb,
            uniform.stripe?.rgb ?: 0,
            uniform.stripeType.name,
            uniform.outerStripe?.rgb ?: 0,
            size,
        ).joinToString(":")

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
            uniform.stripeType.name,
            uniform.outerStripe?.rgb ?: 0,
            uniform.helmetNumber.rgb,
            uniform.helmetLogoMode.name,
            uniform.logoSize,
            uniform.logoX,
            uniform.logoY,
            uniform.logoRotation,
            logo?.hashCode() ?: 0,
            size,
        ).joinToString(":")

    /**
     * A center stripe that hugs the crown: it follows the shell's top contour outward from the apex and stops where the
     * silhouette starts to drop steeply (the front and back corners), so it reads as a stripe over the top rather than a
     * band wrapped around the whole outline. A triple stripe stacks an outer / inner / outer set of bands.
     */
    private fun drawCenterStripe(
        sprite: BufferedImage,
        uniform: Uniform,
    ) {
        val size = sprite.width
        val inner = uniform.stripe ?: return
        val outer = uniform.outerStripe ?: inner
        val top = IntArray(size) { topContourY(sprite, it) }
        val apex = (0 until size).filter { top[it] >= 0 }.minByOrNull { top[it] } ?: return

        val margin = (STRIPE_EDGE_MARGIN * size).toInt()
        val slopeMax = (SLOPE_MAX_FRACTION * size).coerceAtLeast(2f)
        val bands = stripeBands(uniform.stripeType, size, inner, outer)

        var left = apex
        while (left > 0 && top[left - 1] >= 0 && top[left - 1] - top[left] <= slopeMax) left--
        var right = apex
        while (right < size - 1 && top[right + 1] >= 0 && top[right + 1] - top[right] <= slopeMax) right++

        for (x in left..right) {
            var y = top[x] + margin
            for (band in bands) {
                repeat(band.thickness) {
                    if (band.color !== SHELL_GAP && y in 0 until size && (sprite.getRGB(x, y) ushr 24) >= OPAQUE) {
                        sprite.setRGB(x, y, (0xFF shl 24) or (band.color.rgb and 0xFFFFFF))
                    }
                    y++
                }
            }
        }
    }

    private fun topContourY(
        sprite: BufferedImage,
        x: Int,
    ): Int {
        for (y in 0 until sprite.height) {
            if ((sprite.getRGB(x, y) ushr 24) >= OPAQUE) return y
        }
        return -1
    }

    private fun stripeBands(
        type: StripeType,
        size: Int,
        inner: java.awt.Color,
        outer: java.awt.Color,
    ): List<StripeBand> {
        if (type == StripeType.SINGLE) return listOf(StripeBand((STRIPE_THICKNESS * size).toInt(), inner))
        val outerThickness = (TRIPLE_OUTER_THICKNESS * size).toInt().coerceAtLeast(1)
        val innerThickness = (TRIPLE_INNER_THICKNESS * size).toInt().coerceAtLeast(1)
        val gap = (TRIPLE_GAP * size).toInt().coerceAtLeast(1)
        val shell = StripeBand(gap, SHELL_GAP)
        return listOf(
            StripeBand(outerThickness, outer),
            shell,
            StripeBand(innerThickness, inner),
            shell,
            StripeBand(outerThickness, outer),
        )
    }

    private data class StripeBand(val thickness: Int, val color: java.awt.Color)

    private fun drawNumberDecal(
        g: Graphics2D,
        uniform: Uniform,
        centerX: Int,
        centerY: Int,
        size: Int,
    ) {
        val font = AnimationFonts.graduate.deriveFont((NUMBER_FRACTION * size * uniform.logoSize).coerceAtLeast(6f))
        val glyphs = font.createGlyphVector(g.fontRenderContext, NUMBER_DECAL)
        tightenTracking(glyphs)
        val bounds = glyphs.visualBounds
        val centered = AffineTransform.getTranslateInstance(centerX - bounds.centerX, centerY - bounds.centerY)
        g.color = uniform.helmetNumber
        g.fill(centered.createTransformedShape(glyphs.outline))
    }

    /** Graduate sets the digits with a lot of air; pulling each glyph toward the first tightens the decal. */
    private fun tightenTracking(glyphs: java.awt.font.GlyphVector) {
        val first = glyphs.getGlyphPosition(0)
        for (i in 1 until glyphs.numGlyphs) {
            val position = glyphs.getGlyphPosition(i)
            glyphs.setGlyphPosition(
                i,
                java.awt.geom.Point2D.Float((position.x - (position.x - first.x) * NUMBER_TRACKING).toFloat(), position.y.toFloat()),
            )
        }
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
                        red > RED_MARKER_MIN && red - green > RED_MARKER_DELTA && red - blue > RED_MARKER_DELTA -> uniform.facemask.rgb
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
