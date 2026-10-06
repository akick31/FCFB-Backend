package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.BowlField
import com.fcfb.arceus.model.FieldAppearance
import com.fcfb.arceus.model.PostseasonField
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniformHistory
import java.awt.Color
import java.awt.image.BufferedImage

data class FieldTheme(
    val style: FieldStyle,
    val homeTeam: Team,
    val awayTeam: Team,
    val centerLogoUrl: String?,
    val flipped: Boolean = false,
    val turf: Color = FieldBackgroundPainter.TURF_COLOR,
    val homeConferenceLogoUrl: String? = null,
    val awayConferenceLogoUrl: String? = null,
    val homeUniform: TeamUniformHistory? = null,
    val awayUniform: TeamUniformHistory? = null,
    val midfieldCaption: List<String> = emptyList(),
    val midfieldLocation: String? = null,
    val wallCaption: String? = null,
    val wallLogoUrl: String? = null,
    val homeField: FieldAppearance? = null,
    val quarterLogoUrl: String? = null,
    val bowlField: BowlField? = null,
    val postseasonField: PostseasonField? = null,
    val awayUsesAlternateHelmet: Boolean = false,
) {
    private val bowlStyling: BowlFieldStyling? by lazy { bowlField?.let { BowlFieldStyling(it, homeTeam, awayTeam, turf) } }

    val hasCustomField: Boolean get() = homeField != null || bowlField != null || postseasonField != null

    fun endZoneFont(): String? = homeField?.endZoneFont ?: bowlField?.endZoneFont ?: postseasonField?.endZoneFont

    fun wallDesign(): String? = homeField?.wallDesign ?: bowlField?.wallDesign ?: postseasonField?.wallDesign

    fun wallColor(): String? = homeField?.wallColor ?: bowlField?.wallColor ?: postseasonField?.wallColor

    fun wallTextOutlineColor(): String? =
        homeField?.wallTextOutlineColor ?: bowlField?.wallTextOutlineColor ?: postseasonField?.wallTextOutlineColor

    fun goalPostColor(): String? = homeField?.goalPostColor ?: bowlField?.goalPostColor ?: postseasonField?.goalPostColor

    fun goalPostStyle(): String? = homeField?.goalPostStyle ?: bowlField?.goalPostStyle ?: postseasonField?.goalPostStyle

    fun yardNumberOutline(
        yard: Int,
        top: Boolean,
    ): Color? {
        bowlStyling?.let { return it.yardNumberOutline(yard, top, teamOf(leftSide()), teamOf(rightSide())) }
        parsed(homeField?.fieldNumberOutlineColor)?.let { return it }
        postseasonField?.let { field ->
            if (YardNumberSource.from(field.yardNumberSource) == YardNumberSource.TEAM_PER_SIDE) {
                val owner =
                    when {
                        yard < HALF_FIELD -> teamOf(leftSide())
                        yard > HALF_FIELD -> teamOf(rightSide())
                        top -> homeTeam
                        else -> awayTeam
                    }
                return FieldBackgroundPainter.parseColor(owner.primaryColor)
            }
            return parsed(field.yardNumberOutlineColor)
        }
        return null
    }

    fun redZoneBorder(yard: Int): Color? {
        bowlStyling?.let { return it.redZoneBorder(yard, teamOf(leftSide()), teamOf(rightSide())) }
        parsed(homeField?.redZoneBorderColor)?.let { return it }
        postseasonField?.let { field ->
            val side = if (yard < HALF_FIELD) field.leftRedZoneColor else field.rightRedZoneColor
            resolveToken(side)?.let { return it }
            return parsed(field.redZoneBorderColor)
        }
        return null
    }

    fun sidelineAccent(physicalLeft: Boolean): Color? {
        bowlStyling?.let { return it.sidelineAccent(homeEnd = physicalLeft == (leftSide() == TeamSide.HOME)) }
        parsed(homeField?.oobLineColor)?.let { return it }
        postseasonField?.let { field ->
            val side = if (physicalLeft) field.leftSidelineColor else field.rightSidelineColor
            resolveToken(side)?.let { return it }
            return parsed(field.sidelineAccentColor)
        }
        return null
    }

    private fun resolveToken(value: String?): Color? =
        ColorToken.resolve(
            value,
            FieldBackgroundPainter.parseColor(homeTeam.primaryColor),
            FieldBackgroundPainter.parseColor(awayTeam.primaryColor),
        )

    private fun parsed(hex: String?): Color? = hex?.let { FieldBackgroundPainter.parseColor(it) }

    private fun teamOf(side: TeamSide): Team = if (side == TeamSide.HOME) homeTeam else awayTeam

    fun uniforms(): Pair<Uniform, Uniform> = Uniforms.forMatchup(homeTeam, awayTeam, homeUniform, awayUniform)

    fun homeLogoUrl(): String? = helmetDecalUrl(homeUniform, homeTeam, alt = false)

    fun awayLogoUrl(): String? = helmetDecalUrl(awayUniform, awayTeam, alt = awayUsesAlternateHelmet)

    private fun helmetDecalUrl(
        uniform: TeamUniformHistory?,
        team: Team,
        alt: Boolean,
    ): String? {
        val mode = HelmetLogoMode.from(HelmetFields.helmetLogoMode(uniform, alt), HelmetFields.hasLogo(uniform, alt))
        if (mode == HelmetLogoMode.UPLOAD) return HelmetFields.logoUrl(uniform, alt)
        return when (LogoSource.from(HelmetFields.helmetLogoSource(uniform, alt))) {
            LogoSource.SECONDARY -> team.secondaryLogo
            else -> team.logo
        }
    }

    fun leftSide(): TeamSide = if (flipped) TeamSide.AWAY else TeamSide.HOME

    fun rightSide(): TeamSide = if (flipped) TeamSide.HOME else TeamSide.AWAY

    fun conferenceLogoOf(side: TeamSide): String? = if (side == TeamSide.HOME) homeConferenceLogoUrl else awayConferenceLogoUrl

    fun conferenceLogoImage(side: TeamSide): BufferedImage? {
        val url = conferenceLogoOf(side)
        if (style != FieldStyle.HOME_FIELD) return LogoLoader.load(url)
        return ConferenceLogoTint.load(
            url,
            homeField?.conferenceLogoColorMap,
            FieldBackgroundPainter.parseColor(homeTeam.primaryColor),
            FieldBackgroundPainter.parseColor(homeTeam.secondaryColor),
            FieldBackgroundPainter.parseColor(homeTeam.tertiaryColor ?: homeTeam.secondaryColor),
        )
    }

    fun endZoneOf(side: TeamSide): EndZoneDecoration {
        val team = if (style == FieldStyle.HOME_FIELD || side == TeamSide.HOME) homeTeam else awayTeam
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        val secondary = FieldBackgroundPainter.parseColor(team.secondaryColor)
        val base =
            when (style) {
                FieldStyle.HOME_FIELD -> {
                    val text =
                        homeField?.endZoneTextColor?.let { FieldBackgroundPainter.parseColor(it) } ?: FieldBackgroundPainter.LINE_COLOR
                    val outline =
                        homeField?.endZoneOutlineColor?.let { FieldBackgroundPainter.parseColor(it) } ?: outlineOf(text, secondary)
                    val custom = if (side == leftSide()) homeField?.endZoneTextLeft else homeField?.endZoneTextRight
                    EndZoneDecoration(
                        team,
                        homeFieldEndZoneFill() ?: primary,
                        text,
                        outline,
                        homeFieldEndZoneLogo(),
                        custom?.takeIf { it.isNotBlank() },
                        homeFieldEndZoneLogoScale(),
                        homeField?.endZoneOutlineEnabled != false,
                    )
                }
                FieldStyle.BOWL ->
                    bowlStyling?.endZoneOf(team, side == TeamSide.HOME)
                        ?: EndZoneDecoration(
                            team,
                            primary,
                            FieldBackgroundPainter.LINE_COLOR,
                            outlineOf(FieldBackgroundPainter.LINE_COLOR, secondary),
                            null,
                        )
                FieldStyle.PLAYOFF ->
                    onGrass(
                        team,
                        primary,
                        secondary,
                        centerLogoUrl,
                        PLAYOFF_END_ZONE_LOGO_SCALE,
                        postseasonEndZoneText(side),
                    )
                FieldStyle.NATIONAL_CHAMPIONSHIP ->
                    EndZoneDecoration(
                        team,
                        Color.BLACK,
                        FieldBackgroundPainter.LINE_COLOR,
                        outlineOf(FieldBackgroundPainter.LINE_COLOR, primary),
                        centerLogoUrl,
                        null,
                        POSTSEASON_END_ZONE_LOGO_SCALE,
                    )
                FieldStyle.CONFERENCE_CHAMPIONSHIP ->
                    onGrass(team, primary, secondary, postseasonEndZoneLogo(side), 0f, postseasonEndZoneText(side))
            }
        return base.copy(
            fontFamily = EndZoneFonts.familyOf(endZoneFontFor(side)),
            wallDesign = wallDesignFor(side),
            wallText = wallTextFor(side),
        )
    }

    private fun endZoneFontFor(side: TeamSide): String? {
        val perSide =
            when (style) {
                FieldStyle.HOME_FIELD -> if (side == leftSide()) homeField?.leftEndZoneFont else homeField?.rightEndZoneFont
                FieldStyle.BOWL -> if (side == leftSide()) bowlField?.leftEndZoneFont else bowlField?.rightEndZoneFont
                else -> if (side == leftSide()) postseasonField?.leftEndZoneFont else postseasonField?.rightEndZoneFont
            }
        return perSide ?: endZoneFont()
    }

    private fun wallDesignFor(side: TeamSide): String? {
        val perSide =
            when (style) {
                FieldStyle.HOME_FIELD -> if (side == leftSide()) homeField?.wallDesign else homeField?.rightWallDesign
                FieldStyle.BOWL -> if (side == leftSide()) bowlField?.wallDesign else bowlField?.rightWallDesign
                else -> if (side == leftSide()) postseasonField?.wallDesign else postseasonField?.rightWallDesign
            }
        return perSide ?: wallDesign()
    }

    private fun wallTextFor(side: TeamSide): String? {
        val perSide =
            when (style) {
                FieldStyle.HOME_FIELD -> if (side == leftSide()) homeField?.wallText else homeField?.rightWallText
                FieldStyle.BOWL -> if (side == leftSide()) bowlField?.wallText else bowlField?.rightWallText
                else -> if (side == leftSide()) postseasonField?.wallText else postseasonField?.rightWallText
            }
        return perSide?.takeIf { it.isNotBlank() }
    }

    fun yardNumberFontFamily(): String =
        EndZoneFonts.familyOf(homeField?.yardNumberFont ?: bowlField?.yardNumberFont ?: postseasonField?.yardNumberFont)

    fun midfieldLogoScale(): Float = (homeField?.midfieldLogoSize ?: 1.0).toFloat()

    private fun homeFieldEndZoneFill(): Color? = homeField?.endZoneColor?.let { FieldBackgroundPainter.parseColor(it) }

    private fun outlineOf(
        textColor: Color,
        outlineColor: Color,
    ): Color = if (isWhite(outlineColor) && isWhite(textColor)) Color.BLACK else outlineColor

    private fun isWhite(color: Color): Boolean = ColorSimilarity.areSimilar(color, FieldBackgroundPainter.LINE_COLOR)

    private fun onGrass(
        team: Team,
        primary: Color,
        secondary: Color,
        logoUrl: String?,
        logoScale: Float = 0f,
        customText: String? = null,
    ): EndZoneDecoration {
        val text = listOf(primary, secondary).firstOrNull { !ColorSimilarity.areSimilar(it, turf) } ?: FieldBackgroundPainter.LINE_COLOR
        val outline = listOf(secondary, primary).firstOrNull { it != text && !ColorSimilarity.areSimilar(it, turf) } ?: Color.BLACK
        return EndZoneDecoration(team, null, text, outlineOf(text, outline), logoUrl, customText, logoScale)
    }

    private fun postseasonEndZoneText(side: TeamSide): String? {
        val field = postseasonField ?: return null
        return (if (side == leftSide()) field.leftEndZoneText else field.rightEndZoneText)?.takeIf { it.isNotBlank() }
    }

    private fun postseasonEndZoneLogo(side: TeamSide): String? {
        val field = postseasonField ?: return null
        val isLeft = side == leftSide()
        val source = if (isLeft) field.leftEndZoneLogoSource else field.rightEndZoneLogoSource
        val url = (if (isLeft) field.leftEndZoneLogoUrl else field.rightEndZoneLogoUrl)?.takeIf { it.isNotBlank() }
        val team = teamOf(side)
        return when (LogoSource.from(source)) {
            LogoSource.PRIMARY -> team.logo
            LogoSource.SECONDARY -> team.secondaryLogo
            LogoSource.CUSTOM -> url
            LogoSource.NONE -> null
        }
    }

    private fun homeFieldEndZoneLogo(): String? {
        val field = homeField ?: return null
        if (!field.endZoneLogoEnabled) return null
        return when (LogoSource.from(field.endZoneLogoSource)) {
            LogoSource.PRIMARY -> homeTeam.logo ?: homeTeam.scorebugLogo
            LogoSource.SECONDARY -> homeTeam.secondaryLogo
            LogoSource.CUSTOM -> field.endZoneLogoUrl
            LogoSource.NONE -> null
        }
    }

    private fun homeFieldEndZoneLogoScale(): Float = (homeField?.endZoneLogoSize ?: 0.0).toFloat()

    companion object {
        private const val POSTSEASON_END_ZONE_LOGO_SCALE = 0.9f
        private const val PLAYOFF_END_ZONE_LOGO_SCALE = 0.8f
        private const val HALF_FIELD = 50
    }
}
