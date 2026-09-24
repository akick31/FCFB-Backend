package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.dto.request.TeamUniformRequest
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
import java.awt.Color

@Service
class TeamAppearanceService(
    private val teamUniformCurrentRepository: TeamUniformCurrentRepository,
    private val teamFieldRepository: TeamFieldRepository,
    private val teamService: TeamService,
    private val userService: UserService,
) {
    fun getUniform(team: String): TeamUniformCurrent = teamUniformCurrentRepository.findById(team).orElseGet { newUniform(team) }

    fun getField(team: String): TeamField = teamFieldRepository.findById(team).orElseGet { newField(team) }

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
        request.helmetNumberColor?.let { uniform.helmetNumberColor = it }
        request.logoUrl?.let { uniform.logoUrl = it }
        request.hasLogo?.let { uniform.hasLogo = it }
        request.hasStripe?.let { uniform.hasStripe = it }
        request.stripeColor?.let { uniform.stripeColor = it }
        request.jerseyColor?.let { uniform.jerseyColor = it }
        request.numberColor?.let { uniform.numberColor = it }
        request.numberOutlineColor?.let { uniform.numberOutlineColor = it }
        request.pantsColor?.let { uniform.pantsColor = it }
        requireDistinctShells(uniform)
        return teamUniformCurrentRepository.save(uniform)
    }

    fun updateField(
        team: String,
        request: TeamFieldRequest,
    ): TeamField {
        requireCanEdit(team)
        val field = getField(team)
        request.turfColor?.let { field.turfColor = it }
        request.endZoneColor?.let { field.endZoneColor = it }
        request.endZoneFont?.let { field.endZoneFont = it }
        request.midfieldLogoUrl?.let { field.midfieldLogoUrl = it }
        request.quarterLogoUrl?.let { field.quarterLogoUrl = it }
        request.wallDesign?.let { field.wallDesign = it }
        request.wallColor?.let { field.wallColor = it }
        request.goalPostColor?.let { field.goalPostColor = it }
        request.goalPostStyle?.let { field.goalPostStyle = it }
        field.fieldNumberOutlineColor = request.fieldNumberOutlineColor
        field.redZoneBorderColor = request.redZoneBorderColor
        field.oobLineColor = request.oobLineColor
        field.wallText = request.wallText
        field.wallTextOutlineColor = request.wallTextOutlineColor
        requireVisibleWall(field)
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

    /** White would vanish against the net and the end zone lines, so it is not an allowed wall color. */
    private fun requireVisibleWall(field: TeamField) {
        val wall = field.wallColor ?: return
        if (ColorSimilarity.areSimilar(FieldBackgroundPainter.parseColor(wall), Color.WHITE)) {
            throw InvalidUniformException("The wall color cannot be white")
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
