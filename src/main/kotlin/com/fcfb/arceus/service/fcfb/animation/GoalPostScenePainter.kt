package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.model.Play
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.util.Random
import kotlin.math.PI

object GoalPostScenePainter {
    const val SCENE_WIDTH = 800
    const val SCENE_HEIGHT = 600
    const val CENTER_X = SCENE_WIDTH / 2
    const val HOLD_DEPTH_YARDS = 7f
    const val END_ZONE_DEPTH_YARDS = 10f
    const val CROSSBAR_HEIGHT_YARDS = 10f / 3f

    /** REFERENCE_* values are measured downward from the top of the stands to the end line at `scale = 1`. */
    private const val REFERENCE_WALL_TOP_Y = 140
    private const val REFERENCE_END_ZONE_TOP_Y = 190
    private const val REFERENCE_CROSSBAR_Y = REFERENCE_END_ZONE_TOP_Y - 45
    private const val REFERENCE_UPRIGHT_TOP_Y = REFERENCE_CROSSBAR_Y - 180
    private const val REFERENCE_NET_TOP_Y = REFERENCE_UPRIGHT_TOP_Y + 10
    private const val REFERENCE_TIER_HEIGHT = 35
    private const val REFERENCE_HALF_GAP = 70
    private const val MIN_KICK_SCALE = 0.45f
    private const val FULL_SCALE_PIXELS_PER_YARD = 13f
    private const val SAFE_TOP_MARGIN = 10
    private const val PAT_LINE_OF_SCRIMMAGE_YARDS = 3f
    private const val CAMERA_DEPTH_YARDS = 3f
    private const val FIELD_GAP_BASE = 90f
    private const val FIELD_GAP_PER_YARD = 7f
    private const val MAX_FIELD_GAP = 430f
    private const val END_ZONE_FORESHORTENING = 0.85f

    data class Layout(
        val scale: Float,
        val endZoneBottomY: Int,
        val endZoneTopY: Int,
        val crossbarY: Int,
        val uprightTopY: Int,
        val netTopY: Int,
        val leftUprightX: Int,
        val rightUprightX: Int,
        val lineOfScrimmageYards: Float,
        val visibleYards: Float,
    ) {
        val uprightMidY: Int get() = (crossbarY + uprightTopY) / 2

        val pixelsPerYard: Float get() = (SCENE_HEIGHT - endZoneBottomY) / visibleYards

        fun mapY(referenceY: Int): Int = endZoneTopY - ((REFERENCE_END_ZONE_TOP_Y - referenceY) * scale).toInt()

        fun yardY(yardsFromGoal: Float): Float = endZoneBottomY + pixelsPerYard * yardsFromGoal

        /** Negative yards are inside the end zone, measured back toward the end line where the posts stand. */
        fun groundY(yardsFromGoal: Float): Float {
            if (yardsFromGoal >= 0f) return yardY(yardsFromGoal)
            return endZoneBottomY + (endZoneTopY - endZoneBottomY) * (-yardsFromGoal / END_ZONE_DEPTH_YARDS)
        }
    }

    /**
     * The bottom of the frame sits just behind the holder, seven yards back from the line of scrimmage. The posts shrink with
     * distance but never so far that the uprights would leave the top of the frame.
     */
    fun layoutFor(play: Play): Layout {
        val lineOfScrimmageYards =
            if (play.playCall == PlayCall.PAT) PAT_LINE_OF_SCRIMMAGE_YARDS else (100 - play.ballLocation).toFloat().coerceAtLeast(1f)
        val visibleYards = lineOfScrimmageYards + HOLD_DEPTH_YARDS + CAMERA_DEPTH_YARDS
        val fieldGap = (FIELD_GAP_BASE + visibleYards * FIELD_GAP_PER_YARD).coerceAtMost(MAX_FIELD_GAP)
        val endZoneBottomY = (SCENE_HEIGHT - fieldGap).toInt()
        val pixelsPerYard = fieldGap / visibleYards
        val endZoneTopY = (endZoneBottomY - END_ZONE_DEPTH_YARDS * pixelsPerYard * END_ZONE_FORESHORTENING).toInt()
        val roomForUprights = (endZoneTopY - SAFE_TOP_MARGIN).toFloat() / (REFERENCE_END_ZONE_TOP_Y - REFERENCE_UPRIGHT_TOP_Y)
        val scale = minOf(pixelsPerYard / FULL_SCALE_PIXELS_PER_YARD, roomForUprights).coerceIn(MIN_KICK_SCALE, 1f)
        val halfGap = (REFERENCE_HALF_GAP * scale).toInt()

        fun mapY(referenceY: Int) = endZoneTopY - ((REFERENCE_END_ZONE_TOP_Y - referenceY) * scale).toInt()

        return Layout(
            scale = scale,
            endZoneBottomY = endZoneBottomY,
            endZoneTopY = endZoneTopY,
            crossbarY = mapY(REFERENCE_CROSSBAR_Y),
            uprightTopY = mapY(REFERENCE_UPRIGHT_TOP_Y),
            netTopY = mapY(REFERENCE_NET_TOP_Y),
            leftUprightX = CENTER_X - halfGap,
            rightUprightX = CENTER_X + halfGap,
            lineOfScrimmageYards = lineOfScrimmageYards,
            visibleYards = visibleYards,
        )
    }

    private const val POST_STROKE_WIDTH = 12f
    private const val OOB_LINE_STROKE_WIDTH = 10f
    private const val GOAL_LINE_STROKE_WIDTH = 7f
    private const val FAN_DOT_SIZE = 16
    private const val FAN_COL_SPACING = 30
    private const val DOINK_FONT_SIZE = 32
    private const val NET_SIDE_MARGIN = 30
    private const val NET_COLS = 8
    private const val NET_ROWS = 6
    private const val WALL_LOGO_SIZE = 60
    private const val WALL_LOGO_SPACING = 40
    private const val WALL_LOGO_HEIGHT_FRACTION = 0.8f
    private const val FIELD_WIDTH_YARDS = 53.3f
    private const val HASH_YARDS_FROM_CENTER = 6.67f
    private const val HASH_TICK_YARDS = 1f
    private const val SIDELINE_TICK_YARDS = 1f
    private const val HASH_STROKE_PER_YARD = 0.25f
    private const val END_ZONE_LETTER_YARDS = 6.5f
    private const val END_ZONE_LETTER_DEPTH_FRACTION = 0.62f
    private const val END_ZONE_HALF_WIDTH_FRACTION = 0.44f
    private const val END_ZONE_OUTLINE_STROKE = 3f
    private const val END_ZONE_LOGO_GAP_FRACTION = 0.25f
    private const val GLYPH_REFERENCE_SIZE = 100
    private const val MIDFIELD_LOGO_ACROSS_YARDS = 16.1f
    private const val MAX_MIDFIELD_LOGO_ALONG_YARDS = 36f
    private const val OVERHEAD_LATERAL_TO_ALONG = 7.2f / 8.4f
    private const val YARD_NUMBER_MAX_SIZE = 40
    private const val YARD_NUMBER_INSET = 60
    private const val YARD_NUMBER_DIGIT_GAP = 6
    private const val YARD_NUMBER_SHARE_OF_TEN_YARDS = 0.3f
    private const val DIGIT_WIDTH_PER_POINT = 0.56f
    private const val FAN_SEED = 42L

    val SKY_COLOR: Color = Color(135, 206, 235)
    val STAND_COLOR_LIGHT: Color = Color(120, 120, 130)
    val STAND_COLOR_DARK: Color = Color(60, 60, 65)
    val POST_COLOR: Color = Color(255, 205, 0)
    val DEFENDER_COLOR: Color = Color(20, 20, 20)
    val NET_COLOR: Color = Color(235, 235, 235)
    val FAN_COLORS: List<Color> =
        listOf(Color(220, 80, 80), Color(80, 120, 220), Color(230, 210, 60), Color(240, 240, 240))
    val STAND_SHADES: List<Color> = listOf(STAND_COLOR_DARK, Color(80, 80, 88), Color(100, 100, 110), STAND_COLOR_LIGHT)

    /** [midfieldTopOnLeft] turns the midfield logo so it reads from the same sideline it does on the overhead field. */
    fun paint(
        theme: FieldTheme,
        endZone: EndZoneDecoration,
        layout: Layout,
        midfieldTopOnLeft: Boolean,
    ): BufferedImage {
        val image = BufferedImage(SCENE_WIDTH, SCENE_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

        g.color = SKY_COLOR
        g.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT)
        drawStands(g, layout)

        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        g.color = FieldBackgroundPainter.parseColor(endZone.team.secondaryColor)
        g.fillRect(0, wallTopY, SCENE_WIDTH, layout.endZoneTopY - wallTopY)
        if (theme.style != FieldStyle.PLAYOFF) {
            drawWallLogos(g, theme.centerLogoUrl, layout)
        }
        drawNet(g, layout)

        g.color = endZone.fill ?: theme.turf
        g.fillRect(0, layout.endZoneTopY, SCENE_WIDTH, layout.endZoneBottomY - layout.endZoneTopY)
        drawEndZoneText(g, endZone, layout)

        drawGoalPost(g, layout)
        drawBackOfEndZoneLine(g, layout)
        drawFieldGap(g, theme, layout, midfieldTopOnLeft)

        g.dispose()
        return image
    }

    /** Redraws the posts over a ball that has passed the end line, so the ball drops behind them instead of in front. */
    fun drawPost(
        image: BufferedImage,
        layout: Layout,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        drawGoalPost(g, layout)
        g.dispose()
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
        val textX = (x - width / 2).coerceIn(0, SCENE_WIDTH - width)
        val textY = (y - 30).coerceAtLeast(DOINK_FONT_SIZE)
        g.drawString(text, textX, textY)
        g.dispose()
    }

    /** Rows of seats climb from the wall to the top of the frame, however much room the kick distance leaves. */
    private fun drawStands(
        g: Graphics2D,
        layout: Layout,
    ) {
        val random = Random(FAN_SEED)
        val fanSize = (FAN_DOT_SIZE * layout.scale).toInt().coerceAtLeast(3)
        val fanSpacing = (FAN_COL_SPACING * layout.scale).toInt().coerceAtLeast(8)
        val tierHeight = (REFERENCE_TIER_HEIGHT * layout.scale).toInt().coerceAtLeast(8)
        var tierBottomY = layout.mapY(REFERENCE_WALL_TOP_Y)
        var tier = 0
        while (tierBottomY > 0) {
            val tierTopY = tierBottomY - tierHeight
            g.color = STAND_SHADES[STAND_SHADES.lastIndex - tier % STAND_SHADES.size]
            g.fillRect(0, tierTopY, SCENE_WIDTH, tierHeight)
            val fanY = tierTopY + tierHeight / 2 - fanSize / 2
            var x = fanSpacing / 2 + (tier % 2) * fanSpacing / 2
            while (x < SCENE_WIDTH) {
                g.color = FAN_COLORS[random.nextInt(FAN_COLORS.size)]
                g.fillOval(x, fanY, fanSize, fanSize)
                x += fanSpacing
            }
            tierBottomY = tierTopY
            tier++
        }
    }

    private fun drawWallLogos(
        g: Graphics2D,
        logoUrl: String?,
        layout: Layout,
    ) {
        val logo = LogoLoader.load(logoUrl) ?: return
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val wallHeight = layout.endZoneTopY - wallTopY
        val size = minOf((WALL_LOGO_SIZE * layout.scale).toInt(), (wallHeight * WALL_LOGO_HEIGHT_FRACTION).toInt()).coerceAtLeast(8)
        val step = size + (WALL_LOGO_SPACING * layout.scale).toInt().coerceAtLeast(4)
        val y = wallTopY + wallHeight / 2
        val logosPerSide = CENTER_X / step + 1
        for (slot in 0 until logosPerSide) {
            val offset = step / 2 + slot * step
            LogoFit.draw(g, logo, CENTER_X + offset, y, size)
            LogoFit.draw(g, logo, CENTER_X - offset, y, size)
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

    /** The post is planted behind the end line, so the stem stops at the line instead of poking into the end zone. */
    private fun drawGoalPost(
        g: Graphics2D,
        layout: Layout,
    ) {
        val width = (POST_STROKE_WIDTH * layout.scale).coerceAtLeast(3f)
        val lineHalfWidth = (OOB_LINE_STROKE_WIDTH * layout.scale).coerceAtLeast(4f) / 2
        g.color = POST_COLOR
        g.stroke = BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
        g.drawLine(CENTER_X, (layout.endZoneTopY - lineHalfWidth).toInt(), CENTER_X, layout.crossbarY)
        val uprights =
            Path2D.Float().apply {
                moveTo(layout.leftUprightX.toFloat(), layout.uprightTopY.toFloat())
                lineTo(layout.leftUprightX.toFloat(), layout.crossbarY.toFloat())
                lineTo(layout.rightUprightX.toFloat(), layout.crossbarY.toFloat())
                lineTo(layout.rightUprightX.toFloat(), layout.uprightTopY.toFloat())
            }
        g.draw(uprights)
    }

    private fun drawBackOfEndZoneLine(
        g: Graphics2D,
        layout: Layout,
    ) {
        g.stroke = BasicStroke((OOB_LINE_STROKE_WIDTH * layout.scale).coerceAtLeast(4f))
        g.color = FieldBackgroundPainter.LINE_COLOR
        g.drawLine(0, layout.endZoneTopY, SCENE_WIDTH, layout.endZoneTopY)
    }

    private fun drawFieldGap(
        g: Graphics2D,
        theme: FieldTheme,
        layout: Layout,
        midfieldTopOnLeft: Boolean,
    ) {
        g.color = theme.turf
        g.fillRect(0, layout.endZoneBottomY, SCENE_WIDTH, SCENE_HEIGHT - layout.endZoneBottomY)

        g.color = FieldBackgroundPainter.LINE_COLOR
        g.stroke = BasicStroke(3f)
        var yardLine = 5
        while (yardLine < layout.visibleYards) {
            val y = layout.yardY(yardLine.toFloat()).toInt()
            g.drawLine(0, y, SCENE_WIDTH, y)
            yardLine += 5
        }
        drawHashMarks(g, layout)
        g.stroke = BasicStroke((GOAL_LINE_STROKE_WIDTH * layout.scale).coerceAtLeast(3f))
        g.drawLine(0, layout.endZoneBottomY, SCENE_WIDTH, layout.endZoneBottomY)
        drawMidfieldLogo(g, theme.centerLogoUrl, layout, midfieldTopOnLeft)

        g.color = FieldBackgroundPainter.LINE_COLOR
        g.font = Font("Arial", Font.BOLD, yardNumberFontSize(layout))
        var numberedLine = 10
        while (numberedLine < layout.visibleYards) {
            val label = (if (numberedLine <= 50) numberedLine else 100 - numberedLine).toString()
            val y = layout.yardY(numberedLine.toFloat()).toInt()
            drawSidewaysYardNumber(g, label, YARD_NUMBER_INSET, y, readsUpward = true)
            drawSidewaysYardNumber(g, label, SCENE_WIDTH - YARD_NUMBER_INSET, y, readsUpward = false)
            numberedLine += 10
        }
    }

    /** Hash marks and sideline ticks run parallel to the yard lines at every yard between the five-yard lines. */
    private fun drawHashMarks(
        g: Graphics2D,
        layout: Layout,
    ) {
        val pixelsAcross = SCENE_WIDTH / FIELD_WIDTH_YARDS
        val hashOffset = HASH_YARDS_FROM_CENTER * pixelsAcross
        val hashHalf = HASH_TICK_YARDS * pixelsAcross / 2
        val sidelineTick = SIDELINE_TICK_YARDS * pixelsAcross
        g.stroke = BasicStroke(maxOf(1.5f, layout.pixelsPerYard * HASH_STROKE_PER_YARD))
        var yard = 1
        while (yard < layout.visibleYards) {
            if (yard % 5 != 0) {
                val y = layout.yardY(yard.toFloat())
                g.draw(Line2D.Float(CENTER_X - hashOffset - hashHalf, y, CENTER_X - hashOffset + hashHalf, y))
                g.draw(Line2D.Float(CENTER_X + hashOffset - hashHalf, y, CENTER_X + hashOffset + hashHalf, y))
                g.draw(Line2D.Float(0f, y, sidelineTick, y))
                g.draw(Line2D.Float(SCENE_WIDTH - sidelineTick, y, SCENE_WIDTH.toFloat(), y))
            }
            yard++
        }
    }

    /**
     * Painted at the 50 the way the overhead field paints it: slightly wider than the hashes across the field and turned to
     * read from the sideline, then squeezed front to back by the same yards-to-pixels squash as the turf around it.
     */
    private fun drawMidfieldLogo(
        g: Graphics2D,
        logoUrl: String?,
        layout: Layout,
        topOnLeft: Boolean,
    ) {
        val logo = LogoLoader.load(logoUrl) ?: return
        val aspect = logo.width.toFloat() / logo.height
        val naturalAlongYards = MIDFIELD_LOGO_ACROSS_YARDS * aspect * OVERHEAD_LATERAL_TO_ALONG
        val alongYards = minOf(naturalAlongYards, MAX_MIDFIELD_LOGO_ALONG_YARDS)
        val acrossYards = MIDFIELD_LOGO_ACROSS_YARDS * alongYards / naturalAlongYards
        if (layout.visibleYards < 50f + alongYards / 2) return
        val screenAcross = acrossYards * (SCENE_WIDTH / FIELD_WIDTH_YARDS)
        val screenAlong = alongYards * layout.pixelsPerYard
        val transform = g.transform
        g.translate(CENTER_X.toDouble(), layout.yardY(50f).toDouble())
        g.rotate(if (topOnLeft) -PI / 2 else PI / 2)
        g.drawImage(logo, (-screenAlong / 2).toInt(), (-screenAcross / 2).toInt(), screenAlong.toInt(), screenAcross.toInt(), null)
        g.transform = transform
    }

    /** Sized so a split number, one digit on each side of its line, stays well clear of the next numbered line. */
    private fun yardNumberFontSize(layout: Layout): Int {
        val tenYards = layout.pixelsPerYard * 10
        return ((tenYards * YARD_NUMBER_SHARE_OF_TEN_YARDS - YARD_NUMBER_DIGIT_GAP) / DIGIT_WIDTH_PER_POINT).toInt().coerceIn(
            8,
            YARD_NUMBER_MAX_SIZE,
        )
    }

    private fun drawSidewaysYardNumber(
        g: Graphics2D,
        label: String,
        centerX: Int,
        lineY: Int,
        readsUpward: Boolean,
    ) {
        val metrics = g.fontMetrics
        val first = label.substring(0, 1)
        val rest = label.substring(1)
        val transform = g.transform
        g.translate(centerX, lineY)
        g.rotate(if (readsUpward) -Math.PI / 2 else Math.PI / 2)
        val baseline = metrics.ascent / 2 - 2
        g.drawString(first, -metrics.stringWidth(first) - YARD_NUMBER_DIGIT_GAP, baseline)
        g.drawString(rest, YARD_NUMBER_DIGIT_GAP, baseline)
        g.transform = transform
    }

    /**
     * Lettering painted on the grass, seen from behind the kicker: each letter keeps its real width across the field but is
     * squashed front to back. The name is centered, with any logo set in front of it.
     */
    private fun drawEndZoneText(
        g: Graphics2D,
        endZone: EndZoneDecoration,
        layout: Layout,
    ) {
        val label = endZone.team.name?.uppercase()?.takeIf { it.isNotBlank() } ?: return
        val logo = LogoLoader.load(endZone.logoUrl)
        val glyphs = Font("Arial", Font.BOLD, GLYPH_REFERENCE_SIZE).createGlyphVector(g.fontRenderContext, label)
        val bounds = glyphs.visualBounds
        val letterHeight = bounds.height.toFloat()
        val glyphWidth = bounds.width.toFloat()
        val logoSpan = logo?.let { letterHeight * it.width / it.height + letterHeight * END_ZONE_LOGO_GAP_FRACTION } ?: 0f
        val naturalScaleX = END_ZONE_LETTER_YARDS * (SCENE_WIDTH / FIELD_WIDTH_YARDS) / letterHeight
        val scaleX = minOf(naturalScaleX, SCENE_WIDTH * END_ZONE_HALF_WIDTH_FRACTION / (glyphWidth / 2 + logoSpan))
        val screenHeight = (layout.endZoneBottomY - layout.endZoneTopY) * END_ZONE_LETTER_DEPTH_FRACTION
        val scaleY = screenHeight / letterHeight
        val textLeft = CENTER_X - glyphWidth * scaleX / 2
        val centerY = (layout.endZoneTopY + layout.endZoneBottomY) / 2f

        logo?.let {
            val logoWidth = letterHeight * it.width / it.height * scaleX
            g.drawImage(
                it,
                (textLeft - logoSpan * scaleX).toInt(),
                (centerY - screenHeight / 2).toInt(),
                logoWidth.toInt(),
                screenHeight.toInt(),
                null,
            )
        }
        val transform =
            AffineTransform().apply {
                translate(textLeft.toDouble(), centerY.toDouble())
                scale(scaleX.toDouble(), scaleY.toDouble())
                translate(-bounds.x, -bounds.centerY)
            }
        val lettering = transform.createTransformedShape(glyphs.outline)
        g.color = endZone.outlineColor
        g.stroke = BasicStroke(END_ZONE_OUTLINE_STROKE * layout.scale.coerceAtLeast(0.6f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(lettering)
        g.color = endZone.textColor
        g.fill(lettering)
    }
}
