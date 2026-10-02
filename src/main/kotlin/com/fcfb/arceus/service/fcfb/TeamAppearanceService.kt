package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.TeamColorsRequest
import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.dto.request.TeamLogosRequest
import com.fcfb.arceus.dto.request.TeamUniformRequest
import com.fcfb.arceus.dto.response.TeamColorsResponse
import com.fcfb.arceus.dto.response.TeamLogosResponse
import com.fcfb.arceus.model.TeamField
import com.fcfb.arceus.model.TeamUniformCurrent
import com.fcfb.arceus.repositories.TeamFieldRepository
import com.fcfb.arceus.repositories.TeamUniformCurrentRepository
import com.fcfb.arceus.service.fcfb.animation.ColorSimilarity
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.InvalidUniformException
import com.fcfb.arceus.util.UserForbiddenException
import org.springframework.stereotype.Service

@Service
class TeamAppearanceService(
    private val teamUniformCurrentRepository: TeamUniformCurrentRepository,
    private val teamFieldRepository: TeamFieldRepository,
    private val teamService: TeamService,
    private val userService: UserService,
    private val fieldAppearanceApplier: FieldAppearanceApplier,
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
        if (request.primaryColor != null || request.secondaryColor != null) requireAdmin()
        if (request.tertiaryColor != null) requireCanEdit(team)
        request.primaryColor?.let { found.primaryColor = it }
        request.secondaryColor?.let { found.secondaryColor = it }
        request.tertiaryColor?.let { found.tertiaryColor = it }
        teamService.saveTeam(found)
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
        requireAdmin()
        val found = teamService.getTeamByName(team)
        found.logo = request.logo
        found.logoDark = request.logoDark
        found.secondaryLogo = request.secondaryLogo
        teamService.saveTeam(found)
        return TeamLogosResponse(found.logo, found.logoDark, found.secondaryLogo)
    }

    /** Primary and secondary are league identity, not a coach's to change; only admins and commissioners may. */
    private fun requireAdmin() {
        if (!AuthContext.isAdmin()) throw UserForbiddenException()
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
        request.logoUrl?.let { uniform.logoUrl = it }
        request.hasLogo?.let { uniform.hasLogo = it }
        request.hasStripe?.let { uniform.hasStripe = it }
        request.stripeColor?.let { uniform.stripeColor = it }
        request.stripeType?.let { uniform.stripeType = it }
        request.secondaryStripeColor?.let { uniform.secondaryStripeColor = it }
        request.jerseyColor?.let { uniform.jerseyColor = it }
        request.numberColor?.let { uniform.numberColor = it }
        request.numberOutlineColor?.let { uniform.numberOutlineColor = it }
        request.awayNumberColor?.let { uniform.awayNumberColor = it }
        request.awayNumberOutlineColor?.let { uniform.awayNumberOutlineColor = it }
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
        request.logoSize?.let { uniform.logoSize = it }
        request.logoX?.let { uniform.logoX = it }
        request.logoY?.let { uniform.logoY = it }
        request.logoRotation?.let { uniform.logoRotation = it }
        requireDistinctShells(uniform)
        return teamUniformCurrentRepository.save(uniform)
    }

    fun updateField(
        team: String,
        request: TeamFieldRequest,
    ): TeamField {
        requireCanEdit(team)
        val field = getField(team)
        fieldAppearanceApplier.apply(field, request)
        return teamFieldRepository.save(field)
    }

    /**
     * Two shells that read alike leave the teams indistinguishable once the away side falls back to its alternate,
     * which is the whole reason the alternate exists.
     */
    private fun requireDistinctShells(uniform: TeamUniformCurrent) {
        val alternate = uniform.secondaryHelmetColor ?: return
        val primary = uniform.helmetColor ?: return
        if (ColorSimilarity.areSimilar(FieldBackgroundPainter.parseColor(primary), FieldBackgroundPainter.parseColor(alternate))) {
            throw InvalidUniformException("The primary and secondary helmet colors are too similar to tell apart")
        }
    }

    /** Admins and commissioners may edit any team; everyone else only the teams they are currently coaching. */
    private fun requireCanEdit(team: String) {
        if (AuthContext.isAdmin()) return
        val userId = AuthContext.currentUserId() ?: throw UserForbiddenException()
        val user = userService.getUserById(userId)
        val target = teamService.getTeamByName(team)
        val byUsername = target.coachUsernames?.contains(user.username) == true
        val byDiscordId = user.discordId?.let { target.coachDiscordIds?.contains(it) } == true
        if (!byUsername && !byDiscordId) throw UserForbiddenException()
    }

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
