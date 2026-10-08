package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamUniformHistory
import java.awt.Color

data class Uniform(
    val jersey: Color,
    val number: Color,
    val helmet: Color,
    val pants: Color,
    val numberOutline: Color?,
    val helmetNumberFont: String?,
    val jerseyNumberFont: String?,
    val shoulderStripe: Color?,
    val jerseyText: String?,
    val numberTopText: String? = null,
) {
    var facemask: Color = Color.WHITE
    var stripe: Color? = null
    var stripeType: StripeType = StripeType.SINGLE
    var outerStripe: Color? = null
    var helmetNumber: Color = Color.WHITE
    var helmetLogoMode: HelmetLogoMode = HelmetLogoMode.NONE
    var logoSize: Float = 0f
    var logoX: Float = 0f
    var logoY: Float = 0f
    var logoRotation: Float = 0f
}

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
                helmetNumberFont = homeSnapshot?.helmetNumberFont,
                jerseyNumberFont = homeSnapshot?.jerseyNumberFont,
                shoulderStripe = shoulderStripe(homeSnapshot),
                jerseyText = homeSnapshot?.jerseyText,
                numberTopText = numberTopText(homeSnapshot),
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
                shoulderStripe = shoulderStripe(awaySnapshot),
                jerseyText = awaySnapshot?.jerseyText,
                numberTopText = numberTopText(awaySnapshot),
            ).withHelmet(awayTeam, awaySnapshot, alt = awayAlt)
        return home to away
    }

    /**
     * The away (white) jersey paired with the team's secondary helmet, for the appearance editor preview only. A real
     * game picks the away helmet by clash in [forMatchup]; the editor always shows the secondary helmet here.
     */
    fun awayJerseyPreview(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Uniform =
        secondaryHelmet(team, snapshot).copy(
            jersey = Color.WHITE,
            number = awayNumber(team, snapshot),
            numberOutline = awayNumberOutline(snapshot),
            pants = awayPants(team, snapshot),
        )

    /** The secondary helmet rendered on its own, for the appearance editor preview. */
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
            shoulderStripe = shoulderStripe(snapshot),
            jerseyText = snapshot?.jerseyText,
            numberTopText = numberTopText(snapshot),
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

    private fun awayPants(
        team: Team,
        snapshot: TeamUniformHistory?,
    ): Color = FieldBackgroundPainter.parseColor(snapshot?.awayPantsColor ?: snapshot?.pantsColor ?: team.primaryColor)

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

    private fun shoulderStripe(snapshot: TeamUniformHistory?): Color? {
        if (snapshot?.hasShoulderStripe != true) return null
        return FieldBackgroundPainter.parseColor(snapshot.shoulderStripeColor)
    }

    private fun numberTopText(snapshot: TeamUniformHistory?): String? = snapshot?.numberTopText
}
