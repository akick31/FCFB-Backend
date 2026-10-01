package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.BowlField
import com.fcfb.arceus.model.PostseasonField
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamField
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
    val homeField: TeamField? = null,
    val quarterLogoUrl: String? = null,
    val bowlField: BowlField? = null,
    val postseasonField: PostseasonField? = null,
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

    fun redZoneBorder(yard: Int): Color? =
        bowlStyling?.redZoneBorder(yard, teamOf(leftSide()), teamOf(rightSide()))
            ?: parsed(homeField?.redZoneBorderColor ?: postseasonField?.redZoneBorderColor)

    fun sidelineAccent(physicalLeft: Boolean): Color? {
        val styling = bowlStyling ?: return parsed(homeField?.oobLineColor ?: postseasonField?.sidelineAccentColor)
        return styling.sidelineAccent(homeEnd = physicalLeft == (leftSide() == TeamSide.HOME))
    }

    private fun parsed(hex: String?): Color? = hex?.let { FieldBackgroundPainter.parseColor(it) }

    private fun teamOf(side: TeamSide): Team = if (side == TeamSide.HOME) homeTeam else awayTeam

    fun uniforms(): Pair<Uniform, Uniform> = Uniforms.forMatchup(homeTeam, awayTeam, homeUniform, awayUniform)

    fun homeLogoUrl(): String? = helmetDecalUrl(homeUniform, homeTeam)

    fun awayLogoUrl(): String? = helmetDecalUrl(awayUniform, awayTeam)

    /** The helmet decal: an uploaded logo if the mode is UPLOAD, otherwise the team's primary or secondary logo. */
    private fun helmetDecalUrl(
        uniform: TeamUniformHistory?,
        team: Team,
    ): String? {
        val mode = HelmetLogoMode.from(uniform?.helmetLogoMode, uniform?.hasLogo ?: true)
        if (mode == HelmetLogoMode.UPLOAD) return uniform?.logoUrl
        return when (LogoSource.from(uniform?.helmetLogoSource)) {
            LogoSource.SECONDARY -> team.secondaryLogo
            else -> team.logo
        }
    }

    fun leftSide(): TeamSide = if (flipped) TeamSide.AWAY else TeamSide.HOME

    fun rightSide(): TeamSide = if (flipped) TeamSide.HOME else TeamSide.AWAY

    fun conferenceLogoOf(side: TeamSide): String? = if (side == TeamSide.HOME) homeConferenceLogoUrl else awayConferenceLogoUrl

    fun endZoneOf(side: TeamSide): EndZoneDecoration {
        val team = if (style == FieldStyle.HOME_FIELD || side == TeamSide.HOME) homeTeam else awayTeam
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        val secondary = FieldBackgroundPainter.parseColor(team.secondaryColor)
        return when (style) {
            FieldStyle.HOME_FIELD -> {
                val text = homeField?.endZoneTextColor?.let { FieldBackgroundPainter.parseColor(it) } ?: FieldBackgroundPainter.LINE_COLOR
                val outline = homeField?.endZoneOutlineColor?.let { FieldBackgroundPainter.parseColor(it) } ?: outlineOf(text, secondary)
                EndZoneDecoration(team, homeFieldEndZoneFill() ?: primary, text, outline, null)
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
            FieldStyle.PLAYOFF -> onGrass(team, primary, secondary, centerLogoUrl)
            FieldStyle.NATIONAL_CHAMPIONSHIP ->
                EndZoneDecoration(
                    team,
                    Color.BLACK,
                    FieldBackgroundPainter.LINE_COLOR,
                    outlineOf(FieldBackgroundPainter.LINE_COLOR, primary),
                    centerLogoUrl,
                )
            FieldStyle.CONFERENCE_CHAMPIONSHIP -> onGrass(team, primary, secondary, null)
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
    ): EndZoneDecoration {
        val text = listOf(primary, secondary).firstOrNull { !ColorSimilarity.areSimilar(it, turf) } ?: FieldBackgroundPainter.LINE_COLOR
        val outline = listOf(secondary, primary).firstOrNull { it != text && !ColorSimilarity.areSimilar(it, turf) } ?: Color.BLACK
        return EndZoneDecoration(team, null, text, outlineOf(text, outline), logoUrl)
    }
}
