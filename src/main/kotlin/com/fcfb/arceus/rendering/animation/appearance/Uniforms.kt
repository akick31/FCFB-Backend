package com.fcfb.arceus.rendering.animation.appearance

import com.fcfb.arceus.enums.animation.HelmetLogoMode
import com.fcfb.arceus.enums.animation.StripeType
import com.fcfb.arceus.model.animation.Uniform
import com.fcfb.arceus.model.appearance.TeamUniformHistory
import com.fcfb.arceus.model.team.Team
import java.awt.Color

class Uniforms {
    fun forMatchup(
        homeTeam: Team,
        awayTeam: Team,
        homeSnapshot: TeamUniformHistory? = null,
        awaySnapshot: TeamUniformHistory? = null,
    ): Pair<Uniform, Uniform> {
        val (homeHelmet, awayHelmet) = HelmetColors.forMatchup(homeTeam, awayTeam, homeSnapshot, awaySnapshot)
        val awayAlt = HelmetColors.awayUsesAlternate(homeTeam, awayTeam, homeSnapshot, awaySnapshot)
        val home =
            Uniform(
                jersey = jersey(homeTeam, homeSnapshot),
                number = numberColor(homeSnapshot),
                helmet = homeHelmet,
                pants = pants(homeTeam, homeSnapshot),
                numberOutline = numberOutline(homeSnapshot),
                helmetNumberFont = homeSnapshot?.helmetNumberFont,
                jerseyNumberFont = homeSnapshot?.jerseyNumberFont,
                numberTopText = homeSnapshot?.numberTopText,
            ).withHelmet(homeTeam, homeSnapshot, alt = false)
        val away =
            Uniform(
                jersey = Color.WHITE,
                number = awayNumber(awayTeam, awaySnapshot),
                helmet = awayHelmet,
                pants = awayPants(awayTeam, awaySnapshot),
                numberOutline = awayNumberOutline(awaySnapshot),
                helmetNumberFont = awaySnapshot?.helmetNumberFont,
                jerseyNumberFont = awaySnapshot?.jerseyNumberFont,
                numberTopText = awaySnapshot?.awayNumberTopText ?: awaySnapshot?.numberTopText,
            ).withHelmet(awayTeam, awaySnapshot, alt = awayAlt)
        return home to away
    }

    fun awayJerseyPreview(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Uniform =
        secondaryHelmet(team, snapshot).copy(
            jersey = Color.WHITE,
            number = awayNumber(team, snapshot),
            numberOutline = awayNumberOutline(snapshot),
            pants = awayPants(team, snapshot),
            numberTopText = snapshot?.awayNumberTopText ?: snapshot?.numberTopText,
        )

    fun secondaryHelmet(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Uniform {
        val shell = HelmetColors.forMatchup(team, team, snapshot, snapshot).second
        return Uniform(
            jersey = jersey(team, snapshot),
            number = numberColor(snapshot),
            helmet = shell,
            pants = pants(team, snapshot),
            numberOutline = numberOutline(snapshot),
            helmetNumberFont = snapshot?.helmetNumberFont,
            jerseyNumberFont = snapshot?.jerseyNumberFont,
            numberTopText = snapshot?.numberTopText,
        ).withHelmet(team, snapshot, alt = true)
    }

    private fun Uniform.withHelmet(
        team: Team,
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Uniform =
        copy(
            facemask = facemask(snapshot, alt),
            stripe = stripe(team, snapshot, alt),
            stripeType = StripeType.from(HelmetFields().stripeType(snapshot, alt)),
            outerStripe = outerStripe(team, snapshot, alt),
            helmetNumber = helmetNumber(snapshot, alt),
            helmetLogoMode = HelmetLogoMode.from(HelmetFields().helmetLogoMode(snapshot, alt), HelmetFields().hasLogo(snapshot, alt)),
            logoSize = HelmetFields().logoSize(snapshot, alt).toFloat(),
            logoX = HelmetFields().logoX(snapshot, alt).toFloat(),
            logoY = HelmetFields().logoY(snapshot, alt).toFloat(),
            logoRotation = HelmetFields().logoRotation(snapshot, alt).toFloat(),
        )

    private fun jersey(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = FieldColors.parseColor(snapshot?.jerseyColor ?: team.primaryColor)

    private fun pants(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = FieldColors.parseColor(snapshot?.pantsColor ?: team.primaryColor)

    private fun awayPants(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = FieldColors.parseColor(snapshot?.awayPantsColor ?: snapshot?.pantsColor ?: team.primaryColor)

    private fun awayNumber(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = snapshot?.awayNumberColor?.let { FieldColors.parseColor(it) } ?: jersey(team, snapshot)

    private fun awayNumberOutline(snapshot: TeamUniformHistory?): Color? =
        snapshot?.awayNumberOutlineColor?.let { FieldColors.parseColor(it) }

    private fun numberColor(snapshot: TeamUniformHistory?): Color = snapshot?.numberColor?.let { FieldColors.parseColor(it) } ?: Color.WHITE

    private fun numberOutline(snapshot: TeamUniformHistory?): Color? = snapshot?.numberOutlineColor?.let { FieldColors.parseColor(it) }

    private fun helmetNumber(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Color = HelmetFields().helmetNumberColor(snapshot, alt)?.let { FieldColors.parseColor(it) } ?: Color.WHITE

    private fun facemask(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Color = HelmetFields().facemaskColor(snapshot, alt)?.let { FieldColors.parseColor(it) } ?: Color.WHITE

    private fun stripe(
        team: Team,
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Color? {
        if (!HelmetFields().hasStripe(snapshot, alt)) return null
        return FieldColors.parseColor(HelmetFields().stripeColor(snapshot, alt) ?: team.secondaryColor)
    }

    private fun outerStripe(
        team: Team,
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Color? {
        if (!HelmetFields().hasStripe(snapshot, alt)) return null
        return FieldColors.parseColor(HelmetFields().secondaryStripeColor(snapshot, alt) ?: team.secondaryColor)
    }
}
