package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Team
import java.awt.Color

/**
 * [flipped] is true when the home team is driving right to left, which happens in the second half and in overtime, where the
 * home team always defends the right end zone.
 */
data class FieldTheme(
    val style: FieldStyle,
    val homeTeam: Team,
    val awayTeam: Team,
    val centerLogoUrl: String?,
    val flipped: Boolean = false,
    val turf: Color = FieldBackgroundPainter.TURF_COLOR,
    val homeConferenceLogoUrl: String? = null,
    val awayConferenceLogoUrl: String? = null,
) {
    /** The team whose end zone sits on the left of the frame, which swaps when the field is flipped. */
    fun leftSide(): TeamSide = if (flipped) TeamSide.AWAY else TeamSide.HOME

    fun rightSide(): TeamSide = if (flipped) TeamSide.HOME else TeamSide.AWAY

    fun conferenceLogoOf(side: TeamSide): String? = if (side == TeamSide.HOME) homeConferenceLogoUrl else awayConferenceLogoUrl

    /** HOME defends the left end zone and AWAY the right; on a home field both end zones belong to the home team. */
    fun endZoneOf(side: TeamSide): EndZoneDecoration {
        val team = if (style == FieldStyle.HOME_FIELD || side == TeamSide.HOME) homeTeam else awayTeam
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        val secondary = FieldBackgroundPainter.parseColor(team.secondaryColor)
        val override = TeamFieldOverrides.forTeam(homeTeam.name)
        return when (style) {
            FieldStyle.HOME_FIELD ->
                EndZoneDecoration(team, override?.endZoneFill ?: primary, FieldBackgroundPainter.LINE_COLOR, secondary, null)
            FieldStyle.BOWL -> EndZoneDecoration(team, primary, FieldBackgroundPainter.LINE_COLOR, secondary, null)
            FieldStyle.PLAYOFF -> onGrass(team, primary, secondary, centerLogoUrl)
            FieldStyle.CONFERENCE_CHAMPIONSHIP -> onGrass(team, primary, secondary, null)
        }
    }

    /** Names painted on grass use the primary color, then the secondary, then white, skipping any color that would blend in. */
    private fun onGrass(
        team: Team,
        primary: Color,
        secondary: Color,
        logoUrl: String?,
    ): EndZoneDecoration {
        val text = listOf(primary, secondary).firstOrNull { !ColorSimilarity.areSimilar(it, turf) } ?: FieldBackgroundPainter.LINE_COLOR
        val outline = listOf(secondary, primary).firstOrNull { it != text && !ColorSimilarity.areSimilar(it, turf) } ?: Color.BLACK
        return EndZoneDecoration(team, null, text, outline, logoUrl)
    }
}
