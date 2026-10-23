package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

import com.fcfb.arceus.enums.animation.BackdropVariant
import com.fcfb.arceus.enums.animation.StadiumBackdrop
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.animation.GameSituation
import com.fcfb.arceus.rendering.animation.appearance.FieldColors
import com.fcfb.arceus.rendering.animation.assets.LogoLoader
import com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility.FacilityGround
import java.awt.Color
import java.awt.Graphics2D

object StadiumBackdropPainter {
    private const val CROWD_POST_HEIGHT = 18
    private const val GROUND_POST_HEIGHT = 16
    private const val APRON_STAND_SHARE = 0.35f
    private const val FOREST_YARDS = 50f
    private const val CAMPUS_YARDS = 80f
    private const val BUILDING_YARDS = 90f
    private const val FIELD_YARDS = 250f
    private const val LAKE_YARDS = 600f
    private const val MOUNTAIN_YARDS = 3000f

    fun paintScenery(
        g: Graphics2D,
        area: BackdropArea,
        theme: FieldTheme,
        side: TeamSide,
    ) {
        val backdrop = theme.stadiumBackdrop(side)
        val variant = theme.backdropVariant(side)
        val skyline = theme.skyline(side)
        if (skyline != null && skylineAllowed(theme, side)) {
            val skylineArea = area.copy(topY = 0, behindWall = true)
            paintBackdrop(g, sceneryArea(skylineArea, skyline.backdrop), skyline.backdrop, skyline.variant, theme, side)
        }
        if (!hasForegroundBuilding(theme, side)) paintBackdrop(g, sceneryArea(area, backdrop), backdrop, variant, theme, side)
    }

    fun paintForeground(
        g: Graphics2D,
        area: BackdropArea,
        theme: FieldTheme,
        side: TeamSide,
    ) {
        val backdrop = theme.stadiumBackdrop(side)
        if (!area.behindWall && backdrop != StadiumBackdrop.HILL && !hasFacade(theme, side)) PerimeterFencePainter.paint(g, area)
        if (hasForegroundBuilding(theme, side)) FacilityBackdrop.paint(g, area, theme.backdropVariant(side), FacilityGround.footY(area))
    }

    fun hasFacade(
        theme: FieldTheme,
        side: TeamSide,
    ): Boolean = theme.stadiumBackdrop(side) == StadiumBackdrop.FACILITY && FacilityBackdrop.fillsView(theme.backdropVariant(side))

    /** How far toward the end zone the frontmost thing behind the posts stands, as a share of the stadium grass. */
    fun frontmostShare(
        theme: FieldTheme,
        side: TeamSide,
    ): Float? =
        when {
            hasForegroundBuilding(theme, side) -> FacilityGround.FOOT_SHARE
            hasScoreboard(theme, side) && standsOnGrass(theme, side) -> APRON_STAND_SHARE
            else -> null
        }

    fun skylineAllowed(
        theme: FieldTheme,
        side: TeamSide,
    ): Boolean =
        when (theme.stadiumBackdrop(side)) {
            StadiumBackdrop.NONE -> false
            StadiumBackdrop.CROWD -> theme.stadiumSize(side) < 1f
            StadiumBackdrop.FACILITY -> !hasFacade(theme, side)
            else -> true
        }

    private fun hasForegroundBuilding(
        theme: FieldTheme,
        side: TeamSide,
    ): Boolean =
        theme.stadiumBackdrop(side) == StadiumBackdrop.FACILITY &&
            !hasFacade(theme, side) &&
            theme.jumbotronOverBuilding(side) &&
            hasScoreboard(theme, side)

    private fun standsOnGrass(
        theme: FieldTheme,
        side: TeamSide,
    ): Boolean =
        when (theme.stadiumBackdrop(side)) {
            StadiumBackdrop.CROWD, StadiumBackdrop.HILL -> false
            StadiumBackdrop.FACILITY -> !hasFacade(theme, side) && !hasForegroundBuilding(theme, side)
            else -> true
        }

    private fun paintBackdrop(
        g: Graphics2D,
        scenery: BackdropArea,
        backdrop: StadiumBackdrop,
        variant: BackdropVariant?,
        theme: FieldTheme,
        side: TeamSide,
    ) {
        when (backdrop) {
            StadiumBackdrop.CROWD -> CrowdBackdrop.paint(g, scenery, theme, side)
            StadiumBackdrop.BUILDING -> paintBuildings(g, scenery, variant)
            StadiumBackdrop.CAMPUS -> CampusBackdrop.paint(g, scenery, variant)
            StadiumBackdrop.FOREST -> ForestBackdrop.paint(g, scenery)
            StadiumBackdrop.FIELD -> FarmBackdrop.paint(g, scenery, variant)
            StadiumBackdrop.LAKE -> LakeBackdrop.paint(g, scenery, variant)
            StadiumBackdrop.MOUNTAINS -> MountainBackdrop.paint(g, scenery, variant)
            StadiumBackdrop.HILL -> HillBackdrop.paint(g, scenery)
            StadiumBackdrop.FACILITY -> FacilityBackdrop.paint(g, scenery, variant, scenery.groundY)
            StadiumBackdrop.NONE -> OpenSkyBackdrop.paint(g, scenery)
        }
    }

    fun paintJumbotron(
        g: Graphics2D,
        area: BackdropArea,
        theme: FieldTheme,
        side: TeamSide,
        situation: GameSituation,
    ) {
        val backdrop = theme.stadiumBackdrop(side)
        if (hasScoreboard(theme, side)) {
            val settings = jumbotronSettings(theme, side)
            JumbotronPainter.paint(
                g,
                area,
                mountFor(sceneryArea(area, backdrop), theme, side, settings.pylonMount),
                settings,
                situation,
                FieldColors.parseColor(theme.homeTeam.primaryColor),
                FieldColors.parseColor(theme.awayTeam.primaryColor),
            )
        }
    }

    fun paletteFor(theme: FieldTheme): List<Color> =
        TeamSide.values()
            .flatMap { side ->
                paletteOf(theme.stadiumBackdrop(side), theme.backdropVariant(side)) +
                    PerimeterFencePainter.PALETTE +
                    skylinePalette(theme, side) +
                    if (hasScoreboard(theme, side)) StadiumScoreboardPainter.paletteOf(theme.scoreboardStyle(side)) else emptyList()
            }.distinct()

    private fun skylinePalette(
        theme: FieldTheme,
        side: TeamSide,
    ): List<Color> = theme.skyline(side)?.takeIf { skylineAllowed(theme, side) }?.let { paletteOf(it.backdrop, it.variant) } ?: emptyList()

    private fun sceneryArea(
        area: BackdropArea,
        backdrop: StadiumBackdrop,
    ): BackdropArea =
        when (backdrop) {
            StadiumBackdrop.FOREST -> area.atDepth(FOREST_YARDS)
            StadiumBackdrop.CAMPUS -> area.atDepth(CAMPUS_YARDS)
            StadiumBackdrop.BUILDING -> area.atDepth(BUILDING_YARDS)
            StadiumBackdrop.FIELD -> area.atDepth(FIELD_YARDS)
            StadiumBackdrop.LAKE -> area.atDepth(LAKE_YARDS)
            StadiumBackdrop.MOUNTAINS -> area.atDepth(MOUNTAIN_YARDS)
            StadiumBackdrop.CROWD, StadiumBackdrop.HILL, StadiumBackdrop.FACILITY, StadiumBackdrop.NONE -> area
        }

    private fun hasScoreboard(
        theme: FieldTheme,
        side: TeamSide,
    ): Boolean = theme.jumbotronEnabled(side) || theme.stadiumBackdrop(side) in BUILT_IN_SCOREBOARD

    private val BUILT_IN_SCOREBOARD = setOf(StadiumBackdrop.HILL)

    private fun mountFor(
        area: BackdropArea,
        theme: FieldTheme,
        side: TeamSide,
        pylonMount: Boolean,
    ): JumbotronMount =
        when (theme.stadiumBackdrop(side)) {
            StadiumBackdrop.CROWD ->
                JumbotronMount(
                    groundY = CrowdBackdrop.standsTopY(area),
                    restingPost = if (pylonMount) area.scaled(CROWD_POST_HEIGHT) else 0,
                    canLift = pylonMount,
                    keepInFrame = false,
                )
            StadiumBackdrop.HILL ->
                JumbotronMount(HillBackdrop.bermTopY(area), area.scaled(GROUND_POST_HEIGHT), canLift = true, keepInFrame = false)
            StadiumBackdrop.FACILITY ->
                when {
                    hasFacade(theme, side) -> FacilityBackdrop.boardOver(area, theme.backdropVariant(side), area.groundY)
                    hasForegroundBuilding(theme, side) ->
                        FacilityBackdrop.boardOver(area, theme.backdropVariant(side), FacilityGround.footY(area))
                    else -> groundMount(area)
                }
            else -> groundMount(area)
        }

    private fun groundMount(area: BackdropArea): JumbotronMount {
        val standY = if (area.behindWall) area.groundY else area.groundY + ((area.frontY - area.groundY) * APRON_STAND_SHARE).toInt()
        return JumbotronMount(
            groundY = standY,
            restingPost = area.scaled(GROUND_POST_HEIGHT),
            canLift = true,
            keepInFrame = true,
            footY = if (area.behindWall) area.frontY else standY,
        )
    }

    private fun jumbotronSettings(
        theme: FieldTheme,
        side: TeamSide,
    ) = JumbotronSettings(
        style = theme.scoreboardStyle(side),
        size = theme.jumbotronSize(side),
        width = theme.jumbotronWidth(side),
        elevation = theme.jumbotronElevation(side),
        pylonMount = theme.jumbotronPylonMount(side),
        pylonStyle = theme.pylonStyle(side),
        pylonsOutside = theme.pylonsOutside(side),
        shape = theme.scoreboardShape(side),
        title = theme.homeTeam.name.orEmpty().uppercase(),
        logo = LogoLoader.load(theme.scoreboardLogoUrl(side)),
    )

    private fun paintBuildings(
        g: Graphics2D,
        area: BackdropArea,
        variant: BackdropVariant?,
    ) = when (variant) {
        BackdropVariant.BUILDING_TOWN -> TownBackdrop.paint(g, area)
        BackdropVariant.BUILDING_NEIGHBORHOOD -> NeighborhoodBackdrop.paint(g, area)
        else -> SkylineBackdrop.paint(g, area)
    }

    private fun paletteOf(
        backdrop: StadiumBackdrop,
        variant: BackdropVariant?,
    ): List<Color> =
        when (backdrop) {
            StadiumBackdrop.CROWD -> CrowdBackdrop.FAN_COLORS + CrowdBackdrop.STAND_SHADES
            StadiumBackdrop.BUILDING ->
                when (variant) {
                    BackdropVariant.BUILDING_TOWN -> TownBackdrop.PALETTE
                    BackdropVariant.BUILDING_NEIGHBORHOOD -> NeighborhoodBackdrop.PALETTE
                    else -> SkylineBackdrop.PALETTE
                }
            StadiumBackdrop.CAMPUS -> CampusBackdrop.paletteOf(variant)
            StadiumBackdrop.FOREST -> ForestBackdrop.PALETTE
            StadiumBackdrop.FIELD -> FarmBackdrop.paletteOf(variant)
            StadiumBackdrop.LAKE -> LakeBackdrop.paletteOf(variant)
            StadiumBackdrop.MOUNTAINS -> MountainBackdrop.paletteOf(variant)
            StadiumBackdrop.HILL -> HillBackdrop.PALETTE
            StadiumBackdrop.FACILITY -> FacilityBackdrop.paletteOf(variant)
            StadiumBackdrop.NONE -> OpenSkyBackdrop.PALETTE
        }
}
