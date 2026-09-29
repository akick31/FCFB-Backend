package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.BowlField
import com.fcfb.arceus.model.Team
import java.awt.Color

/** Turns a bowl's stored settings into paint decisions. Left settings belong to the home team, right to the away team. */
class BowlFieldStyling(
    private val field: BowlField,
    private val homeTeam: Team,
    private val awayTeam: Team,
    private val turf: Color,
) {
    fun endZoneOf(
        team: Team,
        isHome: Boolean,
    ): EndZoneDecoration {
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        val secondary = FieldBackgroundPainter.parseColor(team.secondaryColor)
        val logo = if (isHome) field.leftEndZoneLogoUrl else field.rightEndZoneLogoUrl
        return when (EndZoneFill.from(field.endZoneFill)) {
            EndZoneFill.PRIMARY -> filled(team, primary, secondary, logo)
            EndZoneFill.SECONDARY -> filled(team, secondary, primary, logo)
            EndZoneFill.NONE -> onTurf(team, primary, secondary, logo)
        }
    }

    fun yardNumberOutline(
        yard: Int,
        top: Boolean,
        leftTeam: Team,
        rightTeam: Team,
    ): Color? {
        if (YardNumberSource.from(field.yardNumberSource) == YardNumberSource.FIXED) {
            return field.yardNumberOutlineColor?.let { FieldBackgroundPainter.parseColor(it) }
        }
        val owner =
            when {
                yard < MIDFIELD_YARD -> leftTeam
                yard > MIDFIELD_YARD -> rightTeam
                top -> homeTeam
                else -> awayTeam
            }
        return visibleTeamColor(owner)
    }

    fun redZoneBorder(
        yard: Int,
        leftTeam: Team,
        rightTeam: Team,
    ): Color? {
        if (!field.redZoneEnabled) return null
        field.redZoneBorderColor?.let { return FieldBackgroundPainter.parseColor(it) }
        return visibleTeamColor(if (yard < MIDFIELD_YARD) leftTeam else rightTeam)
    }

    fun sidelineAccent(homeEnd: Boolean): Color? =
        (if (homeEnd) field.leftOobLineColor else field.rightOobLineColor)?.let { FieldBackgroundPainter.parseColor(it) }

    private fun filled(
        team: Team,
        fill: Color,
        outline: Color,
        logo: String?,
    ): EndZoneDecoration =
        EndZoneDecoration(team, fill, FieldBackgroundPainter.LINE_COLOR, outlineOf(FieldBackgroundPainter.LINE_COLOR, outline), logo)

    private fun onTurf(
        team: Team,
        primary: Color,
        secondary: Color,
        logo: String?,
    ): EndZoneDecoration {
        val text = listOf(primary, secondary).firstOrNull { !ColorSimilarity.areSimilar(it, turf) } ?: FieldBackgroundPainter.LINE_COLOR
        val outline = listOf(secondary, primary).firstOrNull { it != text && !ColorSimilarity.areSimilar(it, turf) } ?: Color.BLACK
        return EndZoneDecoration(team, null, text, outlineOf(text, outline), logo)
    }

    private fun outlineOf(
        textColor: Color,
        outlineColor: Color,
    ): Color =
        if (ColorSimilarity.areSimilar(outlineColor, FieldBackgroundPainter.LINE_COLOR) &&
            ColorSimilarity.areSimilar(textColor, FieldBackgroundPainter.LINE_COLOR)
        ) {
            Color.BLACK
        } else {
            outlineColor
        }

    private fun visibleTeamColor(team: Team): Color {
        val primary = FieldBackgroundPainter.parseColor(team.primaryColor)
        return if (ColorSimilarity.areSimilar(primary, turf)) FieldBackgroundPainter.parseColor(team.secondaryColor) else primary
    }

    companion object {
        private const val MIDFIELD_YARD = 50
    }
}
