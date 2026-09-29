package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.BowlField
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
    val bowlField: BowlField? = null,
) {
    private val bowlStyling: BowlFieldStyling? by lazy { bowlField?.let { BowlFieldStyling(it, homeTeam, awayTeam, turf) } }

    val hasCustomField: Boolean get() = homeField != null || bowlField != null

    fun endZoneFont(): String? = homeField?.endZoneFont ?: bowlField?.endZoneFont

    fun wallDesign(): String? = homeField?.wallDesign ?: bowlField?.wallDesign

    fun wallColor(): String? = homeField?.wallColor ?: bowlField?.wallColor

    fun wallTextOutlineColor(): String? = homeField?.wallTextOutlineColor ?: bowlField?.wallTextOutlineColor

    fun goalPostColor(): String? = homeField?.goalPostColor ?: bowlField?.goalPostColor

    fun goalPostStyle(): String? = homeField?.goalPostStyle ?: bowlField?.goalPostStyle

    fun yardNumberOutline(
        yard: Int,
        top: Boolean,
    ): Color? =
        bowlStyling?.yardNumberOutline(yard, top, teamOf(leftSide()), teamOf(rightSide()))
            ?: homeField?.fieldNumberOutlineColor?.let { FieldBackgroundPainter.parseColor(it) }

    fun redZoneBorder(yard: Int): Color? =
        bowlStyling?.redZoneBorder(yard, teamOf(leftSide()), teamOf(rightSide()))
            ?: homeField?.redZoneBorderColor?.let { FieldBackgroundPainter.parseColor(it) }

    fun sidelineAccent(physicalLeft: Boolean): Color? {
        val styling = bowlStyling ?: return homeField?.oobLineColor?.let { FieldBackgroundPainter.parseColor(it) }
        return styling.sidelineAccent(homeEnd = physicalLeft == (leftSide() == TeamSide.HOME))
    }

    private fun teamOf(side: TeamSide): Team = if (side == TeamSide.HOME) homeTeam else awayTeam

    fun uniforms(): Pair<Uniform, Uniform> = Uniforms.forMatchup(homeTeam, awayTeam, homeUniform, awayUniform)

    fun homeLogoUrl(): String? = homeUniform?.logoUrl ?: homeTeam.scorebugLogo

    fun awayLogoUrl(): String? = awayUniform?.logoUrl ?: awayTeam.scorebugLogo

    fun leftSide(): TeamSide = if (flipped) TeamSide.AWAY else TeamSide.HOME

    fun rightSide(): TeamSide = if (flipped) TeamSide.HOME else TeamSide.AWAY

    fun conferenceLogoOf(side: TeamSide): String? = if (side == TeamSide.HOME) homeConferenceLogoUrl else awayConferenceLogoUrl

    fun endZoneOf(side: TeamSide): EndZoneDecoration {
        val team = if (style == FieldStyle.HOME_FIELD || side == TeamSide.HOME) homeTeam else awayTeam
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        val secondary = FieldBackgroundPainter.parseColor(team.secondaryColor)
        return when (style) {
            FieldStyle.HOME_FIELD ->
                EndZoneDecoration(
                    team,
                    homeFieldEndZoneFill() ?: primary,
                    FieldBackgroundPainter.LINE_COLOR,
                    outlineOf(FieldBackgroundPainter.LINE_COLOR, secondary),
                    null,
                )
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
