package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Play
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
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object GoalPostScenePainter {
    const val CENTER_X = FieldBackgroundPainter.WIDTH / 2
    const val BASE_Y = 420

    /**
     * All REFERENCE_* constants are measured downward from 0 = stands top to
     * STACK_HEIGHT_REF = the goal line, at `scale = 1`. [layoutFor] derives `endZoneBottomY`
     * directly from this same stack height so the tallest element (the stands) can never be
     * pushed above y = 0, at any distance.
     */
    private const val STACK_HEIGHT_REF = 250
    private const val REFERENCE_STANDS_TOP_Y = 0
    private const val REFERENCE_STANDS_HEIGHT = 140
    private const val REFERENCE_WALL_HEIGHT = 50
    private const val REFERENCE_END_ZONE_TOP_Y = REFERENCE_STANDS_HEIGHT + REFERENCE_WALL_HEIGHT
    private const val REFERENCE_CROSSBAR_HEIGHT_ABOVE_GROUND = 45
    private const val REFERENCE_CROSSBAR_Y = REFERENCE_END_ZONE_TOP_Y - REFERENCE_CROSSBAR_HEIGHT_ABOVE_GROUND
    private const val REFERENCE_UPRIGHT_HEIGHT_ABOVE_CROSSBAR = 130
    private const val REFERENCE_UPRIGHT_TOP_Y = REFERENCE_CROSSBAR_Y - REFERENCE_UPRIGHT_HEIGHT_ABOVE_CROSSBAR
    private const val REFERENCE_NET_TOP_Y = REFERENCE_UPRIGHT_TOP_Y + 10
    private const val REFERENCE_NET_CONTACT_Y = REFERENCE_CROSSBAR_Y - 60
    private const val REFERENCE_HALF_GAP = 70
    private const val MIN_KICK_SCALE = 0.45f
    private const val SAFE_TOP_MARGIN = 10
    private const val MIN_FIELD_GAP = 40
    private const val MIN_KICK_YARDS = 18f
    private const val MAX_KICK_YARDS = 60f

    data class Layout(
        val scale: Float,
        val endZoneBottomY: Int,
        val crossbarY: Int,
        val uprightTopY: Int,
        val netTopY: Int,
        val netContactY: Int,
        val leftUprightX: Int,
        val rightUprightX: Int,
        val endZoneTopY: Int,
        val kickYardLine: Int,
    ) {
        val uprightMidY: Int get() = (crossbarY + uprightTopY) / 2

        fun mapY(referenceY: Int): Int = endZoneBottomY - ((STACK_HEIGHT_REF - referenceY) * scale).toInt()
    }

    fun layoutFor(play: Play): Layout {
        val yardsToGoal = (100 - play.ballLocation).toFloat().coerceIn(MIN_KICK_YARDS, MAX_KICK_YARDS)
        val distanceFactor = (yardsToGoal - MIN_KICK_YARDS) / (MAX_KICK_YARDS - MIN_KICK_YARDS)
        val scale = 1f - (1f - MIN_KICK_SCALE) * distanceFactor

        val closeBottomY = BASE_Y - MIN_FIELD_GAP
        val farBottomY = SAFE_TOP_MARGIN + (STACK_HEIGHT_REF * MIN_KICK_SCALE).toInt()
        val endZoneBottomY = (closeBottomY - (closeBottomY - farBottomY) * distanceFactor).toInt()
        val halfGap = (REFERENCE_HALF_GAP * scale).toInt()

        fun mapY(referenceY: Int) = endZoneBottomY - ((STACK_HEIGHT_REF - referenceY) * scale).toInt()

        return Layout(
            scale = scale,
            endZoneBottomY = endZoneBottomY,
            crossbarY = mapY(REFERENCE_CROSSBAR_Y),
            uprightTopY = mapY(REFERENCE_UPRIGHT_TOP_Y),
            netTopY = mapY(REFERENCE_NET_TOP_Y),
            netContactY = mapY(REFERENCE_NET_CONTACT_Y),
            leftUprightX = CENTER_X - halfGap,
            rightUprightX = CENTER_X + halfGap,
            endZoneTopY = mapY(REFERENCE_END_ZONE_TOP_Y),
            kickYardLine = if (play.ballLocation <= 50) play.ballLocation else 100 - play.ballLocation,
        )
    }

    private const val REFERENCE_WALL_TOP_Y = REFERENCE_STANDS_HEIGHT
    private const val POST_STROKE_WIDTH = 12f
    private const val OOB_LINE_STROKE_WIDTH = 10f
    private const val STAND_TIER_COUNT = 4
    private const val STAND_TIER_HEIGHT = REFERENCE_STANDS_HEIGHT / STAND_TIER_COUNT
    private const val FAN_DOT_SIZE = 16
    private const val FAN_COL_SPACING = 30
    private const val DOINK_FONT_SIZE = 32
    private const val NET_SIDE_MARGIN = 30
    private const val NET_COLS = 8
    private const val NET_ROWS = 6
    private const val WALL_LOGO_SIZE = 60
    private const val WALL_LOGO_SPACING = 40
    private const val END_ZONE_TEXT_SIZE_FRACTION = 0.55
    private const val END_ZONE_TEXT_MAX_WIDTH_FRACTION = 0.9
    private const val END_ZONE_TEXT_SHEAR = 0.35
    private const val END_ZONE_TEXT_OUTLINE_WIDTH = 2
    private const val YARD_TICK_FONT_SIZE = 20
    private const val YARD_TICK_LABEL_X = 40

    val SKY_COLOR: Color = Color(135, 206, 235)
    val STAND_COLOR_LIGHT: Color = Color(120, 120, 130)
    val STAND_COLOR_DARK: Color = Color(60, 60, 65)
    val POST_COLOR: Color = Color(255, 205, 0)
    val DEFENDER_COLOR: Color = Color(20, 20, 20)
    val NET_COLOR: Color = Color(235, 235, 235)
    val FAN_COLORS: List<Color> =
        listOf(Color(220, 80, 80), Color(80, 120, 220), Color(230, 210, 60), Color(240, 240, 240))
    val STAND_SHADES: List<Color> = listOf(STAND_COLOR_DARK, Color(80, 80, 88), Color(100, 100, 110), STAND_COLOR_LIGHT)

    private const val DEFENDER_HEAD_RADIUS = 12
    private const val DEFENDER_BODY_WIDTH = 26
    private const val DEFENDER_BODY_HEIGHT = 40

    private val logoCache = ConcurrentHashMap<String, BufferedImage>()

    fun paint(
        homeTeam: Team,
        layout: Layout,
    ): BufferedImage {
        val image = BufferedImage(FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.color = SKY_COLOR
        g.fillRect(0, 0, FieldBackgroundPainter.WIDTH, FieldBackgroundPainter.HEIGHT)

        drawStands(g, layout)

        val wallColor = FieldBackgroundPainter.parseColor(homeTeam.secondaryColor)
        g.color = wallColor
        g.fillRect(0, layout.mapY(REFERENCE_WALL_TOP_Y), FieldBackgroundPainter.WIDTH, layout.endZoneTopY - layout.mapY(REFERENCE_WALL_TOP_Y))
        drawWallLogos(g, homeTeam, layout)

        drawNet(g, layout)

        g.color = FieldBackgroundPainter.parseColor(homeTeam.primaryColor)
        g.fillRect(0, layout.endZoneTopY, FieldBackgroundPainter.WIDTH, layout.endZoneBottomY - layout.endZoneTopY)
        drawEndZoneText(g, homeTeam, layout)

        drawGoalPost(g, layout)
        drawBackOfEndZoneLine(g, layout)
        drawFieldGap(g, layout)

        g.dispose()
        return image
    }

    fun drawDoink(
        image: BufferedImage,
        x: Int,
        y: Int,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.font = Font("Arial", Font.BOLD, DOINK_FONT_SIZE)
        g.color = FieldBackgroundPainter.LINE_COLOR
        val text = "*DOINK!*"
        val width = g.fontMetrics.stringWidth(text)
        val textX = (x - width / 2).coerceIn(0, FieldBackgroundPainter.WIDTH - width)
        val textY = (y - 30).coerceAtLeast(DOINK_FONT_SIZE)
        g.drawString(text, textX, textY)
        g.dispose()
    }

    fun drawDefender(
        image: BufferedImage,
        x: Int,
        topY: Int,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = DEFENDER_COLOR

        g.fillOval(x - DEFENDER_HEAD_RADIUS, topY, DEFENDER_HEAD_RADIUS * 2, DEFENDER_HEAD_RADIUS * 2)
        val bodyTop = topY + DEFENDER_HEAD_RADIUS * 2 - 4
        g.fillOval(x - DEFENDER_BODY_WIDTH / 2, bodyTop, DEFENDER_BODY_WIDTH, DEFENDER_BODY_HEIGHT)

        g.stroke = BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val shoulderY = bodyTop + 6
        g.drawLine(x - 8, shoulderY, x - 24, shoulderY - 34)
        g.drawLine(x + 8, shoulderY, x + 24, shoulderY - 34)

        val hipY = bodyTop + DEFENDER_BODY_HEIGHT
        g.drawLine(x - 6, hipY, x - 14, hipY + 24)
        g.drawLine(x + 6, hipY, x + 14, hipY + 24)
        g.dispose()
    }

    private fun drawStands(
        g: Graphics2D,
        layout: Layout,
    ) {
        val random = Random(FAN_SEED)
        val fanSize = (FAN_DOT_SIZE * layout.scale).toInt().coerceAtLeast(2)
        for (tier in 0 until STAND_TIER_COUNT) {
            val tierTopRef = REFERENCE_STANDS_TOP_Y + tier * STAND_TIER_HEIGHT
            val tierTopY = layout.mapY(tierTopRef)
            val tierBottomY = layout.mapY(tierTopRef + STAND_TIER_HEIGHT)
            g.color = STAND_SHADES[tier % STAND_SHADES.size]
            g.fillRect(0, tierTopY, FieldBackgroundPainter.WIDTH, tierBottomY - tierTopY)

            val fanY = (tierTopY + tierBottomY) / 2 - fanSize / 2
            var x = FAN_COL_SPACING / 2
            while (x < FieldBackgroundPainter.WIDTH) {
                g.color = FAN_COLORS[random.nextInt(FAN_COLORS.size)]
                g.fillOval(x, fanY, fanSize, fanSize)
                x += FAN_COL_SPACING
            }
        }
    }

    private fun drawWallLogos(
        g: Graphics2D,
        homeTeam: Team,
        layout: Layout,
    ) {
        val logo = homeTeam.scorebugLogo?.let { loadLogo(it) } ?: return
        val size = (WALL_LOGO_SIZE * layout.scale).toInt().coerceAtLeast(8)
        val spacing = (WALL_LOGO_SPACING * layout.scale).toInt().coerceAtLeast(4)
        val step = size + spacing
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        var y = wallTopY + spacing / 2
        while (y < layout.endZoneTopY) {
            var x = spacing / 2
            while (x < FieldBackgroundPainter.WIDTH) {
                g.drawImage(logo, x, y, size, size, null)
                x += step
            }
            y += step
        }
    }

    private fun drawNet(
        g: Graphics2D,
        layout: Layout,
    ) {
        val netLeftX = layout.leftUprightX - NET_SIDE_MARGIN
        val netRightX = layout.rightUprightX + NET_SIDE_MARGIN
        g.color = NET_COLOR
        g.stroke = BasicStroke(2f)
        val colStep = (netRightX - netLeftX) / NET_COLS
        for (i in 0..NET_COLS) {
            val x = netLeftX + i * colStep
            g.drawLine(x, layout.netTopY, x, layout.crossbarY)
        }
        val rowStep = (layout.crossbarY - layout.netTopY) / NET_ROWS
        for (i in 0..NET_ROWS) {
            val y = layout.netTopY + i * rowStep
            g.drawLine(netLeftX, y, netRightX, y)
        }
    }

    private fun drawGoalPost(
        g: Graphics2D,
        layout: Layout,
    ) {
        val defaultStroke = g.stroke
        g.color = POST_COLOR
        g.stroke = BasicStroke((POST_STROKE_WIDTH * layout.scale).coerceAtLeast(3f))
        g.drawLine(CENTER_X, layout.endZoneTopY, CENTER_X, layout.crossbarY)
        g.drawLine(layout.leftUprightX, layout.crossbarY, layout.rightUprightX, layout.crossbarY)
        g.drawLine(layout.leftUprightX, layout.crossbarY, layout.leftUprightX, layout.uprightTopY)
        g.drawLine(layout.rightUprightX, layout.crossbarY, layout.rightUprightX, layout.uprightTopY)
        g.stroke = defaultStroke
    }

    private fun drawBackOfEndZoneLine(
        g: Graphics2D,
        layout: Layout,
    ) {
        val defaultStroke = g.stroke
        g.stroke = BasicStroke((OOB_LINE_STROKE_WIDTH * layout.scale).coerceAtLeast(4f))
        g.color = FieldBackgroundPainter.LINE_COLOR
        g.drawLine(0, layout.endZoneTopY, FieldBackgroundPainter.WIDTH, layout.endZoneTopY)
        g.stroke = defaultStroke
    }

    private fun drawFieldGap(
        g: Graphics2D,
        layout: Layout,
    ) {
        g.color = FieldBackgroundPainter.TURF_COLOR
        g.fillRect(
            0,
            layout.endZoneBottomY,
            FieldBackgroundPainter.WIDTH,
            FieldBackgroundPainter.HEIGHT - layout.endZoneBottomY,
        )
        if (layout.kickYardLine <= 0) return

        g.color = FieldBackgroundPainter.LINE_COLOR
        g.stroke = BasicStroke(3f)
        g.font = Font("Arial", Font.BOLD, YARD_TICK_FONT_SIZE)
        val fieldSpan = FieldBackgroundPainter.HEIGHT - layout.endZoneBottomY
        var yardMark = 5
        while (yardMark < layout.kickYardLine) {
            val y = layout.endZoneBottomY + fieldSpan * yardMark / layout.kickYardLine
            g.drawLine(0, y, FieldBackgroundPainter.WIDTH, y)
            if (yardMark % 10 == 0) {
                val label = yardMark.toString()
                g.drawString(label, YARD_TICK_LABEL_X, y - 6)
            }
            yardMark += 5
        }
    }

    private fun drawEndZoneText(
        g: Graphics2D,
        homeTeam: Team,
        layout: Layout,
    ) {
        val label = homeTeam.name?.uppercase()?.takeIf { it.isNotBlank() } ?: return
        val bandHeight = (layout.endZoneBottomY - layout.endZoneTopY).coerceAtLeast(1)
        var fontSize = (bandHeight * END_ZONE_TEXT_SIZE_FRACTION).toInt().coerceAtLeast(8)
        g.font = Font("Arial", Font.BOLD, fontSize)
        while (fontSize > 8 && g.fontMetrics.stringWidth(label) > FieldBackgroundPainter.WIDTH * END_ZONE_TEXT_MAX_WIDTH_FRACTION) {
            fontSize--
            g.font = Font("Arial", Font.BOLD, fontSize)
        }
        val width = g.fontMetrics.stringWidth(label)
        val outlineColor = FieldBackgroundPainter.parseColor(homeTeam.secondaryColor)

        val transform = g.transform
        g.translate(FieldBackgroundPainter.WIDTH / 2, layout.endZoneBottomY - bandHeight / 4)
        g.shear(END_ZONE_TEXT_SHEAR, 0.0)
        g.color = outlineColor
        for (dx in -END_ZONE_TEXT_OUTLINE_WIDTH..END_ZONE_TEXT_OUTLINE_WIDTH) {
            for (dy in -END_ZONE_TEXT_OUTLINE_WIDTH..END_ZONE_TEXT_OUTLINE_WIDTH) {
                if (dx != 0 || dy != 0) {
                    g.drawString(label, -width / 2 + dx, dy)
                }
            }
        }
        g.color = FieldBackgroundPainter.LINE_COLOR
        g.drawString(label, -width / 2, 0)
        g.transform = transform
    }

    private fun loadLogo(url: String): BufferedImage? {
        logoCache[url]?.let { return it }
        return try {
            ImageIO.read(URI(url).toURL())?.also { logoCache[url] = it }
        } catch (e: IOException) {
            Logger.error("Error loading field goal wall logo from $url: ${e.message}")
            null
        }
    }

    private const val FAN_SEED = 42L
}
