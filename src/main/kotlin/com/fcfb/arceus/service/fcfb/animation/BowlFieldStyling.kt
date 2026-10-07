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
        val source = if (isHome) field.leftEndZoneLogoSource else field.rightEndZoneLogoSource
        val url = if (isHome) field.leftEndZoneLogoUrl else field.rightEndZoneLogoUrl
        val logo = resolveLogo(source, url, team)
        val text = (if (isHome) field.leftEndZoneText else field.rightEndZoneText)?.takeIf { it.isNotBlank() }
        return when (EndZoneFill.from(field.endZoneFill)) {
            EndZoneFill.PRIMARY -> filled(team, primary, secondary, logo, text)
            EndZoneFill.SECONDARY -> filled(team, secondary, primary, logo, text)
            EndZoneFill.NONE -> onTurf(team, primary, secondary, logo, text)
        }
    }

    private fun resolveLogo(
        source: String?,
        url: String?,
        team: Team,
    ): String? =
        when (LogoSource.from(source)) {
            LogoSource.PRIMARY -> team.logo
            LogoSource.SECONDARY -> team.secondaryLogo
            LogoSource.CUSTOM -> url?.takeIf { it.isNotBlank() }
            LogoSource.NONE -> null
        }

    fun yardNumberOutline(
        yard: Int,
        top: Boolean,
        leftTeam: Team,
        rightTeam: Team,
    ): Color? {
        if (YardNumberSource.from(field.yardNumberSource) == YardNumberSource.FIXED) {
            return token(field.yardNumberOutlineColor, homeTeam)
        }
        val owner =
            when {
                yard < MIDFIELD_YARD -> leftTeam
                yard > MIDFIELD_YARD -> rightTeam
                top -> homeTeam
                else -> awayTeam
            }
        return teamSlotColor(owner, field.yardNumberTeamSlot)
    }

    private fun teamSlotColor(
        team: Team,
        slot: String?,
    ): Color =
        when (slot?.uppercase()) {
            "SECONDARY" -> FieldBackgroundPainter.parseColor(team.secondaryColor)
            "TERTIARY" -> FieldBackgroundPainter.parseColor(team.tertiaryColor ?: team.secondaryColor)
            "PRIMARY" -> FieldBackgroundPainter.parseColor(team.primaryColor)
            else -> visibleTeamColor(team)
        }

    fun redZoneBorder(
        yard: Int,
        leftTeam: Team,
        rightTeam: Team,
    ): Color? {
        if (!field.redZoneEnabled) return null
        token(field.redZoneBorderColor, homeTeam)?.let { return it }
        return visibleTeamColor(if (yard < MIDFIELD_YARD) leftTeam else rightTeam)
    }

    fun sidelineAccent(homeEnd: Boolean): Color? =
        token(if (homeEnd) field.leftOobLineColor else field.rightOobLineColor, if (homeEnd) homeTeam else awayTeam)

    private fun token(
        value: String?,
        team: Team,
    ): Color? =
        ColorToken.resolve(
            value,
            FieldBackgroundPainter.parseColor(team.primaryColor),
            FieldBackgroundPainter.parseColor(team.secondaryColor),
            FieldBackgroundPainter.parseColor(team.tertiaryColor ?: team.secondaryColor),
        )

    private fun filled(
        team: Team,
        fill: Color,
        outline: Color,
        logo: String?,
        customText: String?,
    ): EndZoneDecoration =
        EndZoneDecoration(
            team,
            fill,
            FieldBackgroundPainter.LINE_COLOR,
            outlineOf(FieldBackgroundPainter.LINE_COLOR, outline),
            logo,
            customText,
        )

    private fun onTurf(
        team: Team,
        primary: Color,
        secondary: Color,
        logo: String?,
        customText: String?,
    ): EndZoneDecoration {
        val text = listOf(primary, secondary).firstOrNull { !ColorSimilarity.areSimilar(it, turf) } ?: FieldBackgroundPainter.LINE_COLOR
        val outline = listOf(secondary, primary).firstOrNull { it != text && !ColorSimilarity.areSimilar(it, turf) } ?: Color.BLACK
        return EndZoneDecoration(team, null, text, outlineOf(text, outline), logo, customText)
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
