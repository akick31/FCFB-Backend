package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.util.BoundedCache
import com.fcfb.arceus.util.Logger
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Recolors a marker-painted helmet template. The markers are pure red for the facemask, pure green for the stripe and
 * white for the shell; every other pixel passes through, which is what keeps the shading and outlines intact.
 */
object HelmetSprite {
    const val NUMBER_DECAL = "88"

    private const val TEMPLATE = "/images/helmet-template.png"
    private const val OPAQUE = 128
    private const val CACHE_SIZE = 512
    private const val MARKER_HIGH = 200
    private const val MARKER_LOW = 60
    private const val RED_MARKER_MIN = 90
    private const val RED_MARKER_DELTA = 45

    private const val LOGO_FRACTION = 0.33f
    private const val LOGO_CENTER_X = 0.332f
    private const val LOGO_CENTER_Y = 0.367f
    private const val NUMBER_FRACTION = 0.24f
    private const val NUMBER_TRACKING = 0.22f
    private const val STRIPE_EDGE_MARGIN = -0.006f
    private const val STRIPE_THICKNESS = 0.04f
    private const val TAPER_POWER = 1.3
    private const val STRIPE_SPAN_DROP_BACK = 0.76f
    private const val STRIPE_SPAN_DROP_FRONT = 0.24f
    private const val BACK_EDGE_JUMP = 0.055f
    private const val NORMAL_PROBE = 3f
    private const val TRIPLE_OUTER_THICKNESS = 0.016f
    private const val TRIPLE_INNER_THICKNESS = 0.03f
    private const val TRIPLE_GAP = 0.01f
    private const val MIN_CONTOUR_COLUMNS = 8

    private val cache = BoundedCache<String, HelmetSprites>(CACHE_SIZE)

    private val template: BufferedImage? by lazy {
        try {
            HelmetSprite::class.java.getResourceAsStream(TEMPLATE)?.use { ImageIO.read(it) }
        } catch (e: IOException) {
            Logger.error("Could not read the helmet template: ${e.message}")
            null
        }
    }

    private val shellCache = BoundedCache<String, HelmetSprites>(CACHE_SIZE)

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
     * A center stripe that hugs the shell's silhouette: the outer edge is walked from the front of the dome, over the
     * crown, and down the back edge of the shell, with a band of constant perpendicular width laid just inside it. The
     * front stops short of the facemask; the back runs long. A triple stripe stacks the center band and one outer band
     * (the crown-side band is implied by the shell color).
     */
    private fun drawCenterStripe(
        sprite: BufferedImage,
        uniform: Uniform,
    ) {
        val inner = uniform.stripe ?: return
        val outer = uniform.outerStripe ?: inner
        val size = sprite.width
        val top = IntArray(size) { topContourY(sprite, it) }
        val valid = (0 until size).filter { top[it] >= 0 }
        if (valid.size < MIN_CONTOUR_COLUMNS) return
        val apex = valid.minByOrNull { top[it] } ?: return
        val path = stripeEdgePath(sprite, top, valid, apex)
        if (path.size < 2) return
        val margin = STRIPE_EDGE_MARGIN * size
        val bands = stripeBands(uniform, inner, outer, size)
        val normals = path.indices.map { inwardNormal(sprite, path, it) }
        val apexIndex = path.indices.minByOrNull { path[it][1] } ?: 0

        val layer = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = layer.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        for (band in bands) {
            g.color = band.color
            g.fill(edgeBand(path, normals, margin, margin + band.offset, margin + band.offset + band.thickness, apexIndex))
        }
        g.dispose()
        compositeOnShell(sprite, layer)
    }

    /** Full width from the front to the crown, then fades to a point down the back so the stripe reads as receding in perspective. */
    private fun backTaper(
        i: Int,
        apexIndex: Int,
        last: Int,
    ): Float {
        if (i <= apexIndex || last <= apexIndex) return 1f
        val ratio = ((i - apexIndex).toFloat() / (last - apexIndex)).coerceIn(0f, 1f)
        return Math.pow((1f - ratio).toDouble(), TAPER_POWER).toFloat()
    }

    private fun stripeEdgePath(
        sprite: BufferedImage,
        top: IntArray,
        valid: List<Int>,
        apex: Int,
    ): List<FloatArray> {
        val size = sprite.width
        val apexY = top[apex]
        val minX = valid.first()
        var rightX = apex
        for (x in apex + 1..valid.last()) {
            if (top[x] < 0 || top[x] - apexY > STRIPE_SPAN_DROP_FRONT * size) break
            rightX = x
        }
        val points = ArrayList<FloatArray>()
        for (x in rightX downTo minX) {
            if (top[x] >= 0) points.add(floatArrayOf(x.toFloat(), top[x].toFloat()))
        }
        var prevX = minX
        var y = top[minX] + 1
        while (y < sprite.height && y - apexY <= STRIPE_SPAN_DROP_BACK * size) {
            val lx = leftmostOpaque(sprite, y)
            if (lx < 0 || lx - prevX > BACK_EDGE_JUMP * size) break
            points.add(floatArrayOf(lx.toFloat(), y.toFloat()))
            prevX = lx
            y++
        }
        return points
    }

    private fun leftmostOpaque(
        sprite: BufferedImage,
        y: Int,
    ): Int {
        for (x in 0 until sprite.width) {
            if ((sprite.getRGB(x, y) ushr 24) >= OPAQUE) return x
        }
        return -1
    }

    private fun edgeBand(
        path: List<FloatArray>,
        normals: List<FloatArray>,
        base: Float,
        from: Float,
        to: Float,
        apexIndex: Int,
    ): Path2D.Float {
        val p = Path2D.Float()
        val last = path.size - 1
        path.forEachIndexed { i, pt ->
            val n = normals[i]
            val f = base + (from - base) * backTaper(i, apexIndex, last)
            if (i == 0) p.moveTo(pt[0] + n[0] * f, pt[1] + n[1] * f) else p.lineTo(pt[0] + n[0] * f, pt[1] + n[1] * f)
        }
        for (i in path.indices.reversed()) {
            val pt = path[i]
            val n = normals[i]
            val t = base + (to - base) * backTaper(i, apexIndex, last)
            p.lineTo(pt[0] + n[0] * t, pt[1] + n[1] * t)
        }
        p.closePath()
        return p
    }

    /** Perpendicular to the local edge direction, pointing into the shell (chosen by sampling an opaque pixel). */
    private fun inwardNormal(
        sprite: BufferedImage,
        path: List<FloatArray>,
        i: Int,
    ): FloatArray {
        val a = path[(i - 2).coerceAtLeast(0)]
        val b = path[(i + 2).coerceAtMost(path.size - 1)]
        val tx = b[0] - a[0]
        val ty = b[1] - a[1]
        val len = Math.hypot(tx.toDouble(), ty.toDouble()).toFloat()
        if (len < 1e-3f) return floatArrayOf(0f, 1f)
        val nx = -ty / len
        val ny = tx / len
        val px = path[i][0] + nx * NORMAL_PROBE
        val py = path[i][1] + ny * NORMAL_PROBE
        return if (isOpaqueAt(sprite, px, py)) floatArrayOf(nx, ny) else floatArrayOf(-nx, -ny)
    }

    private fun isOpaqueAt(
        sprite: BufferedImage,
        x: Float,
        y: Float,
    ): Boolean {
        val xi = x.toInt()
        val yi = y.toInt()
        if (xi < 0 || yi < 0 || xi >= sprite.width || yi >= sprite.height) return false
        return (sprite.getRGB(xi, yi) ushr 24) >= OPAQUE
    }

    private fun stripeBands(
        uniform: Uniform,
        inner: java.awt.Color,
        outer: java.awt.Color,
        size: Int,
    ): List<StripeBand> {
        if (uniform.stripeType == StripeType.SINGLE) {
            return listOf(StripeBand(0f, STRIPE_THICKNESS * size, inner))
        }
        val outerT = TRIPLE_OUTER_THICKNESS * size
        val innerT = TRIPLE_INNER_THICKNESS * size
        val gap = if (uniform.stripeType == StripeType.TRIPLE_FLUSH) 0f else TRIPLE_GAP * size
        val list = mutableListOf<StripeBand>()
        list.add(StripeBand(0f, innerT, inner))
        list.add(StripeBand(innerT + gap, outerT, outer))
        return list
    }

    private data class StripeBand(val offset: Float, val thickness: Float, val color: java.awt.Color)

    /** Lays the stamped stripe onto the shell, only over opaque shell pixels, so it stops cleanly at the edges. */
    private fun compositeOnShell(
        sprite: BufferedImage,
        layer: BufferedImage,
    ) {
        for (y in 0 until sprite.height) {
            for (x in 0 until sprite.width) {
                val lp = layer.getRGB(x, y)
                val la = (lp ushr 24) and 0xFF
                if (la == 0) continue
                val sp = sprite.getRGB(x, y)
                if ((sp ushr 24) and 0xFF < OPAQUE) continue
                val a = la / 255f
                val r = (((lp shr 16) and 0xFF) * a + ((sp shr 16) and 0xFF) * (1 - a)).toInt()
                val gg = (((lp shr 8) and 0xFF) * a + ((sp shr 8) and 0xFF) * (1 - a)).toInt()
                val b = ((lp and 0xFF) * a + (sp and 0xFF) * (1 - a)).toInt()
                sprite.setRGB(x, y, (0xFF shl 24) or (r shl 16) or (gg shl 8) or b)
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

    private fun drawNumberDecal(
        g: Graphics2D,
        uniform: Uniform,
        centerX: Int,
        centerY: Int,
        size: Int,
    ) {
        val font = NumberFonts.font(uniform.helmetNumberFont, (NUMBER_FRACTION * size * uniform.logoSize).coerceAtLeast(6f))
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
                if (alpha == 0) continue
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
