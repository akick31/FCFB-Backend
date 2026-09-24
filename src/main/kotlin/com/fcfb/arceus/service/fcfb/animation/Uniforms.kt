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
        val home =
            Uniform(
                jersey = jersey(homeTeam, homeSnapshot),
                number = numberColor(homeSnapshot),
                helmet = homeHelmet,
                pants = pants(homeTeam, homeSnapshot),
                facemask = facemask(homeSnapshot),
                numberOutline = numberOutline(homeSnapshot),
                stripe = stripe(homeTeam, homeSnapshot),
                helmetNumber = helmetNumber(homeSnapshot),
                helmetLogoMode = logoMode(homeSnapshot),
            )
        val away =
            Uniform(
                jersey = Color.WHITE,
                number = jersey(awayTeam, awaySnapshot),
                helmet = awayHelmet,
                pants = pants(awayTeam, awaySnapshot),
                facemask = facemask(awaySnapshot),
                numberOutline = numberOutline(awaySnapshot),
                stripe = stripe(awayTeam, awaySnapshot),
                helmetNumber = helmetNumber(awaySnapshot),
                helmetLogoMode = logoMode(awaySnapshot),
            )
        return home to away
    }

    private fun jersey(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = FieldBackgroundPainter.parseColor(snapshot?.jerseyColor ?: team.primaryColor)

    private fun pants(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = FieldBackgroundPainter.parseColor(snapshot?.pantsColor ?: team.primaryColor)

    /** The road team wears white, so its numbers stay the team color; only the home set honors a configured number color. */
    private fun numberColor(snapshot: TeamUniformHistory?): Color =
        snapshot?.numberColor?.let { FieldBackgroundPainter.parseColor(it) } ?: Color.WHITE

    private fun numberOutline(snapshot: TeamUniformHistory?): Color? =
        snapshot?.numberOutlineColor?.let { FieldBackgroundPainter.parseColor(it) }

    private fun helmetNumber(snapshot: TeamUniformHistory?): Color =
        snapshot?.helmetNumberColor?.let { FieldBackgroundPainter.parseColor(it) } ?: Color.WHITE

    private fun facemask(snapshot: TeamUniformHistory?): Color =
        snapshot?.facemaskColor?.let { FieldBackgroundPainter.parseColor(it) } ?: Color.WHITE

    private fun stripe(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color? {
        if (snapshot?.hasStripe != true) return null
        return FieldBackgroundPainter.parseColor(snapshot.stripeColor ?: team.secondaryColor)
    }

    private fun logoMode(snapshot: TeamUniformHistory?): HelmetLogoMode =
        HelmetLogoMode.from(snapshot?.helmetLogoMode, snapshot?.hasLogo ?: true)
}
