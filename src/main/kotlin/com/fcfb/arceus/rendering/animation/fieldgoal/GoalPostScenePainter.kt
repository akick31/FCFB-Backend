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
    private const val OOB_LINE_STROKE_WIDTH = 10f
    private const val GOAL_LINE_STROKE_WIDTH = 7f
    private const val FAN_DOT_SIZE = 16
    private const val FAN_COL_SPACING = 30
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
    private const val FAN_SEED = 42L
    private const val HOME_PRIMARY_SHARE = 8
    private const val HOME_SECONDARY_SHARE = 2
    private const val HOME_VISITOR_SHARE = 1
    private const val NEUTRAL_SIDE_SHARE = 5
    private const val NEUTRAL_OTHER_SHARE = 1
    private const val TIER_CONTRAST_MIDPOINT = 128
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

    private const val BUILDING_MIN_WIDTH = 28
    private const val BUILDING_MAX_WIDTH = 60
    private const val BUILDING_MIN_HEIGHT_FRACTION = 0.4f
    private const val MIN_BUILDING_WIDTH = 14
    private const val MIN_BUILDING_HEIGHT = 10
    private const val WINDOW_SIZE = 3
    private const val WINDOW_GAP = 7
    private const val MIN_WINDOW_SIZE = 2
    private const val WINDOW_LIT_CHANCE = 0.4f

    private const val CAMPUS_MIN_WIDTH = 34
    private const val CAMPUS_MAX_WIDTH = 64
    private const val CAMPUS_MIN_HEIGHT_FRACTION = 0.3f
    private const val CAMPUS_MAX_HEIGHT_FRACTION = 0.6f
    private const val CAMPUS_WINDOW_LIT_CHANCE = 0.12f
    private const val CAMPUS_ROOF_KINDS = 4
    private const val CAMPUS_GABLE_RISE = 0.3f
    private const val CAMPUS_TOWER_WIDTH_FRACTION = 0.28f
    private const val CAMPUS_TOWER_HEIGHT_FRACTION = 0.7f

    private const val TREE_MIN_RADIUS = 14
    private const val TREE_MAX_RADIUS = 26
    private const val TREE_SPACING = 18
    private const val MIN_TREE_SPACING = 10
    private const val MIN_TREE_RADIUS = 6
    private const val TRUNK_HEIGHT_FRACTION = 0.7f
    private const val MIN_TRUNK_HEIGHT = 2
    private const val TRUNK_WIDTH_FRACTION = 0.22f

    private const val FIELD_WOBBLE_FRACTION = 0.12f
    private const val FIELD_STEP = 40
    private const val MIN_FIELD_STEP = 16

    private const val LAKE_SHIMMER_TOP_DIVISOR = 6
    private const val LAKE_SHIMMER_ROWS = 8
    private const val MIN_SHIMMER_ROW_STEP = 14
    private const val LAKE_SHIMMER_START_JITTER = 60
    private const val LAKE_SHIMMER_MIN_LENGTH = 20
    private const val LAKE_SHIMMER_MAX_LENGTH = 60
    private const val LAKE_SHIMMER_THICKNESS = 2
    private const val LAKE_SHIMMER_MIN_GAP = 30
    private const val LAKE_SHIMMER_MAX_GAP = 90

    private const val MOUNTAIN_MIN_PEAKS = 6
    private const val MOUNTAIN_PEAK_VARIANCE = 3
    private const val MOUNTAIN_MIN_PEAK_FRACTION = 0.5f
    private const val MOUNTAIN_SNOW_SPREAD = 0.18f
    private const val MOUNTAIN_SNOW_DROP = 0.22f
    private val MOUNTAIN_RIDGE_HEIGHT_FRACTIONS = listOf(0.55f, 0.75f, 1.0f)

    private const val HILL_CREST_FRACTION = 0.55f
    private const val JUMBOTRON_WIDTH = 230
    private const val JUMBOTRON_HEIGHT = 84
    private const val JUMBOTRON_MIN_FONT_SIZE = 4
    private const val JUMBOTRON_SCREEN_INSET_FRACTION = 0.08f
    private const val JUMBOTRON_CROWD_POST_HEIGHT = 10
    private const val JUMBOTRON_GROUND_POST_HEIGHT = 30
    private const val JUMBOTRON_POST_WIDTH = 5
    private const val JUMBOTRON_TEXT_MARGIN = 6
    private const val SECONDS_PER_MINUTE = 60
    private const val HILL_SCOREBOARD_WIDTH = 90
    private const val HILL_SCOREBOARD_HEIGHT = 44
    private const val HILL_SCOREBOARD_POST_HEIGHT = 14
    private const val HILL_SCOREBOARD_POST_WIDTH = 4
    private const val HILL_SCOREBOARD_SCREEN_INSET = 0.12f

    val SKY_COLOR: Color = Color(135, 206, 235)
    val STAND_COLOR_LIGHT: Color = Color(120, 120, 130)
    val STAND_COLOR_DARK: Color = Color(60, 60, 65)
    val POST_COLOR: Color = Color(255, 205, 0)
    val DEFENDER_COLOR: Color = Color(20, 20, 20)
    val NET_COLOR: Color = Color(235, 235, 235)
    val DEFAULT_WALL_COLOR: Color = Color.BLACK
    val SILVER_WALL_COLOR: Color = Color(176, 179, 184)
    val WINDOW_COLOR: Color = Color(255, 221, 120)
    val FAN_COLORS: List<Color> =
        listOf(Color(220, 80, 80), Color(80, 120, 220), Color(230, 210, 60), Color(240, 240, 240))
    val STAND_SHADES: List<Color> = listOf(STAND_COLOR_DARK, Color(80, 80, 88), Color(100, 100, 110), STAND_COLOR_LIGHT)
    val BUILDING_SHADES: List<Color> = listOf(Color(45, 50, 65), Color(55, 60, 78), Color(38, 42, 56), Color(65, 70, 88))
    val CAMPUS_SHADES: List<Color> = listOf(Color(120, 74, 58), Color(138, 90, 70), Color(104, 64, 50), Color(146, 100, 78))
    val CAMPUS_ROOF_COLOR: Color = Color(70, 42, 34)
    val CAMPUS_WINDOW_COLOR: Color = Color(255, 236, 200)
    val TREE_SHADES: List<Color> = listOf(Color(26, 58, 34), Color(34, 70, 42), Color(20, 48, 28), Color(42, 82, 50))
    val TRUNK_COLOR: Color = Color(50, 38, 28)
    val FIELD_SHADES: List<Color> = listOf(Color(74, 120, 58), Color(92, 142, 70), Color(110, 162, 84))
    val LAKE_SHADES: List<Color> = listOf(Color(18, 60, 92), Color(26, 76, 110), Color(38, 96, 130), Color(54, 116, 148))
    val LAKE_SHIMMER_COLOR = Color(255, 255, 255, 90)
    val MOUNTAIN_RIDGE_SHADES: List<Color> = listOf(Color(120, 134, 156), Color(96, 112, 138), Color(68, 82, 104))
    val MOUNTAIN_SNOW_COLOR: Color = Color(240, 244, 248)
    val HILL_COLOR: Color = Color(84, 132, 64)
    val HILL_SCOREBOARD_FRAME_COLOR: Color = Color(30, 32, 36)
    val HILL_SCOREBOARD_SCREEN_COLOR: Color = Color(20, 60, 36)
    val HILL_SCOREBOARD_POST_COLOR: Color = Color(70, 70, 76)
    val JUMBOTRON_FRAME_COLOR: Color = Color(25, 27, 30)
    val JUMBOTRON_SCREEN_COLOR: Color = Color(10, 16, 36)
    val JUMBOTRON_TEXT_COLOR: Color = Color.WHITE
    val JUMBOTRON_POST_COLOR: Color = Color(60, 60, 66)

    fun paint(
        theme: FieldTheme,
        endZone: EndZoneDecoration,
        layout: GoalPostSceneLayout,
        midfieldTopOnLeft: Boolean,
        side: TeamSide,
        situation: GameSituation,
    ): BufferedImage {
        val image = BufferedImage(SCENE_WIDTH, SCENE_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

        g.color = SKY_COLOR
        g.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT)
        val backdrop = theme.stadiumBackdrop(side)
        val backdropSize = theme.stadiumSize(side)
        when (backdrop) {
            StadiumBackdrop.CROWD -> drawStands(g, layout, crowdColors(theme), backdropSize)
            StadiumBackdrop.BUILDING -> drawBuildings(g, layout, backdropSize)
            StadiumBackdrop.CAMPUS -> drawCampus(g, layout, backdropSize)
            StadiumBackdrop.FOREST -> drawForest(g, layout, backdropSize)
            StadiumBackdrop.FIELD -> drawField(g, layout, backdropSize)
            StadiumBackdrop.LAKE -> drawLake(g, layout, backdropSize)
            StadiumBackdrop.MOUNTAINS -> drawMountains(g, layout, backdropSize)
            StadiumBackdrop.HILL -> drawHill(g, layout, backdropSize)
            StadiumBackdrop.NONE -> {}
        }
        if (theme.jumbotronEnabled(side)) {
            drawJumbotron(g, layout, backdrop, backdropSize, theme.jumbotronPylonMount(side), situation)
        }

        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
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
        drawNet(g, layout)

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

    private fun crowdColors(theme: FieldTheme): List<Color> {
        val homePrimary = FieldColors.parseColor(theme.homeTeam.primaryColor)
        val homeSecondary = FieldColors.parseColor(theme.homeTeam.secondaryColor)
        val awayPrimary = FieldColors.parseColor(theme.awayTeam.primaryColor)
        if (theme.style != FieldStyle.HOME_FIELD) {
            return List(NEUTRAL_SIDE_SHARE) { homePrimary } +
                List(NEUTRAL_SIDE_SHARE) { awayPrimary } +
                List(NEUTRAL_OTHER_SHARE) { FAN_COLORS.last() }
        }
        return List(HOME_PRIMARY_SHARE) { homePrimary } +
            List(HOME_SECONDARY_SHARE) { homeSecondary } +
            List(HOME_VISITOR_SHARE) { awayPrimary }
    }

    private fun againstTier(
        color: Color,
        tierShade: Color,
    ): Color = if (ColorSimilarity.areSimilar(color, tierShade)) contrastOf(color) else color

    private fun contrastOf(color: Color): Color {
        val lightness = (color.red + color.green + color.blue) / 3
        return if (lightness < TIER_CONTRAST_MIDPOINT) FAN_COLORS.last() else STAND_COLOR_DARK
    }

    private fun drawStands(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        crowd: List<Color>,
        sizeFraction: Float,
    ) {
        val random = Random(FAN_SEED)
        val fanSize = (FAN_DOT_SIZE * layout.scale).toInt().coerceAtLeast(3)
        val fanSpacing = (FAN_COL_SPACING * layout.scale).toInt().coerceAtLeast(8)
        val tierHeight = (REFERENCE_TIER_HEIGHT * layout.scale).toInt().coerceAtLeast(8)
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val backdropTopY = backdropTopY(wallTopY, sizeFraction)
        var tierBottomY = wallTopY
        var tier = 0
        while (tierBottomY > backdropTopY) {
            val tierTopY = tierBottomY - tierHeight
            val tierShade = STAND_SHADES[STAND_SHADES.lastIndex - tier % STAND_SHADES.size]
            g.color = tierShade
            g.fillRect(0, tierTopY, SCENE_WIDTH, tierHeight)
            val fanY = tierTopY + tierHeight / 2 - fanSize / 2
            var x = fanSpacing / 2 + (tier % 2) * fanSpacing / 2
            while (x < SCENE_WIDTH) {
                g.color = againstTier(crowd[random.nextInt(crowd.size)], tierShade)
                g.fillOval(x, fanY, fanSize, fanSize)
                x += fanSpacing
            }
            tierBottomY = tierTopY
            tier++
        }
    }

    private fun drawBuildings(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        sizeFraction: Float,
    ) {
        val random = Random(FAN_SEED)
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val backdropTopY = backdropTopY(wallTopY, sizeFraction)
        val maxHeight = (wallTopY - backdropTopY).coerceAtLeast(MIN_BUILDING_HEIGHT)
        val minWidth = (BUILDING_MIN_WIDTH * layout.scale).toInt().coerceAtLeast(MIN_BUILDING_WIDTH)
        val maxWidth = (BUILDING_MAX_WIDTH * layout.scale).toInt().coerceAtLeast(minWidth + MIN_BUILDING_WIDTH)
        var x = 0
        while (x < SCENE_WIDTH) {
            val width = minWidth + random.nextInt(maxWidth - minWidth + 1)
            val heightFraction = BUILDING_MIN_HEIGHT_FRACTION + random.nextFloat() * (1f - BUILDING_MIN_HEIGHT_FRACTION)
            val height = (maxHeight * heightFraction).toInt().coerceAtLeast(MIN_BUILDING_HEIGHT)
            val top = wallTopY - height
            g.color = BUILDING_SHADES[random.nextInt(BUILDING_SHADES.size)]
            g.fillRect(x, top, width, height)
            drawWindows(g, random, x, top, width, height, layout.scale, WINDOW_COLOR, WINDOW_LIT_CHANCE)
            x += width
        }
    }

    private fun drawWindows(
        g: Graphics2D,
        random: Random,
        buildingX: Int,
        buildingTop: Int,
        buildingWidth: Int,
        buildingHeight: Int,
        scale: Float,
        windowColor: Color,
        litChance: Float,
    ) {
        val windowSize = (WINDOW_SIZE * scale).toInt().coerceAtLeast(MIN_WINDOW_SIZE)
        val gap = (WINDOW_GAP * scale).toInt().coerceAtLeast(windowSize + 1)
        var wy = buildingTop + gap
        while (wy < buildingTop + buildingHeight - gap) {
            var wx = buildingX + gap
            while (wx < buildingX + buildingWidth - gap) {
                if (random.nextFloat() < litChance) {
                    g.color = windowColor
                    g.fillRect(wx, wy, windowSize, windowSize)
                }
                wx += gap
            }
            wy += gap
        }
    }

    private fun drawCampus(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        sizeFraction: Float,
    ) {
        val random = Random(FAN_SEED)
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val backdropTopY = backdropTopY(wallTopY, sizeFraction)
        val maxHeight = (wallTopY - backdropTopY).coerceAtLeast(MIN_BUILDING_HEIGHT)
        val minWidth = (CAMPUS_MIN_WIDTH * layout.scale).toInt().coerceAtLeast(MIN_BUILDING_WIDTH)
        val maxWidth = (CAMPUS_MAX_WIDTH * layout.scale).toInt().coerceAtLeast(minWidth + MIN_BUILDING_WIDTH)
        var x = 0
        while (x < SCENE_WIDTH) {
            val width = minWidth + random.nextInt(maxWidth - minWidth + 1)
            val heightFraction = CAMPUS_MIN_HEIGHT_FRACTION + random.nextFloat() * (CAMPUS_MAX_HEIGHT_FRACTION - CAMPUS_MIN_HEIGHT_FRACTION)
            val bodyHeight = (maxHeight * heightFraction).toInt().coerceAtLeast(MIN_BUILDING_HEIGHT)
            val bodyTop = wallTopY - bodyHeight
            val wallShade = CAMPUS_SHADES[random.nextInt(CAMPUS_SHADES.size)]
            g.color = wallShade
            g.fillRect(x, bodyTop, width, bodyHeight)
            drawWindows(g, random, x, bodyTop, width, bodyHeight, layout.scale, CAMPUS_WINDOW_COLOR, CAMPUS_WINDOW_LIT_CHANCE)
            drawCampusRoof(g, random, x, bodyTop, width, bodyHeight, wallShade)
            x += width
        }
    }

    private fun drawCampusRoof(
        g: Graphics2D,
        random: Random,
        buildingX: Int,
        bodyTop: Int,
        buildingWidth: Int,
        bodyHeight: Int,
        wallShade: Color,
    ) {
        when (random.nextInt(CAMPUS_ROOF_KINDS)) {
            0 -> {
                val roofHeight = (buildingWidth * CAMPUS_GABLE_RISE).toInt()
                val gable = Path2D.Float()
                gable.moveTo(buildingX.toDouble(), bodyTop.toDouble())
                gable.lineTo((buildingX + buildingWidth / 2).toDouble(), (bodyTop - roofHeight).toDouble())
                gable.lineTo((buildingX + buildingWidth).toDouble(), bodyTop.toDouble())
                gable.closePath()
                g.color = CAMPUS_ROOF_COLOR
                g.fill(gable)
            }
            1 -> {
                g.color = CAMPUS_ROOF_COLOR
                g.fillArc(buildingX, bodyTop - buildingWidth / 2, buildingWidth, buildingWidth, 0, 180)
            }
            2 -> {
                val towerWidth = (buildingWidth * CAMPUS_TOWER_WIDTH_FRACTION).toInt().coerceAtLeast(MIN_WINDOW_SIZE)
                val towerHeight = (bodyHeight * CAMPUS_TOWER_HEIGHT_FRACTION).toInt()
                val towerX = buildingX + buildingWidth / 2 - towerWidth / 2
                g.color = wallShade
                g.fillRect(towerX, bodyTop - towerHeight, towerWidth, towerHeight)
                val spire = Path2D.Float()
                spire.moveTo(towerX.toDouble(), (bodyTop - towerHeight).toDouble())
                spire.lineTo((towerX + towerWidth / 2).toDouble(), (bodyTop - towerHeight - towerWidth).toDouble())
                spire.lineTo((towerX + towerWidth).toDouble(), (bodyTop - towerHeight).toDouble())
                spire.closePath()
                g.color = CAMPUS_ROOF_COLOR
                g.fill(spire)
            }
            else -> Unit
        }
    }

    private fun drawForest(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        sizeFraction: Float,
    ) {
        val random = Random(FAN_SEED)
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val backdropTopY = backdropTopY(wallTopY, sizeFraction)
        val spacing = (TREE_SPACING * layout.scale).toInt().coerceAtLeast(MIN_TREE_SPACING)
        var x = spacing / 2
        while (x < SCENE_WIDTH) {
            val radiusRange = TREE_MAX_RADIUS - TREE_MIN_RADIUS + 1
            val radius = ((TREE_MIN_RADIUS + random.nextInt(radiusRange)) * layout.scale).toInt().coerceAtLeast(MIN_TREE_RADIUS)
            val trunkHeight = (radius * TRUNK_HEIGHT_FRACTION).toInt().coerceAtLeast(MIN_TRUNK_HEIGHT)
            val trunkWidth = (radius * TRUNK_WIDTH_FRACTION).toInt().coerceAtLeast(1)
            val canopyBottom = wallTopY - trunkHeight
            val canopyTop = canopyBottom - radius * 2
            if (canopyTop < backdropTopY) {
                x += spacing + random.nextInt(spacing)
                continue
            }
            g.color = TRUNK_COLOR
            g.fillRect(x - trunkWidth / 2, canopyBottom, trunkWidth, trunkHeight)
            g.color = TREE_SHADES[random.nextInt(TREE_SHADES.size)]
            g.fillOval(x - radius, canopyTop, radius * 2, radius * 2)
            x += spacing + random.nextInt(spacing)
        }
    }

    private fun drawField(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        sizeFraction: Float,
    ) {
        val random = Random(FAN_SEED)
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val backdropTopY = backdropTopY(wallTopY, sizeFraction)
        val maxHeight = (wallTopY - backdropTopY).coerceAtLeast(MIN_BUILDING_HEIGHT)
        FIELD_SHADES.forEachIndexed { index, shade ->
            val bandTopFraction = 1f - (index + 1).toFloat() / FIELD_SHADES.size
            val baseY = wallTopY - (maxHeight * bandTopFraction).toInt()
            val amplitude = maxHeight * FIELD_WOBBLE_FRACTION
            val path = Path2D.Float()
            path.moveTo(0.0, wallTopY.toDouble())
            var x = 0
            val step = (FIELD_STEP * layout.scale).toInt().coerceAtLeast(MIN_FIELD_STEP)
            while (x <= SCENE_WIDTH) {
                val wobble = (random.nextFloat() - 0.5f) * amplitude
                path.lineTo(x.toDouble(), (baseY + wobble).toDouble())
                x += step
            }
            path.lineTo(SCENE_WIDTH.toDouble(), wallTopY.toDouble())
            path.closePath()
            g.color = shade
            g.fill(path)
        }
    }

    private fun drawLake(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        sizeFraction: Float,
    ) {
        val random = Random(FAN_SEED)
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val backdropTopY = backdropTopY(wallTopY, sizeFraction)
        val height = (wallTopY - backdropTopY).coerceAtLeast(MIN_BUILDING_HEIGHT)
        LAKE_SHADES.forEachIndexed { index, shade ->
            val bandTop = backdropTopY + height * index / LAKE_SHADES.size
            val bandBottom = backdropTopY + height * (index + 1) / LAKE_SHADES.size
            g.color = shade
            g.fillRect(0, bandTop, SCENE_WIDTH, bandBottom - bandTop)
        }
        g.color = LAKE_SHIMMER_COLOR
        var y = backdropTopY + height / LAKE_SHIMMER_TOP_DIVISOR
        val rowStep = (height / LAKE_SHIMMER_ROWS).coerceAtLeast(MIN_SHIMMER_ROW_STEP)
        while (y < wallTopY) {
            var x = random.nextInt(LAKE_SHIMMER_START_JITTER)
            while (x < SCENE_WIDTH) {
                val len = LAKE_SHIMMER_MIN_LENGTH + random.nextInt(LAKE_SHIMMER_MAX_LENGTH - LAKE_SHIMMER_MIN_LENGTH)
                g.fillRect(x, y, len, LAKE_SHIMMER_THICKNESS)
                x += len + LAKE_SHIMMER_MIN_GAP + random.nextInt(LAKE_SHIMMER_MAX_GAP - LAKE_SHIMMER_MIN_GAP)
            }
            y += rowStep
        }
    }

    private fun drawMountains(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        sizeFraction: Float,
    ) {
        val random = Random(FAN_SEED)
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val backdropTopY = backdropTopY(wallTopY, sizeFraction)
        val maxHeight = (wallTopY - backdropTopY).coerceAtLeast(MIN_BUILDING_HEIGHT)
        MOUNTAIN_RIDGE_SHADES.forEachIndexed { index, shade ->
            val heightFraction = MOUNTAIN_RIDGE_HEIGHT_FRACTIONS[index]
            val withSnow = index == MOUNTAIN_RIDGE_SHADES.lastIndex
            drawRidge(g, random, wallTopY, maxHeight, heightFraction, shade, withSnow)
        }
    }

    private fun drawRidge(
        g: Graphics2D,
        random: Random,
        wallTopY: Int,
        maxHeight: Int,
        heightFraction: Float,
        color: Color,
        withSnow: Boolean,
    ) {
        val peakCount = MOUNTAIN_MIN_PEAKS + random.nextInt(MOUNTAIN_PEAK_VARIANCE)
        val stepX = SCENE_WIDTH.toFloat() / peakCount
        val points = mutableListOf<Pair<Float, Float>>()
        val path = Path2D.Float()
        path.moveTo(0.0, wallTopY.toDouble())
        for (i in 0..peakCount) {
            val x = i * stepX
            val peakScale = MOUNTAIN_MIN_PEAK_FRACTION + random.nextFloat() * (1f - MOUNTAIN_MIN_PEAK_FRACTION)
            val peakHeight = maxHeight * heightFraction * peakScale
            val y = wallTopY - peakHeight
            points.add(x to y)
            path.lineTo(x.toDouble(), y.toDouble())
        }
        path.lineTo(SCENE_WIDTH.toDouble(), wallTopY.toDouble())
        path.closePath()
        g.color = color
        g.fill(path)
        if (withSnow) {
            g.color = MOUNTAIN_SNOW_COLOR
            points.forEach { (x, y) ->
                val cap = Path2D.Float()
                cap.moveTo(x.toDouble(), y.toDouble())
                cap.lineTo((x - stepX * MOUNTAIN_SNOW_SPREAD).toDouble(), (y + stepX * MOUNTAIN_SNOW_DROP).toDouble())
                cap.lineTo((x + stepX * MOUNTAIN_SNOW_SPREAD).toDouble(), (y + stepX * MOUNTAIN_SNOW_DROP).toDouble())
                cap.closePath()
                g.fill(cap)
            }
        }
    }

    private fun drawHill(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        sizeFraction: Float,
    ) {
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val backdropTopY = backdropTopY(wallTopY, sizeFraction)
        val maxHeight = (wallTopY - backdropTopY).coerceAtLeast(MIN_BUILDING_HEIGHT)
        val crestY = wallTopY - (maxHeight * HILL_CREST_FRACTION).toInt()
        val controlY = 2 * crestY - wallTopY
        val hillCenterX = SCENE_WIDTH / 2
        val slope = Path2D.Float()
        slope.moveTo(0.0, wallTopY.toDouble())
        slope.quadTo(hillCenterX.toDouble(), controlY.toDouble(), SCENE_WIDTH.toDouble(), wallTopY.toDouble())
        slope.lineTo(SCENE_WIDTH.toDouble(), wallTopY.toDouble())
        slope.closePath()
        g.color = HILL_COLOR
        g.fill(slope)
        drawHillScoreboard(g, layout, hillCenterX, crestY)
    }

    private fun drawHillScoreboard(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        centerX: Int,
        hillTopY: Int,
    ) {
        val boardWidth = (HILL_SCOREBOARD_WIDTH * layout.scale).toInt().coerceAtLeast(MIN_BUILDING_HEIGHT)
        val boardHeight = (HILL_SCOREBOARD_HEIGHT * layout.scale).toInt().coerceAtLeast(MIN_WINDOW_SIZE)
        val postHeight = (HILL_SCOREBOARD_POST_HEIGHT * layout.scale).toInt().coerceAtLeast(MIN_WINDOW_SIZE)
        val postWidth = (HILL_SCOREBOARD_POST_WIDTH * layout.scale).toInt().coerceAtLeast(1)
        val boardBottom = hillTopY - postHeight
        val boardTop = boardBottom - boardHeight
        g.color = HILL_SCOREBOARD_POST_COLOR
        g.fillRect(centerX - boardWidth / 2 + postWidth, boardBottom, postWidth, postHeight)
        g.fillRect(centerX + boardWidth / 2 - postWidth * 2, boardBottom, postWidth, postHeight)
        g.color = HILL_SCOREBOARD_FRAME_COLOR
        g.fillRect(centerX - boardWidth / 2, boardTop, boardWidth, boardHeight)
        val inset = (boardHeight * HILL_SCOREBOARD_SCREEN_INSET).toInt().coerceAtLeast(1)
        g.color = HILL_SCOREBOARD_SCREEN_COLOR
        g.fillRect(centerX - boardWidth / 2 + inset, boardTop + inset, boardWidth - inset * 2, boardHeight - inset * 2)
    }

    private fun drawJumbotron(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
        backdrop: StadiumBackdrop,
        sizeFraction: Float,
        pylonMount: Boolean,
        situation: GameSituation,
    ) {
        val wallTopY = layout.mapY(REFERENCE_WALL_TOP_Y)
        val centerX = SCENE_WIDTH / 2
        val boardWidth = (JUMBOTRON_WIDTH * layout.scale).toInt().coerceAtLeast(MIN_BUILDING_HEIGHT)
        val boardHeight = (JUMBOTRON_HEIGHT * layout.scale).toInt().coerceAtLeast(MIN_WINDOW_SIZE)
        val postWidth = (JUMBOTRON_POST_WIDTH * layout.scale).toInt().coerceAtLeast(1)

        val onCrowd = backdrop == StadiumBackdrop.CROWD
        val drawsPosts = !onCrowd || pylonMount
        val postHeight =
            if (onCrowd) {
                (JUMBOTRON_CROWD_POST_HEIGHT * layout.scale).toInt()
            } else {
                (JUMBOTRON_GROUND_POST_HEIGHT * layout.scale).toInt()
            }
        val boardBottom =
            if (onCrowd) {
                val backdropTopY = backdropTopY(wallTopY, sizeFraction)
                if (pylonMount) backdropTopY - postHeight else backdropTopY
            } else {
                wallTopY - postHeight
            }
        val boardTop = boardBottom - boardHeight

        if (drawsPosts) {
            g.color = JUMBOTRON_POST_COLOR
            g.fillRect(centerX - boardWidth / 2 + postWidth, boardBottom, postWidth, postHeight)
            g.fillRect(centerX + boardWidth / 2 - postWidth * 2, boardBottom, postWidth, postHeight)
        }

        g.color = JUMBOTRON_FRAME_COLOR
        g.fillRect(centerX - boardWidth / 2, boardTop, boardWidth, boardHeight)
        val inset = (boardHeight * JUMBOTRON_SCREEN_INSET_FRACTION).toInt().coerceAtLeast(1)
        val screenLeft = centerX - boardWidth / 2 + inset
        val screenTop = boardTop + inset
        val screenWidth = boardWidth - inset * 2
        val screenHeight = boardHeight - inset * 2
        g.color = JUMBOTRON_SCREEN_COLOR
        g.fillRect(screenLeft, screenTop, screenWidth, screenHeight)
        drawJumbotronText(g, situation, screenLeft, screenTop, screenWidth, screenHeight)
    }

    private fun drawJumbotronText(
        g: Graphics2D,
        situation: GameSituation,
        screenLeft: Int,
        screenTop: Int,
        screenWidth: Int,
        screenHeight: Int,
    ) {
        val scoreLine =
            "${situation.homeTeamName.uppercase()} ${situation.homeScore}-${situation.awayScore} ${situation.awayTeamName.uppercase()}"
        val situationLine =
            "Q${situation.quarter} ${clockLabel(situation.clockSeconds)}  ${downLabel(situation.down)} & ${situation.yardsToGo}"
        val lines = listOf(scoreLine, situationLine)
        val previousFont = g.font
        val targetWidth = (screenWidth - JUMBOTRON_TEXT_MARGIN * 2).coerceAtLeast(1)
        g.font = fittedFont(g, lines, targetWidth, JUMBOTRON_MIN_FONT_SIZE, MAX_CAPTION_FONT_SIZE)
        val metrics = g.fontMetrics
        val lineHeight = metrics.height
        var y = screenTop + (screenHeight - lineHeight * lines.size) / 2 + metrics.ascent
        g.color = JUMBOTRON_TEXT_COLOR
        lines.forEach { line ->
            val x = screenLeft + (screenWidth - metrics.stringWidth(line)) / 2
            g.drawString(line, x, y)
            y += lineHeight
        }
        g.font = previousFont
    }

    private fun clockLabel(seconds: Int): String {
        val whole = seconds.coerceAtLeast(0)
        val minutes = whole / SECONDS_PER_MINUTE
        val remainder = whole % SECONDS_PER_MINUTE
        return "$minutes:${remainder.toString().padStart(2, '0')}"
    }

    private fun downLabel(down: Int): String =
        when (down) {
            1 -> "1ST"
            2 -> "2ND"
            3 -> "3RD"
            else -> "4TH"
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

    private fun drawNet(
        g: Graphics2D,
        layout: GoalPostSceneLayout,
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
