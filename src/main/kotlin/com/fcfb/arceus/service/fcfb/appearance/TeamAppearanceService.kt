package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.TeamColorsRequest
import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.dto.request.TeamLogosRequest
import com.fcfb.arceus.dto.request.TeamUniformRequest
import com.fcfb.arceus.dto.response.TeamColorsResponse
import com.fcfb.arceus.dto.response.TeamLogosResponse
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.model.appearance.TeamField
import com.fcfb.arceus.model.appearance.TeamUniformCurrent
import com.fcfb.arceus.rendering.animation.appearance.ColorSimilarity
import com.fcfb.arceus.rendering.animation.appearance.FieldColors
import com.fcfb.arceus.rendering.animation.assets.ConferenceLogoTint
import com.fcfb.arceus.repositories.appearance.TeamFieldRepository
import com.fcfb.arceus.repositories.appearance.TeamUniformCurrentRepository
import com.fcfb.arceus.repositories.conference.ConferenceRepository
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.appearance.AppearanceThumbnailCache
import org.springframework.stereotype.Service

@Service
class TeamAppearanceService(
    private val teamUniformCurrentRepository: TeamUniformCurrentRepository,
    private val teamFieldRepository: TeamFieldRepository,
    private val teamService: TeamService,
    private val fieldAppearanceService: FieldAppearanceService,
    private val conferenceRepository: ConferenceRepository,
    private val thumbnailCache: AppearanceThumbnailCache,
    private val teamEditPermissionService: TeamEditPermissionService,
) {
    fun getUniform(team: String): TeamUniformCurrent = teamUniformCurrentRepository.findById(team).orElseGet { newUniform(team) }

    fun getField(team: String): TeamField = teamFieldRepository.findById(team).orElseGet { newField(team) }

    fun getColors(team: String): TeamColorsResponse {
        val found = teamService.getTeamByName(team)
        return TeamColorsResponse(found.primaryColor, found.secondaryColor, found.tertiaryColor)
    }

    fun updateColors(
        team: String,
        request: TeamColorsRequest,
    ): TeamColorsResponse {
        val found = teamService.getTeamByName(team)
        if (request.primaryColor != null || request.secondaryColor != null) AuthContext().requireAdminOrCommissioner()
        if (request.tertiaryColor != null) requireCanEdit(team)
        request.primaryColor?.let { found.primaryColor = it }
        request.secondaryColor?.let { found.secondaryColor = it }
        request.tertiaryColor?.let { found.tertiaryColor = it }
        teamService.saveTeam(found)
        thumbnailCache.evictTeam(team)
        return TeamColorsResponse(found.primaryColor, found.secondaryColor, found.tertiaryColor)
    }

    fun getLogos(team: String): TeamLogosResponse {
        val found = teamService.getTeamByName(team)
        return TeamLogosResponse(found.logo, found.logoDark, found.secondaryLogo)
    }

    fun updateLogos(
        team: String,
        request: TeamLogosRequest,
    ): TeamLogosResponse {
        AuthContext().requireAdminOrCommissioner()
        val found = teamService.getTeamByName(team)
        found.logo = request.logo
        found.logoDark = request.logoDark
        found.secondaryLogo = request.secondaryLogo
        teamService.saveTeam(found)
        thumbnailCache.evictTeam(team)
        return TeamLogosResponse(found.logo, found.logoDark, found.secondaryLogo)
    }

    fun updateUniform(
        team: String,
        request: TeamUniformRequest,
    ): TeamUniformCurrent {
        requireCanEdit(team)
        val uniform = getUniform(team)
        request.tertiaryColor?.let { uniform.tertiaryColor = it }
        request.helmetColor?.let { uniform.helmetColor = it }
        request.secondaryHelmetColor?.let { uniform.secondaryHelmetColor = it }
        request.facemaskColor?.let { uniform.facemaskColor = it }
        request.helmetLogoMode?.let { uniform.helmetLogoMode = it }
        request.helmetLogoSource?.let { uniform.helmetLogoSource = it }
        request.helmetNumberColor?.let { uniform.helmetNumberColor = it }
        uniform.helmetNumberFont = request.helmetNumberFont
        request.logoUrl?.let { uniform.logoUrl = it }
        request.hasLogo?.let { uniform.hasLogo = it }
        request.hasStripe?.let { uniform.hasStripe = it }
        request.stripeColor?.let { uniform.stripeColor = it }
        request.stripeType?.let { uniform.stripeType = it }
        request.secondaryStripeColor?.let { uniform.secondaryStripeColor = it }
        request.jerseyColor?.let { uniform.jerseyColor = it }
        request.numberColor?.let { uniform.numberColor = it }
        uniform.jerseyNumberFont = request.jerseyNumberFont
        uniform.numberOutlineColor = request.numberOutlineColor
        request.awayNumberColor?.let { uniform.awayNumberColor = it }
        uniform.awayNumberOutlineColor = request.awayNumberOutlineColor
        uniform.altFacemaskColor = request.altFacemaskColor
        uniform.altHelmetNumberColor = request.altHelmetNumberColor
        request.altHelmetLogoMode?.let { uniform.altHelmetLogoMode = it }
        request.altHelmetLogoSource?.let { uniform.altHelmetLogoSource = it }
        request.altHasLogo?.let { uniform.altHasLogo = it }
        uniform.altLogoUrl = request.altLogoUrl
        request.altLogoSize?.let { uniform.altLogoSize = it }
        request.altLogoX?.let { uniform.altLogoX = it }
        request.altLogoY?.let { uniform.altLogoY = it }
        request.altLogoRotation?.let { uniform.altLogoRotation = it }
        request.altHasStripe?.let { uniform.altHasStripe = it }
        uniform.altStripeColor = request.altStripeColor
        request.altStripeType?.let { uniform.altStripeType = it }
        uniform.altSecondaryStripeColor = request.altSecondaryStripeColor
        request.pantsColor?.let { uniform.pantsColor = it }
        uniform.awayPantsColor = request.awayPantsColor
        uniform.numberTopText = request.numberTopText
        uniform.awayNumberTopText = request.awayNumberTopText
        uniform.bumperColor = request.bumperColor
        request.logoSize?.let { uniform.logoSize = it }
        request.logoX?.let { uniform.logoX = it }
        request.logoY?.let { uniform.logoY = it }
        request.logoRotation?.let { uniform.logoRotation = it }
        requireDistinctShells(uniform)
        thumbnailCache.evictTeam(team)
        return teamUniformCurrentRepository.save(uniform)
    }

    fun getConferenceLogoColors(team: String): List<String> {
        val found = teamService.getTeamByName(team)
        val code = found.conference ?: return emptyList()
        val url = conferenceRepository.findById(code).orElse(null)?.logoUrl ?: return emptyList()
        return com.fcfb.arceus.rendering.animation.assets.ConferenceLogoTint.dominantColors(url)
    }

    fun updateField(
        team: String,
        request: TeamFieldRequest,
    ): TeamField {
        requireCanEdit(team)
        val field = getField(team)
        fieldAppearanceService.apply(field, request)
        thumbnailCache.evictTeam(team)
        return teamFieldRepository.save(field)
    }

    private fun requireDistinctShells(uniform: TeamUniformCurrent) {
        val alternate = uniform.secondaryHelmetColor ?: return
        val primary = uniform.helmetColor ?: return
        if (ColorSimilarity.areSimilar(FieldColors.parseColor(primary), FieldColors.parseColor(alternate))) {
            throw InvalidUniformException("The primary and secondary helmet colors are too similar to tell apart")
        }
    }

    private fun requireCanEdit(team: String) = teamEditPermissionService.requireCanEdit(team)

    private fun newUniform(team: String): TeamUniformCurrent {
        val existing = teamService.getTeamByName(team)
        return TeamUniformCurrent().apply {
            this.team = team
            primaryColor = existing.primaryColor
            secondaryColor = existing.secondaryColor
            jerseyColor = existing.primaryColor
            pantsColor = existing.primaryColor
            logoUrl = existing.scorebugLogo
        }
    }

    private fun newField(team: String): TeamField {
        val existing = teamService.getTeamByName(team)
        return TeamField().apply {
            this.team = team
            endZoneColor = existing.primaryColor
            midfieldLogoUrl = existing.scorebugLogo
            redZoneBorderColor = existing.primaryColor
            wallColor = existing.primaryColor
        }
    }
}
