package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniformHistory
import java.awt.Color

object Uniforms {
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
            ).withHelmet(homeTeam, homeSnapshot, alt = false)
        val away =
            Uniform(
                jersey = Color.WHITE,
                number = awayNumber(awayTeam, awaySnapshot),
                helmet = awayHelmet,
                pants = pants(awayTeam, awaySnapshot),
                numberOutline = awayNumberOutline(awaySnapshot),
            ).withHelmet(awayTeam, awaySnapshot, alt = awayAlt)
        return home to away
    }

    /** The secondary helmet rendered on its own, for the appearance editor preview. */
    fun secondaryHelmet(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Uniform {
        val shell =
            snapshot?.secondaryHelmetColor?.let { FieldBackgroundPainter.parseColor(it) }
                ?: snapshot?.helmetColor?.let { FieldBackgroundPainter.parseColor(it) }
                ?: HelmetColors.shellColor(team)
        return Uniform(
            jersey = jersey(team, snapshot),
            number = numberColor(snapshot),
            helmet = shell,
            pants = pants(team, snapshot),
            numberOutline = numberOutline(snapshot),
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
            stripeType = StripeType.from(HelmetFields.stripeType(snapshot, alt)),
            outerStripe = outerStripe(team, snapshot, alt),
            helmetNumber = helmetNumber(snapshot, alt),
            helmetLogoMode = HelmetLogoMode.from(HelmetFields.helmetLogoMode(snapshot, alt), HelmetFields.hasLogo(snapshot, alt)),
            logoSize = HelmetFields.logoSize(snapshot, alt).toFloat(),
            logoX = HelmetFields.logoX(snapshot, alt).toFloat(),
            logoY = HelmetFields.logoY(snapshot, alt).toFloat(),
            logoRotation = HelmetFields.logoRotation(snapshot, alt).toFloat(),
        )

    private fun jersey(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = FieldBackgroundPainter.parseColor(snapshot?.jerseyColor ?: team.primaryColor)

    private fun pants(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = FieldBackgroundPainter.parseColor(snapshot?.pantsColor ?: team.primaryColor)

    /** The road team wears white, so its numbers stay the team color; only the home set honors a configured number color. */
    private fun awayNumber(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = snapshot?.awayNumberColor?.let { FieldBackgroundPainter.parseColor(it) } ?: jersey(team, snapshot)

    private fun awayNumberOutline(snapshot: TeamUniformHistory?): Color? =
        snapshot?.awayNumberOutlineColor?.let { FieldBackgroundPainter.parseColor(it) }

    private fun numberColor(snapshot: TeamUniformHistory?): Color =
        snapshot?.numberColor?.let { FieldBackgroundPainter.parseColor(it) } ?: Color.WHITE

    private fun numberOutline(snapshot: TeamUniformHistory?): Color? =
        snapshot?.numberOutlineColor?.let { FieldBackgroundPainter.parseColor(it) }

    private fun helmetNumber(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Color = HelmetFields.helmetNumberColor(snapshot, alt)?.let { FieldBackgroundPainter.parseColor(it) } ?: Color.WHITE

    private fun facemask(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Color = HelmetFields.facemaskColor(snapshot, alt)?.let { FieldBackgroundPainter.parseColor(it) } ?: Color.WHITE

    private fun stripe(
        team: Team,
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Color? {
        if (!HelmetFields.hasStripe(snapshot, alt)) return null
        return FieldBackgroundPainter.parseColor(HelmetFields.stripeColor(snapshot, alt) ?: team.secondaryColor)
    }

    private fun outerStripe(
        team: Team,
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Color? {
        if (!HelmetFields.hasStripe(snapshot, alt)) return null
        return FieldBackgroundPainter.parseColor(HelmetFields.secondaryStripeColor(snapshot, alt) ?: team.secondaryColor)
    }
}
