package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.model.appearance.TeamUniformCurrent
import com.fcfb.arceus.model.appearance.TeamUniformHistory
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.rendering.animation.appearance.HelmetColors
import com.fcfb.arceus.repositories.appearance.TeamUniformCurrentRepository
import com.fcfb.arceus.repositories.appearance.TeamUniformHistoryRepository
import com.fcfb.arceus.service.fcfb.team.TeamService
import org.springframework.stereotype.Service
import java.awt.Color

@Service
class TeamUniformService(
    private val teamUniformHistoryRepository: TeamUniformHistoryRepository,
    private val teamUniformCurrentRepository: TeamUniformCurrentRepository,
    private val teamService: TeamService,
) {
    fun snapshot(game: Game) {
        val season = game.season ?: return
        val week = game.week ?: return
        listOf(game.homeTeam, game.awayTeam).forEach { capture(it, season, week) }
    }

    fun uniformFor(
        team: String,
        season: Int?,
        week: Int?,
    ): TeamUniformHistory? {
        if (season == null || week == null) return null
        return teamUniformHistoryRepository.findByTeamAndSeasonNumberAndWeek(team, season, week)
    }

    private fun capture(
        team: String,
        season: Int,
        week: Int,
    ) {
        if (teamUniformHistoryRepository.findByTeamAndSeasonNumberAndWeek(team, season, week) != null) return
        teamUniformHistoryRepository.save(uniformOf(teamService.getTeamByName(team), season, week))
    }

    private fun uniformOf(
        team: Team,
        season: Int,
        week: Int,
    ): TeamUniformHistory {
        val name = team.name.orEmpty()
        val current = teamUniformCurrentRepository.findById(name).orElse(null)
        return TeamUniformHistory(
            team = name,
            seasonNumber = season,
            week = week,
            primaryColor = current?.primaryColor ?: team.primaryColor,
            secondaryColor = current?.secondaryColor ?: team.secondaryColor,
            tertiaryColor = current?.tertiaryColor,
            helmetColor = current?.helmetColor ?: hex(HelmetColors.shellColor(team)),
            secondaryHelmetColor = current?.secondaryHelmetColor,
            helmetNumberColor = current?.helmetNumberColor ?: TeamUniformCurrent.DEFAULT_HELMET_NUMBER_COLOR,
            facemaskColor = current?.facemaskColor ?: FACEMASK_COLOR,
            helmetLogoMode = current?.helmetLogoMode ?: TeamUniformCurrent.DEFAULT_HELMET_LOGO_MODE,
            helmetLogoSource = current?.helmetLogoSource ?: TeamUniformCurrent.DEFAULT_LOGO_SOURCE,
            jerseyColor = current?.jerseyColor ?: team.primaryColor,
            numberColor = current?.numberColor ?: NUMBER_COLOR,
            numberOutlineColor = current?.numberOutlineColor,
            awayNumberColor = current?.awayNumberColor,
            awayNumberOutlineColor = current?.awayNumberOutlineColor,
            pantsColor = current?.pantsColor ?: team.primaryColor,
            awayPantsColor = current?.awayPantsColor,
            logoUrl = current?.logoUrl ?: team.scorebugLogo,
            hasLogo = current?.hasLogo ?: true,
            hasStripe = current?.hasStripe ?: false,
            stripeColor = current?.stripeColor,
            stripeType = current?.stripeType ?: TeamUniformCurrent.DEFAULT_STRIPE_TYPE,
            secondaryStripeColor = current?.secondaryStripeColor,
            logoSize = current?.logoSize ?: TeamUniformCurrent.DEFAULT_LOGO_SIZE,
            logoX = current?.logoX ?: 0.0,
            logoY = current?.logoY ?: 0.0,
            logoRotation = current?.logoRotation ?: 0.0,
            helmetNumberFont = current?.helmetNumberFont,
            jerseyNumberFont = current?.jerseyNumberFont,
            numberTopText = current?.numberTopText,
            awayNumberTopText = current?.awayNumberTopText,
        ).apply {
            altFacemaskColor = current?.altFacemaskColor
            altHelmetNumberColor = current?.altHelmetNumberColor
            altHelmetLogoMode = current?.altHelmetLogoMode
            altHelmetLogoSource = current?.altHelmetLogoSource
            altHasLogo = current?.altHasLogo
            altLogoUrl = current?.altLogoUrl
            altLogoSize = current?.altLogoSize
            altLogoX = current?.altLogoX
            altLogoY = current?.altLogoY
            altLogoRotation = current?.altLogoRotation
            altHasStripe = current?.altHasStripe
            altStripeColor = current?.altStripeColor
            altStripeType = current?.altStripeType
            altSecondaryStripeColor = current?.altSecondaryStripeColor
        }
    }

    private fun hex(color: Color): String = String.format("#%06X", color.rgb and 0xFFFFFF)

    companion object {
        private const val FACEMASK_COLOR = "#FFFFFF"
        private const val NUMBER_COLOR = "#FFFFFF"
    }
}
