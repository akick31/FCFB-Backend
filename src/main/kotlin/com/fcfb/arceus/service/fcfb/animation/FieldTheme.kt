package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.BowlField
import com.fcfb.arceus.model.FieldAppearance
import com.fcfb.arceus.model.PostseasonField
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniformHistory
import java.awt.Color

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
    ): Color? =
        bowlStyling?.yardNumberOutline(yard, top, teamOf(leftSide()), teamOf(rightSide()))
            ?: parsed(homeField?.fieldNumberOutlineColor ?: postseasonField?.yardNumberOutlineColor)

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

    /** The helmet decal: an uploaded logo if the mode is UPLOAD, otherwise the team's primary or secondary logo. */
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

    fun conferenceLogoTint(): Color? =
        if (style == FieldStyle.HOME_FIELD && homeField?.recolorConferenceLogo == true) {
            FieldBackgroundPainter.parseColor(homeTeam.primaryColor)
        } else {
            null
        }

    fun endZoneOf(side: TeamSide): EndZoneDecoration {
        val team = if (style == FieldStyle.HOME_FIELD || side == TeamSide.HOME) homeTeam else awayTeam
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        val secondary = FieldBackgroundPainter.parseColor(team.secondaryColor)
        return when (style) {
            FieldStyle.HOME_FIELD -> {
                val text = homeField?.endZoneTextColor?.let { FieldBackgroundPainter.parseColor(it) } ?: FieldBackgroundPainter.LINE_COLOR
                val outline = homeField?.endZoneOutlineColor?.let { FieldBackgroundPainter.parseColor(it) } ?: outlineOf(text, secondary)
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
    }

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
