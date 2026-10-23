package com.fcfb.arceus.rendering.animation.fieldgoal

import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.enums.animation.GoalPostStyle
import com.fcfb.arceus.enums.animation.StadiumBackdrop
import com.fcfb.arceus.enums.animation.WallDesign
import com.fcfb.arceus.enums.animation.YardNumberArrowAlign
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.EndZoneDecoration
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.animation.GameSituation
import com.fcfb.arceus.model.animation.GoalPostSceneLayout
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.rendering.animation.appearance.ColorSimilarity
import com.fcfb.arceus.rendering.animation.appearance.FieldColors
import com.fcfb.arceus.rendering.animation.assets.EndZoneFonts
import com.fcfb.arceus.rendering.animation.assets.LogoFit
import com.fcfb.arceus.rendering.animation.assets.LogoLoader
import com.fcfb.arceus.rendering.animation.assets.LogoPalette
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.BackdropArea
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.GrassPainter
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.StadiumBackdropPainter
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import kotlin.math.PI
import kotlin.math.roundToInt

object GoalPostScenePainter {
    const val SCENE_WIDTH = 800
    const val SCENE_HEIGHT = GoalPostSceneLayout.SCENE_HEIGHT
    const val CENTER_X = SCENE_WIDTH / 2
    const val HOLD_DEPTH_YARDS = 7f
    const val END_ZONE_DEPTH_YARDS = GoalPostSceneLayout.END_ZONE_DEPTH_YARDS
    const val CROSSBAR_HEIGHT_YARDS = 10f / 3f

    private const val REFERENCE_WALL_TOP_Y = 140
    private const val REFERENCE_END_ZONE_TOP_Y = GoalPostSceneLayout.REFERENCE_END_ZONE_TOP_Y
    private const val REFERENCE_CROSSBAR_Y = REFERENCE_END_ZONE_TOP_Y - 45
    private const val REFERENCE_UPRIGHT_TOP_Y = REFERENCE_CROSSBAR_Y - 180
    private const val REFERENCE_NET_TOP_Y = REFERENCE_UPRIGHT_TOP_Y + 10
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

    fun layoutFor(play: Play): GoalPostSceneLayout {
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

        return GoalPostSceneLayout(
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
    private const val GRASS_BEYOND_NET = 8f
    private const val FACADE_REBOUND_SHARE = 0.3f
    private const val SHORT_OF_OBSTACLE_SHARE = 0.4f
    private const val OOB_LINE_STROKE_WIDTH = 10f
    private const val GOAL_LINE_STROKE_WIDTH = 7f
    private const val DOINK_FONT_SIZE = 32
    const val NET_SIDE_MARGIN = 30

    private const val NET_COLS = 8
    private const val NET_ROWS = 6
    private const val WALL_LOGO_SIZE = 57
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
    private const val YARD_NUMBER_OUTLINE_WIDTH = 2
    private const val FG_ARROW_HEIGHT_RATIO = 0.475f
    private const val FG_ARROW_WIDTH_RATIO = 0.4f
    private const val FG_ARROW_GAP_RATIO = 0.22f
    private const val FG_ARROW_GAP_OUTLINED_RATIO = 0.3f
    private const val FG_ARROW_EDGE_GAP_RATIO = 0.1f
    private const val YARD_NUMBER_SHARE_OF_TEN_YARDS = 0.3f
    private const val DIGIT_WIDTH_PER_POINT = 0.56f
    private const val CAPTION_TARGET_YARDS = 14f
    private const val MIN_CAPTION_FONT_SIZE = 7
    private const val MAX_CAPTION_FONT_SIZE = 30
    private const val CAPTION_GAP = 6
    private const val CAPTION_LINE_GAP = 2
    private const val LOCATION_SIZE_DROP = 4
    private const val LOGO_ACCENT_SAMPLES = 8
    private const val NEAR_BLACK_TOTAL = 90
    private const val PALE_WALL_LIGHTNESS = 170
    private const val WALL_CAPTION_GAP = 4
    private const val WALL_BOTTOM_GAP = 10
    private const val WALL_TOP_GAP = 2
    private const val WALL_CAPTIONED_LOGO_FRACTION = 0.55f
    private const val WALL_CAPTION_WIDTH_FACTOR = 2
    private const val CHAMPIONSHIP_TEXT_HEIGHT_FRACTION = 0.72f
    private const val WALL_TEXT_LIFT = 0.14f
    private const val CHAMPIONSHIP_TEXT_MARGIN = 120
    private const val CHAMPIONSHIP_OUTLINE_WIDTH = 1
    private const val WALL_FLANK_LOGO_FRACTION = 0.8f
    private const val WALL_FLANK_GAP = 14
    private const val MIN_WALL_SATURATION = 0.18f
    private const val WALL_FLANK_CENTER_SHARE = 0.34f
    private const val CFP_FLANK_CENTER_SHARE = 0.46f

    val SKY_COLOR: Color = Color(135, 206, 235)
    val POST_COLOR: Color = Color(255, 205, 0)
    val DEFENDER_COLOR: Color = Color(20, 20, 20)
    val NET_COLOR: Color = Color(235, 235, 235)
    val DEFAULT_WALL_COLOR: Color = Color.BLACK
    val SILVER_WALL_COLOR: Color = Color(176, 179, 184)

    fun paint(
        theme: FieldTheme,
        endZone: EndZoneDecoration,
        layout: GoalPostSceneLayout,
        midfieldTopOnLeft: Boolean,
        side: TeamSide,
        situation: GameSituation,
        time: Float = 0f,
    ): BufferedImage {
        val image = BufferedImage(SCENE_WIDTH, SCENE_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

        g.color = SKY_COLOR
        g.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT)
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val showWall = theme.wallShown(side)
        val groundY =
            when {
                showWall -> wallTopY
                theme.stadiumBackdrop(side) == StadiumBackdrop.HILL -> layout.endZoneTopY
                else -> grassTopY(layout)
            }
        val backdropArea =
            BackdropArea(
                width = SCENE_WIDTH,
                groundY = groundY,
                topY = backdropTopY(groundY, theme.stadiumSize(side)),
                scale = layout.scale,
                netHalfWidth = CENTER_X - layout.leftUprightX + NET_SIDE_MARGIN,
                uprightClearHalfWidth = CENTER_X - layout.leftUprightX - (POST_STROKE_WIDTH * layout.scale / 2).toInt(),
                playSeed = situation.hashCode().toLong(),
                frontY = layout.endZoneTopY,
                behindWall = showWall,
                time = time,
                postTopY = layout.uprightTopY,
            )
        StadiumBackdropPainter.paintScenery(g, backdropArea, theme, side)
        if (!showWall) GrassPainter.paint(g, backdropArea.copy(groundY = layout.endZoneTopY), groundY)
        StadiumBackdropPainter.paintForeground(g, backdropArea, theme, side)
        StadiumBackdropPainter.paintJumbotron(g, backdropArea, theme, side, situation)
        if (showWall) drawWall(g, theme, endZone, layout, wallTopY)
        if (backstopFor(theme, side, layout) == KickBackstop.Net) drawNet(g, layout, wallTopY)

        g.color = endZone.fill ?: theme.turf
        g.fillRect(0, layout.endZoneTopY, SCENE_WIDTH, layout.endZoneBottomY - layout.endZoneTopY)
        drawEndZoneText(g, endZone, layout, endZone.fontValue ?: theme.endZoneFont(), theme.endZoneOutlineScale())

        drawGoalPost(g, theme, layout)
        drawBackOfEndZoneLine(g, layout)
        drawFieldGap(g, theme, layout, midfieldTopOnLeft)

        g.dispose()
        return image
    }

    fun drawPost(
        image: BufferedImage,
        theme: FieldTheme,
        layout: GoalPostSceneLayout,
    ) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        drawGoalPost(g, theme, layout)
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
        g.color = FieldColors.LINE_COLOR
        val text = "*DOINK!*"
        val width = g.fontMetrics.stringWidth(text)
        val textX = (x - width / 2).coerceIn(0, SCENE_WIDTH - width)
        val textY = (y - 30).coerceAtLeast(DOINK_FONT_SIZE)
        g.drawString(text, textX, textY)
        g.dispose()
    }

    private fun fittedCaptionFont(
        g: Graphics2D,
        lines: List<String>,
        targetWidth: Int,
    ): Font = fittedFont(g, lines, targetWidth, MIN_CAPTION_FONT_SIZE, MAX_CAPTION_FONT_SIZE)

    private fun fittedFont(
        g: Graphics2D,
        lines: List<String>,
        targetWidth: Int,
        minSize: Int,
        maxSize: Int,
    ): Font {
        var best = Font(Font.SANS_SERIF, Font.BOLD, minSize)
        for (size in minSize..maxSize) {
            val candidate = Font(Font.SANS_SERIF, Font.BOLD, size)
            if (lines.maxOf { g.getFontMetrics(candidate).stringWidth(it) } > targetWidth) break
            best = candidate
        }
        return best
    }

    private fun backdropTopY(
        wallTopY: Int,
        sizeFraction: Float,
    ): Int = (wallTopY * (1f - sizeFraction.coerceIn(0f, 1f))).toInt()

    private fun drawWallLogos(
        g: Graphics2D,
        theme: FieldTheme,
        layout: GoalPostSceneLayout,
        wallLogoUrl: String?,
    ) {
        val logo = LogoLoader.load(wallLogoUrl ?: theme.centerLogoUrl) ?: return
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val wallHeight = layout.endZoneTopY - wallTopY
        val lines = theme.midfieldCaption.map { it.uppercase() }
        val usable = (wallHeight - WALL_BOTTOM_GAP).coerceAtLeast(10)
        val size =
            if (lines.isEmpty()) {
                minOf((WALL_LOGO_SIZE * layout.scale).toInt(), (usable * WALL_LOGO_HEIGHT_FRACTION).toInt()).coerceAtLeast(8)
            } else {
                (usable * WALL_CAPTIONED_LOGO_FRACTION).toInt().coerceAtLeast(8)
            }
        val logoCenterY = if (lines.isEmpty()) wallTopY + usable / 2 else wallTopY + WALL_TOP_GAP + size / 2
        val captionFont = if (lines.isEmpty()) null else fittedCaptionFont(g, lines, size * WALL_CAPTION_WIDTH_FACTOR)
        val captionWidth = captionFont?.let { font -> lines.maxOf { g.getFontMetrics(font).stringWidth(it) } } ?: 0
        val step = maxOf(size, captionWidth) + (WALL_LOGO_SPACING * layout.scale).toInt().coerceAtLeast(4)
        val logosPerSide = CENTER_X / step + 1
        for (slot in 0 until logosPerSide) {
            val offset = step / 2 + slot * step
            for (centerX in listOf(CENTER_X + offset, CENTER_X - offset)) {
                LogoFit().draw(g, logo, centerX, logoCenterY, size)
                captionFont?.let { drawWallCaption(g, lines, it, centerX, logoCenterY + size / 2 + WALL_CAPTION_GAP) }
            }
        }
    }

    private fun drawWallCaption(
        g: Graphics2D,
        lines: List<String>,
        font: Font,
        centerX: Int,
        top: Int,
    ) {
        val previousFont = g.font
        g.font = font
        var baseline = top + g.fontMetrics.ascent
        lines.forEach { line ->
            val x = centerX - g.fontMetrics.stringWidth(line) / 2
            g.color = Color.BLACK
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx != 0 || dy != 0) g.drawString(line, x + dx, baseline + dy)
                }
            }
            g.color = FieldColors.LINE_COLOR
            g.drawString(line, x, baseline)
            baseline += g.fontMetrics.ascent + CAPTION_LINE_GAP
        }
        g.font = previousFont
    }

    private fun wallDesign(theme: FieldTheme): WallDesign {
        if (theme.style != FieldStyle.HOME_FIELD && !theme.hasCustomField) return WallDesign.TEXT_WITH_LOGOS
        return WallDesign.from(theme.wallDesign())
    }

    private fun cfpWall(style: FieldStyle): Boolean = style == FieldStyle.PLAYOFF || style == FieldStyle.NATIONAL_CHAMPIONSHIP

    private fun usesLogoColors(style: FieldStyle): Boolean = style == FieldStyle.BOWL || style == FieldStyle.CONFERENCE_CHAMPIONSHIP

    private fun wallTextOutline(
        theme: FieldTheme,
        endZone: EndZoneDecoration,
        wallPaint: Color,
    ): Color? {
        if (theme.hasCustomField) {
            return (endZone.wallTextOutlineColor ?: theme.wallTextOutlineColor())?.let { FieldColors.parseColor(it) }
        }
        if (!usesLogoColors(theme.style)) return POST_COLOR
        val logo = LogoLoader.load(theme.centerLogoUrl) ?: return Color.BLACK
        return LogoPalette.dominantColors(logo, LOGO_ACCENT_SAMPLES)
            .firstOrNull { !isPaleWall(it) && !ColorSimilarity.areSimilar(it, wallPaint) }
            ?: Color.BLACK
    }

    private fun drawWallText(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        text: String,
        outlineColor: Color?,
        logoUrl: String?,
        flankCenterShare: Float,
        fontValue: String?,
        outlineWidth: Int,
    ) {
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val wallHeight = layout.endZoneTopY - wallTopY
        val previousFont = g.font
        var size = (wallHeight * CHAMPIONSHIP_TEXT_HEIGHT_FRACTION).toInt().coerceAtLeast(MIN_CAPTION_FONT_SIZE)
        g.font = Font(EndZoneFonts.familyOf(fontValue), EndZoneFonts.styleFor(fontValue), size)
        while (size > MIN_CAPTION_FONT_SIZE && g.fontMetrics.stringWidth(text) > SCENE_WIDTH - CHAMPIONSHIP_TEXT_MARGIN) {
            size -= 1
            g.font = Font(EndZoneFonts.familyOf(fontValue), EndZoneFonts.styleFor(fontValue), size)
        }
        val metrics = g.fontMetrics
        val x = CENTER_X - metrics.stringWidth(text) / 2
        val lift = (wallHeight * WALL_TEXT_LIFT).toInt()
        val baseline = wallTopY + (wallHeight + metrics.ascent) / 2 - lift
        outlineColor?.let {
            g.color = it
            for (dx in -outlineWidth..outlineWidth) {
                for (dy in -outlineWidth..outlineWidth) {
                    if (dx != 0 || dy != 0) g.drawString(text, x + dx, baseline + dy)
                }
            }
        }
        g.color = FieldColors.LINE_COLOR
        g.drawString(text, x, baseline)
        g.font = previousFont
        drawFlankingLogos(
            g,
            logoUrl,
            baseline - (metrics.ascent * flankCenterShare).toInt(),
            wallHeight,
            x,
            metrics.stringWidth(text),
        )
    }

    private fun drawFlankingLogos(
        g: Graphics2D,
        logoUrl: String?,
        centerY: Int,
        wallHeight: Int,
        textX: Int,
        textWidth: Int,
    ) {
        val logo = LogoLoader.load(logoUrl) ?: return
        val size = (wallHeight * WALL_FLANK_LOGO_FRACTION).toInt().coerceAtLeast(8)
        LogoFit().draw(g, logo, textX - WALL_FLANK_GAP - size / 2, centerY, size)
        LogoFit().draw(g, logo, textX + textWidth + WALL_FLANK_GAP + size / 2, centerY, size)
    }

    private fun wallColor(
        theme: FieldTheme,
        endZone: EndZoneDecoration,
    ): Color {
        if (theme.hasCustomField) {
            (endZone.wallColor ?: theme.wallColor())?.let { return FieldColors.parseColor(it) }
        }
        if (theme.style == FieldStyle.NATIONAL_CHAMPIONSHIP || theme.style == FieldStyle.PLAYOFF) return Color.BLACK
        if (theme.style == FieldStyle.CONFERENCE_CHAMPIONSHIP) return SILVER_WALL_COLOR
        if (usesLogoColors(theme.style)) {
            logoAccent(theme.centerLogoUrl)?.let { return it }
        }
        val accent = logoAccent(theme.centerLogoUrl)
        val secondary = FieldColors.parseColor(endZone.team.secondaryColor)
        val primary = FieldColors.parseColor(endZone.team.primaryColor)
        listOf(secondary, primary)
            .firstOrNull { usableWall(it) && (accent == null || !ColorSimilarity.areSimilar(it, accent)) }
            ?.let { return it }
        return DEFAULT_WALL_COLOR
    }

    private fun logoAccent(logoUrl: String?): Color? {
        val logo = LogoLoader.load(logoUrl) ?: return null
        return LogoPalette.dominantColors(logo, LOGO_ACCENT_SAMPLES).firstOrNull { usableWall(it) && !isNearBlack(it) }
    }

    private fun usableWall(color: Color): Boolean = !isPaleWall(color) && !ColorSimilarity.isNearGreen(color) && !isNearGrey(color)

    private fun isNearGrey(color: Color): Boolean {
        val brightest = maxOf(color.red, color.green, color.blue)
        val darkest = minOf(color.red, color.green, color.blue)
        return brightest == 0 || (brightest - darkest).toFloat() / brightest < MIN_WALL_SATURATION
    }

    private fun isPaleWall(color: Color): Boolean = (color.red + color.green + color.blue) / 3 > PALE_WALL_LIGHTNESS

    private fun isNearBlack(color: Color): Boolean = color.red + color.green + color.blue < NEAR_BLACK_TOTAL

    private fun drawWall(
        g: Graphics2D,
        theme: FieldTheme,
        endZone: EndZoneDecoration,
        layout: GoalPostSceneLayout,
        wallTopY: Int,
    ) {
        val wallPaint = wallColor(theme, endZone)
        g.color = wallPaint
        g.fillRect(0, wallTopY, SCENE_WIDTH, layout.endZoneTopY - wallTopY)
        val wallText = (endZone.wallText ?: theme.wallCaption ?: theme.midfieldCaption.joinToString(" ")).uppercase()
        val design = endZone.wallDesign?.let { WallDesign.from(it) } ?: wallDesign(theme)
        if (design.showsText && wallText.isNotBlank()) {
            val flankShare = if (cfpWall(theme.style)) CFP_FLANK_CENTER_SHARE else WALL_FLANK_CENTER_SHARE
            val flankLogo =
                if (design == WallDesign.TEXT_ONLY || endZone.wallLogoBlank) {
                    null
                } else {
                    endZone.wallLogoUrl ?: theme.wallLogoUrl ?: theme.centerLogoUrl
                }
            val wallOutlineWidth = (CHAMPIONSHIP_OUTLINE_WIDTH * theme.wallTextOutlineScale()).roundToInt().coerceAtLeast(0)
            drawWallText(
                g,
                layout,
                wallText,
                wallTextOutline(theme, endZone, wallPaint),
                flankLogo,
                flankShare,
                theme.wallTextFont(),
                wallOutlineWidth,
            )
        } else if (design == WallDesign.REPEATING_LOGOS && !endZone.wallLogoBlank) {
            drawWallLogos(g, theme, layout, endZone.wallLogoUrl)
        }
    }

    fun backstopFor(
        theme: FieldTheme,
        side: TeamSide,
        layout: GoalPostSceneLayout,
    ): KickBackstop =
        when {
            theme.stadiumBackdrop(side) == StadiumBackdrop.CROWD -> KickBackstop.Net
            theme.wallShown(side) -> KickBackstop.Wall(layout.mapY(REFERENCE_WALL_TOP_Y))
            StadiumBackdropPainter.hasFacade(theme, side) -> KickBackstop.Facade(grassY(layout, FACADE_REBOUND_SHARE))
            else -> grassLanding(layout, StadiumBackdropPainter.frontmostShare(theme, side))
        }

    private fun grassLanding(
        layout: GoalPostSceneLayout,
        frontmostShare: Float?,
    ): KickBackstop.Grass {
        val share = frontmostShare?.let { it + (1f - it) * SHORT_OF_OBSTACLE_SHARE } ?: 0f
        return KickBackstop.Grass(grassY(layout, share), distance = 1f - share)
    }

    private fun grassY(
        layout: GoalPostSceneLayout,
        share: Float,
    ): Int = grassTopY(layout) + ((layout.endZoneTopY - grassTopY(layout)) * share).toInt()

    private fun grassTopY(layout: GoalPostSceneLayout): Int = layout.crossbarY - (GRASS_BEYOND_NET * layout.scale).toInt().coerceAtLeast(2)

    private fun drawNet(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        bottomY: Int,
    ) {
        val netLeftX = layout.leftUprightX - NET_SIDE_MARGIN
        val netRightX = layout.rightUprightX + NET_SIDE_MARGIN
        g.color = NET_COLOR
        g.stroke = BasicStroke(2f)
        val colStep = (netRightX - netLeftX) / NET_COLS
        for (i in 0..NET_COLS) {
            val x = netLeftX + i * colStep
            g.drawLine(x, layout.netTopY, x, bottomY)
        }
        val rowStep = (bottomY - layout.netTopY) / NET_ROWS
        var y = layout.netTopY
        while (y <= bottomY) {
            g.drawLine(netLeftX, y, netRightX, y)
            y += rowStep
        }
    }

    private fun drawGoalPost(
        g: Graphics2D,
        theme: FieldTheme,
        layout: GoalPostSceneLayout,
    ) {
        val width = (POST_STROKE_WIDTH * layout.scale).coerceAtLeast(3f)
        val lineHalfWidth = (OOB_LINE_STROKE_WIDTH * layout.scale).coerceAtLeast(4f) / 2
        val baseY = (layout.endZoneTopY - lineHalfWidth).toInt()
        g.color = goalPostColor(theme)
        g.stroke = BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
        if (goalPostStyle(theme) == GoalPostStyle.H) {
            g.drawLine(layout.leftUprightX, baseY, layout.leftUprightX, layout.crossbarY)
            g.drawLine(layout.rightUprightX, baseY, layout.rightUprightX, layout.crossbarY)
        } else {
            g.drawLine(CENTER_X, baseY, CENTER_X, layout.crossbarY)
        }
        val uprights =
            Path2D.Float().apply {
                moveTo(layout.leftUprightX.toFloat(), layout.uprightTopY.toFloat())
                lineTo(layout.leftUprightX.toFloat(), layout.crossbarY.toFloat())
                lineTo(layout.rightUprightX.toFloat(), layout.crossbarY.toFloat())
                lineTo(layout.rightUprightX.toFloat(), layout.uprightTopY.toFloat())
            }
        g.draw(uprights)
    }

    private fun goalPostColor(theme: FieldTheme): Color {
        if (!theme.hasCustomField) return POST_COLOR
        return theme.goalPostColor()?.let { FieldColors.parseColor(it) } ?: POST_COLOR
    }

    private fun goalPostStyle(theme: FieldTheme): GoalPostStyle {
        if (!theme.hasCustomField) return GoalPostStyle.Y
        return GoalPostStyle.from(theme.goalPostStyle())
    }

    private fun drawBackOfEndZoneLine(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
    ) {
        g.stroke = BasicStroke((OOB_LINE_STROKE_WIDTH * layout.scale).coerceAtLeast(4f))
        g.color = FieldColors.LINE_COLOR
        g.drawLine(0, layout.endZoneTopY, SCENE_WIDTH, layout.endZoneTopY)
    }

    private fun drawFieldGap(
        g: Graphics2D,
        theme: FieldTheme,
        layout: GoalPostSceneLayout,
        midfieldTopOnLeft: Boolean,
    ) {
        g.color = theme.turf
        g.fillRect(0, layout.endZoneBottomY, SCENE_WIDTH, SCENE_HEIGHT - layout.endZoneBottomY)

        g.color = FieldColors.LINE_COLOR
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
        drawMidfieldLogo(g, theme.centerLogoUrl, layout, midfieldTopOnLeft, theme)

        val yardFont = theme.yardNumberFont()
        g.font = Font(EndZoneFonts.familyOf(yardFont), EndZoneFonts.styleFor(yardFont), yardNumberFontSize(layout))
        val arrowAlign = theme.yardNumberArrowAlign()
        val yardOutlineWidth = (YARD_NUMBER_OUTLINE_WIDTH * theme.yardNumberOutlineScale()).roundToInt().coerceAtLeast(0)
        var numberedLine = 10
        while (numberedLine < layout.visibleYards) {
            val label = (if (numberedLine <= 50) numberedLine else 100 - numberedLine).toString()
            val y = layout.yardY(numberedLine.toFloat()).toInt()
            val leftOutline = theme.yardNumberOutline(numberedLine, top = true)
            val rightOutline = theme.yardNumberOutline(numberedLine, top = false)
            val arrowDir =
                if (numberedLine < 50) {
                    -1
                } else if (numberedLine > 50) {
                    1
                } else {
                    0
                }
            drawSidewaysYardNumber(
                g, label, YARD_NUMBER_INSET, y, readsUpward = true, leftOutline, arrowDir, arrowAlign, yardOutlineWidth,
            )
            drawSidewaysYardNumber(
                g, label, SCENE_WIDTH - YARD_NUMBER_INSET, y, readsUpward = false, rightOutline, arrowDir, arrowAlign, yardOutlineWidth,
            )
            numberedLine += 10
        }
    }

    private fun drawHashMarks(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
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

    private fun drawMidfieldLogo(
        g: Graphics2D,
        logoUrl: String?,
        layout: GoalPostSceneLayout,
        topOnLeft: Boolean,
        theme: FieldTheme,
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
        if (theme.midfieldCaption.isNotEmpty()) {
            val previousFont = g.font
            val targetWidth = (CAPTION_TARGET_YARDS * SCENE_WIDTH / FIELD_WIDTH_YARDS).toInt()
            val lines = theme.midfieldCaption.map { it.uppercase() }
            g.font = fittedCaptionFont(g, lines, targetWidth)
            var baseline = (screenAcross / 2).toInt() + CAPTION_GAP + g.fontMetrics.ascent
            val lineStep = g.fontMetrics.ascent + CAPTION_LINE_GAP
            lines.forEach { line ->
                val x = -g.fontMetrics.stringWidth(line) / 2
                g.color = Color.BLACK
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        if (dx != 0 || dy != 0) g.drawString(line, x + dx, baseline + dy)
                    }
                }
                g.color = FieldColors.LINE_COLOR
                g.drawString(line, x, baseline)
                baseline += lineStep
            }
            theme.midfieldLocation?.uppercase()?.let { location ->
                g.font = Font(Font.SANS_SERIF, Font.BOLD, maxOf(MIN_CAPTION_FONT_SIZE, g.font.size - LOCATION_SIZE_DROP))
                g.color = Color.BLACK
                g.drawString(location, -g.fontMetrics.stringWidth(location) / 2, baseline)
            }
            g.font = previousFont
        }
        g.transform = transform
    }

    private fun yardNumberFontSize(layout: GoalPostSceneLayout): Int {
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
        outline: Color?,
        arrowDir: Int,
        arrowAlign: YardNumberArrowAlign,
        outlineWidth: Int,
    ) {
        val metrics = g.fontMetrics
        val first = label.substring(0, 1)
        val rest = label.substring(1)
        val glyphBounds = g.font.createGlyphVector(g.fontRenderContext, "0").visualBounds
        val numberHeight = glyphBounds.height.toFloat()
        val glyphMinY = glyphBounds.minY.toFloat()
        val transform = g.transform
        g.translate(centerX, lineY)
        g.rotate(if (readsUpward) -Math.PI / 2 else Math.PI / 2)
        val baseline = metrics.ascent / 2 - 2
        val firstX = -metrics.stringWidth(first) - YARD_NUMBER_DIGIT_GAP
        outline?.let {
            g.color = it
            for (dx in -outlineWidth..outlineWidth) {
                for (dy in -outlineWidth..outlineWidth) {
                    if (dx != 0 || dy != 0) {
                        g.drawString(first, firstX + dx, baseline + dy)
                        g.drawString(rest, YARD_NUMBER_DIGIT_GAP + dx, baseline + dy)
                    }
                }
            }
        }
        g.color = FieldColors.LINE_COLOR
        g.drawString(first, firstX, baseline)
        g.drawString(rest, YARD_NUMBER_DIGIT_GAP, baseline)
        if (arrowDir != 0) {
            val localSign = arrowDir * (if (readsUpward) -1 else 1)
            drawSidewaysYardArrow(
                g,
                localSign,
                firstX.toFloat(),
                (YARD_NUMBER_DIGIT_GAP + metrics.stringWidth(rest)).toFloat(),
                baseline + glyphMinY,
                numberHeight,
                arrowAlign,
                outline,
                outlineWidth,
            )
        }
        g.transform = transform
    }

    private fun drawSidewaysYardArrow(
        g: Graphics2D,
        localSign: Int,
        numberStart: Float,
        numberEnd: Float,
        numberTop: Float,
        numberHeight: Float,
        arrowAlign: YardNumberArrowAlign,
        outline: Color?,
        outlineWidth: Int,
    ) {
        val height = numberHeight * FG_ARROW_HEIGHT_RATIO
        val length = numberHeight * FG_ARROW_WIDTH_RATIO
        val gap = numberHeight * (if (outline != null) FG_ARROW_GAP_OUTLINED_RATIO else FG_ARROW_GAP_RATIO)
        val edge = numberHeight * FG_ARROW_EDGE_GAP_RATIO
        val numberBottom = numberTop + numberHeight
        val midY =
            when (arrowAlign) {
                YardNumberArrowAlign.TOP -> numberTop + edge + height / 2
                YardNumberArrowAlign.BOTTOM -> numberBottom - edge - height / 2
                else -> numberTop + numberHeight / 2
            }
        val baseX = if (localSign < 0) numberStart - gap else numberEnd + gap
        val tipX = baseX + localSign * length
        val shape = { dx: Int, dy: Int ->
            Path2D.Float().apply {
                moveTo((tipX + dx).toDouble(), (midY + dy).toDouble())
                lineTo((baseX + dx).toDouble(), (midY - height / 2 + dy).toDouble())
                lineTo((baseX + dx).toDouble(), (midY + height / 2 + dy).toDouble())
                closePath()
            }
        }
        outline?.let {
            g.color = it
            for (dx in -outlineWidth..outlineWidth) {
                for (dy in -outlineWidth..outlineWidth) {
                    if (dx != 0 || dy != 0) g.fill(shape(dx, dy))
                }
            }
        }
        g.color = FieldColors.LINE_COLOR
        g.fill(shape(0, 0))
    }

    private fun drawEndZoneText(
        g: Graphics2D,
        endZone: EndZoneDecoration,
        layout: GoalPostSceneLayout,
        fontValue: String?,
        outlineScale: Float,
    ) {
        val label = (endZone.text?.takeIf { it.isNotBlank() } ?: endZone.team.name)?.uppercase()?.takeIf { it.isNotBlank() } ?: return
        val logo = LogoLoader.load(endZone.logoUrl)
        val font = Font(EndZoneFonts.familyOf(fontValue), EndZoneFonts.styleFor(fontValue), GLYPH_REFERENCE_SIZE)
        val glyphs = font.createGlyphVector(g.fontRenderContext, label)
        val bounds = glyphs.visualBounds
        val letterHeight = bounds.height.toFloat()
        val glyphWidth = bounds.width.toFloat()
        val screenHeight = (layout.endZoneBottomY - layout.endZoneTopY) * END_ZONE_LETTER_DEPTH_FRACTION
        val scaleY = screenHeight / letterHeight
        val centerY = (layout.endZoneTopY + layout.endZoneBottomY) / 2f
        val logoWidth = logo?.let { screenHeight * it.width / it.height } ?: 0f
        val logoGap = if (logo != null) screenHeight * END_ZONE_LOGO_GAP_FRACTION else 0f
        val naturalScaleX = END_ZONE_LETTER_YARDS * (SCENE_WIDTH / FIELD_WIDTH_YARDS) / letterHeight
        val maxTextWidth = SCENE_WIDTH * END_ZONE_HALF_WIDTH_FRACTION * 2 - logoWidth - logoGap
        val scaleX = minOf(naturalScaleX, maxTextWidth / glyphWidth)
        val textWidth = glyphWidth * scaleX
        val groupLeft = CENTER_X - (logoWidth + logoGap + textWidth) / 2
        val textLeft = groupLeft + logoWidth + logoGap

        logo?.let {
            g.drawImage(
                it,
                groupLeft.toInt(),
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
        if (endZone.outlineEnabled) {
            g.color = endZone.outlineColor
            g.stroke =
                BasicStroke(
                    END_ZONE_OUTLINE_STROKE * outlineScale * layout.scale.coerceAtLeast(0.6f),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                )
            g.draw(lettering)
        }
        g.color = endZone.textColor
        g.fill(lettering)
    }
}
