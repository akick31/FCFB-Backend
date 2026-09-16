package com.fcfb.arceus.service.fcfb.animation

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object FieldBackgroundPainter {
    const val WIDTH = 1000
    const val HEIGHT = 440
    const val MARGIN = 80

    private const val OOB_INSET = 28
    private const val TOP_NUMBER_Y = 68
    private const val BOTTOM_NUMBER_Y = 386
    private const val DIGIT_GAP = 4
    private const val CONFERENCE_LOGO_YARD = 25
    private const val CONFERENCE_LOGO_SIZE = 48
    private const val MIDFIELD_LOGO_HEIGHT = 116f
    private const val MIDFIELD_LOGO_MAX_WIDTH = 300f
    private const val CONFERENCE_MIDFIELD_LOGO_HEIGHT = 84f
    private const val CONFERENCE_MIDFIELD_LOGO_MAX_WIDTH = 220f
    private const val OOB_STROKE_WIDTH = 6f
    private const val BACK_LINE_INSET = 3
    private const val END_ZONE_OUTLINE_WIDTH = 2
    private const val END_ZONE_TEXT_PADDING = OOB_INSET + 8 + END_ZONE_OUTLINE_WIDTH
    private const val END_ZONE_LOGO_GAP = 8
    private const val BALL_HALF_LENGTH = 12f
    private const val BALL_HALF_WIDTH = 7.5f
    private const val LACE_LENGTH_FRACTION = 0.6f
    private const val STITCH_HEIGHT_FRACTION = 0.38f
    private const val STITCHES = 2
    private const val SPIRAL_LACE_TRAVEL = 0.55f
    private const val TUMBLE_MIN_LENGTH = 0.62f
    private const val HASH_OFFSET = 48
    private const val HASH_TICK_LENGTH = 10
    private const val SIDELINE_TICK_LENGTH = 10
    private const val HASH_STROKE_WIDTH = 2f

    val TURF_COLOR: Color = Color(34, 102, 51)
    val LINE_COLOR: Color = Color.WHITE
    val BALL_COLOR: Color = Color(101, 67, 33)
    val LOS_COLOR: Color = Color(30, 90, 220)
    val FIRST_DOWN_COLOR: Color = Color(255, 215, 0)

    fun paint(
        theme: FieldTheme,
        zoom: Float = 1f,
    ): BufferedImage {
        val image = BufferedImage((WIDTH * zoom).toInt(), (HEIGHT * zoom).toInt(), BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.scale(zoom.toDouble(), zoom.toDouble())
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        g.color = theme.turf
        g.fillRect(0, 0, WIDTH, HEIGHT)

        val homeEndZone = theme.endZoneOf(theme.leftSide())
        val awayEndZone = theme.endZoneOf(theme.rightSide())
        homeEndZone.fill?.let {
            g.color = it
            g.fillRect(BACK_LINE_INSET, OOB_INSET, MARGIN - BACK_LINE_INSET, HEIGHT - 2 * OOB_INSET)
        }
        awayEndZone.fill?.let {
            g.color = it
            g.fillRect(WIDTH - MARGIN, OOB_INSET, MARGIN - BACK_LINE_INSET, HEIGHT - 2 * OOB_INSET)
        }

        val defaultStroke = g.stroke
        g.stroke = BasicStroke(OOB_STROKE_WIDTH)
        g.color = LINE_COLOR
        g.drawLine(0, OOB_INSET, WIDTH, OOB_INSET)
        g.drawLine(0, HEIGHT - OOB_INSET, WIDTH, HEIGHT - OOB_INSET)
        g.drawLine(BACK_LINE_INSET, OOB_INSET, BACK_LINE_INSET, HEIGHT - OOB_INSET)
        g.drawLine(WIDTH - BACK_LINE_INSET, OOB_INSET, WIDTH - BACK_LINE_INSET, HEIGHT - OOB_INSET)
        g.stroke = defaultStroke

        drawHashMarks(g)
        g.font = Font("Arial", Font.BOLD, 26)
        for (yard in 0..100 step 5) {
            val x = FieldCoordinateMapper.toPixelX(yard, WIDTH, MARGIN)
            g.drawLine(x, OOB_INSET, x, HEIGHT - OOB_INSET)
            if (yard == 0 || yard == 100 || yard % 10 != 0) continue
            val label = (if (yard <= 50) yard else 100 - yard).toString()
            drawSplitYardNumber(g, label, x, TOP_NUMBER_Y)
            drawSplitYardNumber(g, label, x, BOTTOM_NUMBER_Y)
        }

        val endZoneFontSize = minOf(fittedEndZoneFontSize(g, homeEndZone), fittedEndZoneFontSize(g, awayEndZone))
        drawEndZoneText(g, homeEndZone, MARGIN / 2, clockwise = false, endZoneFontSize)
        drawEndZoneText(g, awayEndZone, WIDTH - MARGIN / 2, clockwise = true, endZoneFontSize)

        val midX = FieldCoordinateMapper.toPixelX(50, WIDTH, MARGIN)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        LogoLoader.load(theme.centerLogoUrl)?.let { logo -> drawMidfieldLogo(g, logo, midX, theme.style) }
        drawConferenceLogos(g, theme)

        g.dispose()
        return image
    }

    /** Bowls paint each team's conference logo on that team's own 25, between the hash marks and the sideline, on opposite sides. */
    private fun drawConferenceLogos(
        g: Graphics2D,
        theme: FieldTheme,
    ) {
        if (theme.style != FieldStyle.BOWL) return
        val offset = HASH_OFFSET + (HEIGHT / 2 - OOB_INSET - HASH_OFFSET) / 2
        LogoLoader.load(theme.conferenceLogoOf(theme.leftSide()))?.let {
            val x = FieldCoordinateMapper.toPixelX(CONFERENCE_LOGO_YARD, WIDTH, MARGIN)
            LogoFit.draw(g, it, x, HEIGHT / 2 + offset, CONFERENCE_LOGO_SIZE)
        }
        LogoLoader.load(theme.conferenceLogoOf(theme.rightSide()))?.let {
            val x = FieldCoordinateMapper.toPixelX(100 - CONFERENCE_LOGO_YARD, WIDTH, MARGIN)
            LogoFit.draw(g, it, x, HEIGHT / 2 - offset, CONFERENCE_LOGO_SIZE)
        }
    }

    /**
     * Midfield logos run slightly past both hash marks across the field, however wide the logo is. Conference marks are
     * dense wordmarks that overpower the field at that size, so a championship logo stays inside the hashes.
     */
    private fun drawMidfieldLogo(
        g: Graphics2D,
        logo: BufferedImage,
        centerX: Int,
        style: FieldStyle,
    ) {
        val conference = style == FieldStyle.CONFERENCE_CHAMPIONSHIP
        val targetHeight = if (conference) CONFERENCE_MIDFIELD_LOGO_HEIGHT else MIDFIELD_LOGO_HEIGHT
        val maxWidth = if (conference) CONFERENCE_MIDFIELD_LOGO_MAX_WIDTH else MIDFIELD_LOGO_MAX_WIDTH
        val aspect = logo.width.toFloat() / logo.height
        val width = minOf(targetHeight * aspect, maxWidth)
        val height = width / aspect
        g.drawImage(logo, (centerX - width / 2).toInt(), (HEIGHT / 2 - height / 2).toInt(), width.toInt(), height.toInt(), null)
    }

    fun drawScrimmageLines(
        image: BufferedImage,
        losAbs: Int,
        firstDownAbs: Int,
        zoom: Float = 1f,
        flipped: Boolean = false,
    ) {
        val g = image.createGraphics()
        g.scale(zoom.toDouble(), zoom.toDouble())
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.stroke = BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)

        val sidelineHalfWidth = (OOB_STROKE_WIDTH / 2).toInt()
        val topY = OOB_INSET + sidelineHalfWidth
        val bottomY = HEIGHT - OOB_INSET - sidelineHalfWidth

        val shown = { abs: Int -> if (flipped) 100 - abs.coerceIn(0, 100) else abs.coerceIn(0, 100) }
        val losX = FieldCoordinateMapper.toPixelX(shown(losAbs), WIDTH, MARGIN)
        g.color = LOS_COLOR
        g.drawLine(losX, topY, losX, bottomY)

        val firstDownX = FieldCoordinateMapper.toPixelX(shown(firstDownAbs), WIDTH, MARGIN)
        g.color = FIRST_DOWN_COLOR
        g.drawLine(firstDownX, topY, firstDownX, bottomY)

        g.dispose()
    }

    fun drawBall(
        image: BufferedImage,
        x: Int,
        y: Int,
        scale: Float = 1f,
        angle: Double = 0.0,
    ) = drawFootball(image, x, y, scale, angle, lengthFactor = 1f, laceOffset = 0f, lacesVisible = true)

    /** A spiral keeps its nose on the flight line while the laces roll around the long axis. */
    fun drawSpiralingBall(
        image: BufferedImage,
        x: Int,
        y: Int,
        spinProgress: Float,
        scale: Float = 1f,
        angle: Double = 0.0,
    ) {
        val turn = spinProgress * 2 * PI
        drawFootball(
            image,
            x,
            y,
            scale,
            angle,
            lengthFactor = 1f,
            laceOffset = sin(turn).toFloat() * SPIRAL_LACE_TRAVEL,
            lacesVisible = cos(turn) > -0.3,
        )
    }

    /** End over end: the ball's length shrinks as its nose swings toward the viewer and grows back as it turns broadside. */
    fun drawTumblingBall(
        image: BufferedImage,
        x: Int,
        y: Int,
        tumbleProgress: Float,
        scale: Float = 1f,
        angle: Double = 0.0,
    ) {
        val turn = tumbleProgress * 2 * PI
        val length = TUMBLE_MIN_LENGTH + (1f - TUMBLE_MIN_LENGTH) * abs(cos(turn)).toFloat()
        drawFootball(image, x, y, scale, angle, lengthFactor = length, laceOffset = 0f, lacesVisible = cos(turn) > 0)
    }

    private fun drawFootball(
        image: BufferedImage,
        x: Int,
        y: Int,
        scale: Float,
        angle: Double,
        lengthFactor: Float,
        laceOffset: Float,
        lacesVisible: Boolean,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.translate(x.toDouble(), y.toDouble())
        g.rotate(angle)
        val halfLength = BALL_HALF_LENGTH * scale * lengthFactor
        val halfWidth = BALL_HALF_WIDTH * scale
        g.color = BALL_COLOR
        g.fill(Ellipse2D.Float(-halfLength, -halfWidth, halfLength * 2, halfWidth * 2))
        if (lacesVisible) {
            val seamY = laceOffset * halfWidth
            val seamHalf = halfLength * LACE_LENGTH_FRACTION
            val stitchHalf = halfWidth * STITCH_HEIGHT_FRACTION
            g.color = LINE_COLOR
            g.stroke = BasicStroke(maxOf(1.4f, scale * 1.7f))
            g.draw(Line2D.Float(-seamHalf, seamY, seamHalf, seamY))
            g.stroke = BasicStroke(maxOf(1f, scale * 1.2f))
            for (stitch in -STITCHES..STITCHES) {
                val stitchX = stitch * seamHalf / (STITCHES + 0.5f)
                g.draw(Line2D.Float(stitchX, seamY - stitchHalf, stitchX, seamY + stitchHalf))
            }
        }
        g.dispose()
    }

    fun parseColor(colorString: String?): Color {
        return try {
            Color.decode(colorString ?: "#000000")
        } catch (e: NumberFormatException) {
            Color.BLACK
        }
    }

    /** College hashes sit 60 feet in from each sideline, leaving them 40 feet apart across the middle of the field. */
    private fun drawHashMarks(g: Graphics2D) {
        val stroke = g.stroke
        g.stroke = BasicStroke(HASH_STROKE_WIDTH)
        val centerY = HEIGHT / 2
        val halfTick = HASH_TICK_LENGTH / 2
        for (yard in 1 until 100) {
            if (yard % 5 == 0) continue
            val x = FieldCoordinateMapper.toPixelX(yard, WIDTH, MARGIN)
            g.drawLine(x, centerY - HASH_OFFSET - halfTick, x, centerY - HASH_OFFSET + halfTick)
            g.drawLine(x, centerY + HASH_OFFSET - halfTick, x, centerY + HASH_OFFSET + halfTick)
            g.drawLine(x, OOB_INSET, x, OOB_INSET + SIDELINE_TICK_LENGTH)
            g.drawLine(x, HEIGHT - OOB_INSET - SIDELINE_TICK_LENGTH, x, HEIGHT - OOB_INSET)
        }
        g.stroke = stroke
    }

    private fun drawSplitYardNumber(
        g: Graphics2D,
        label: String,
        lineX: Int,
        y: Int,
    ) {
        val left = label.substring(0, 1)
        val right = label.substring(1)
        val leftWidth = g.fontMetrics.stringWidth(left)
        g.drawString(left, lineX - leftWidth - DIGIT_GAP, y)
        g.drawString(right, lineX + DIGIT_GAP, y)
    }

    private fun drawEndZoneText(
        g: Graphics2D,
        endZone: EndZoneDecoration,
        centerX: Int,
        clockwise: Boolean,
        fontSize: Int,
    ) {
        val label = endZone.team.name?.uppercase()?.takeIf { it.isNotBlank() } ?: return
        val logo = LogoLoader.load(endZone.logoUrl)
        g.font = Font("Arial", Font.BOLD, fontSize)
        val metrics = g.fontMetrics
        val logoSize = metrics.ascent
        val start = -endZoneLength(g, label, logo) / 2
        val textX = start + if (logo != null) logoSize + END_ZONE_LOGO_GAP else 0

        val transform = g.transform
        g.translate(centerX, HEIGHT / 2)
        g.rotate(if (clockwise) PI / 2 else -PI / 2)
        logo?.let {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            LogoFit.draw(g, it, start + logoSize / 2, 0, logoSize)
        }
        g.color = endZone.outlineColor
        for (dx in -END_ZONE_OUTLINE_WIDTH..END_ZONE_OUTLINE_WIDTH) {
            for (dy in -END_ZONE_OUTLINE_WIDTH..END_ZONE_OUTLINE_WIDTH) {
                if (dx != 0 || dy != 0) {
                    g.drawString(label, textX + dx, metrics.ascent / 2 + dy)
                }
            }
        }
        g.color = endZone.textColor
        g.drawString(label, textX, metrics.ascent / 2)
        g.transform = transform
    }

    /** Both end zones share the smaller of their two fitted sizes so the names, and the playoff logos beside them, match. */
    private fun fittedEndZoneFontSize(
        g: Graphics2D,
        endZone: EndZoneDecoration,
    ): Int {
        var fontSize = MARGIN - 16
        val label = endZone.team.name?.uppercase()?.takeIf { it.isNotBlank() } ?: return fontSize
        val logo = LogoLoader.load(endZone.logoUrl)
        val availableHeight = (HEIGHT - 2 * END_ZONE_TEXT_PADDING).toFloat() - 2 * END_ZONE_OUTLINE_WIDTH
        g.font = Font("Arial", Font.BOLD, fontSize)
        while (fontSize > 6 && endZoneLength(g, label, logo) > availableHeight) {
            fontSize--
            g.font = Font("Arial", Font.BOLD, fontSize)
        }
        return fontSize
    }

    /** The playoff logo sits in line with the team name at the same height as the lettering, so both count toward the fit. */
    private fun endZoneLength(
        g: Graphics2D,
        label: String,
        logo: BufferedImage?,
    ): Int = g.fontMetrics.stringWidth(label) + if (logo != null) g.fontMetrics.ascent + END_ZONE_LOGO_GAP else 0
}
