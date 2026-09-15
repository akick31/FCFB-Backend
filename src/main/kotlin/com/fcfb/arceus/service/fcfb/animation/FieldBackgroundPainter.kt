package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import com.fcfb.arceus.util.Logger
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.PI
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
    private const val LOGO_SIZE = 114
    private const val OOB_STROKE_WIDTH = 6f
    private const val BACK_LINE_INSET = 3
    private const val END_ZONE_OUTLINE_WIDTH = 2
    private const val END_ZONE_TEXT_PADDING = OOB_INSET + 8 + END_ZONE_OUTLINE_WIDTH

    val TURF_COLOR: Color = Color(34, 102, 51)
    val LINE_COLOR: Color = Color.WHITE
    val BALL_COLOR: Color = Color(101, 67, 33)
    val LOS_COLOR: Color = Color(30, 90, 220)
    val FIRST_DOWN_COLOR: Color = Color(255, 215, 0)

    private val logoCache = ConcurrentHashMap<String, BufferedImage>()

    fun paint(
        homeTeam: Team,
        awayTeam: Team,
        zoom: Float = 1f,
    ): BufferedImage {
        val image = BufferedImage((WIDTH * zoom).toInt(), (HEIGHT * zoom).toInt(), BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.scale(zoom.toDouble(), zoom.toDouble())
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        g.color = TURF_COLOR
        g.fillRect(0, 0, WIDTH, HEIGHT)

        val endZoneColor = parseColor(homeTeam.primaryColor)
        g.color = endZoneColor
        g.fillRect(BACK_LINE_INSET, OOB_INSET, MARGIN - BACK_LINE_INSET, HEIGHT - 2 * OOB_INSET)
        g.fillRect(WIDTH - MARGIN, OOB_INSET, MARGIN - BACK_LINE_INSET, HEIGHT - 2 * OOB_INSET)

        val defaultStroke = g.stroke
        g.stroke = BasicStroke(OOB_STROKE_WIDTH)
        g.color = LINE_COLOR
        g.drawLine(0, OOB_INSET, WIDTH, OOB_INSET)
        g.drawLine(0, HEIGHT - OOB_INSET, WIDTH, HEIGHT - OOB_INSET)
        g.drawLine(BACK_LINE_INSET, OOB_INSET, BACK_LINE_INSET, HEIGHT - OOB_INSET)
        g.drawLine(WIDTH - BACK_LINE_INSET, OOB_INSET, WIDTH - BACK_LINE_INSET, HEIGHT - OOB_INSET)
        g.stroke = defaultStroke

        g.font = Font("Arial", Font.BOLD, 26)
        for (yard in 0..100 step 5) {
            val x = FieldCoordinateMapper.toPixelX(yard, WIDTH, MARGIN)
            g.drawLine(x, OOB_INSET, x, HEIGHT - OOB_INSET)
            if (yard == 0 || yard == 100 || yard % 10 != 0) continue
            val label = (if (yard <= 50) yard else 100 - yard).toString()
            drawSplitYardNumber(g, label, x, TOP_NUMBER_Y)
            drawSplitYardNumber(g, label, x, BOTTOM_NUMBER_Y)
        }

        val outlineColor = parseColor(homeTeam.secondaryColor)
        drawEndZoneText(g, homeTeam.name, MARGIN / 2, clockwise = false, outlineColor)
        drawEndZoneText(g, homeTeam.name, WIDTH - MARGIN / 2, clockwise = true, outlineColor)

        val midX = FieldCoordinateMapper.toPixelX(50, WIDTH, MARGIN)
        loadLogo(homeTeam.scorebugLogo)?.let { logo ->
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.drawImage(logo, midX - LOGO_SIZE / 2, HEIGHT / 2 - LOGO_SIZE / 2, LOGO_SIZE, LOGO_SIZE, null)
        }

        g.dispose()
        return image
    }

    fun drawScrimmageLines(
        image: BufferedImage,
        losAbs: Int,
        firstDownAbs: Int,
        zoom: Float = 1f,
    ) {
        val g = image.createGraphics()
        g.scale(zoom.toDouble(), zoom.toDouble())
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.stroke = BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)

        val sidelineHalfWidth = (OOB_STROKE_WIDTH / 2).toInt()
        val topY = OOB_INSET + sidelineHalfWidth
        val bottomY = HEIGHT - OOB_INSET - sidelineHalfWidth

        val losX = FieldCoordinateMapper.toPixelX(losAbs.coerceIn(0, 100), WIDTH, MARGIN)
        g.color = LOS_COLOR
        g.drawLine(losX, topY, losX, bottomY)

        val firstDownX = FieldCoordinateMapper.toPixelX(firstDownAbs.coerceIn(0, 100), WIDTH, MARGIN)
        g.color = FIRST_DOWN_COLOR
        g.drawLine(firstDownX, topY, firstDownX, bottomY)

        g.dispose()
    }

    fun drawBall(
        image: BufferedImage,
        x: Int,
        y: Int,
        scale: Float = 1f,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val halfWidth = (12 * scale).toInt().coerceAtLeast(2)
        val halfHeight = (8 * scale).toInt().coerceAtLeast(1)
        g.color = BALL_COLOR
        g.fillOval(x - halfWidth, y - halfHeight, halfWidth * 2, halfHeight * 2)
        g.color = LINE_COLOR
        g.drawLine(x - halfWidth / 2, y, x + halfWidth / 2, y)
        g.dispose()
    }

    fun drawSpiralingBall(
        image: BufferedImage,
        x: Int,
        y: Int,
        spinProgress: Float,
        scale: Float = 1f,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val halfWidth = (12 * scale).toInt()
        val halfHeight = (8 * scale).toInt()
        g.color = BALL_COLOR
        g.fillOval(x - halfWidth, y - halfHeight, halfWidth * 2, halfHeight * 2)

        val angle = spinProgress * 2 * PI
        val laceLength = halfWidth / 2f
        val laceX1 = x - laceLength * cos(angle).toFloat()
        val laceY1 = y - laceLength * sin(angle).toFloat() * 0.5f
        val laceX2 = x + laceLength * cos(angle).toFloat()
        val laceY2 = y + laceLength * sin(angle).toFloat() * 0.5f
        g.color = LINE_COLOR
        g.drawLine(laceX1.toInt(), laceY1.toInt(), laceX2.toInt(), laceY2.toInt())
        g.dispose()
    }

    fun parseColor(colorString: String?): Color {
        return try {
            Color.decode(colorString ?: "#000000")
        } catch (e: NumberFormatException) {
            Color.BLACK
        }
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
        text: String?,
        centerX: Int,
        clockwise: Boolean,
        outlineColor: Color,
    ) {
        val label = text?.uppercase()?.takeIf { it.isNotBlank() } ?: return
        val availableHeight = (HEIGHT - 2 * END_ZONE_TEXT_PADDING).toFloat() - 2 * END_ZONE_OUTLINE_WIDTH
        val maxFontSize = MARGIN - 16

        g.font = Font("Arial", Font.BOLD, maxFontSize)
        val measuredWidth = g.fontMetrics.stringWidth(label)
        var fittedSize =
            if (measuredWidth > 0) {
                (maxFontSize * (availableHeight / measuredWidth)).toInt().coerceIn(6, maxFontSize)
            } else {
                maxFontSize
            }
        g.font = Font("Arial", Font.BOLD, fittedSize)
        while (fittedSize > 6 && g.fontMetrics.stringWidth(label) > availableHeight) {
            fittedSize--
            g.font = Font("Arial", Font.BOLD, fittedSize)
        }
        val metrics = g.fontMetrics
        val labelWidth = metrics.stringWidth(label)

        val transform = g.transform
        g.translate(centerX, HEIGHT / 2)
        g.rotate(if (clockwise) PI / 2 else -PI / 2)
        g.color = outlineColor
        for (dx in -END_ZONE_OUTLINE_WIDTH..END_ZONE_OUTLINE_WIDTH) {
            for (dy in -END_ZONE_OUTLINE_WIDTH..END_ZONE_OUTLINE_WIDTH) {
                if (dx != 0 || dy != 0) {
                    g.drawString(label, -labelWidth / 2 + dx, metrics.ascent / 2 + dy)
                }
            }
        }
        g.color = LINE_COLOR
        g.drawString(label, -labelWidth / 2, metrics.ascent / 2)
        g.transform = transform
    }

    private fun loadLogo(url: String?): BufferedImage? {
        if (url == null) return null
        logoCache[url]?.let { return it }
        return try {
            ImageIO.read(URI(url).toURL())?.also { logoCache[url] = it }
        } catch (e: IOException) {
            Logger.error("Error loading field logo from $url: ${e.message}")
            null
        }
    }
}
