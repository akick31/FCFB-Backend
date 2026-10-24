package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.domain.appearance.WallColorValidator
import com.fcfb.arceus.dto.request.BowlFieldRequest
import com.fcfb.arceus.dto.request.BowlMetaRequest
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.repositories.appearance.BowlFieldRepository
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.season.BowlRepository
import com.fcfb.arceus.util.AuthContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BowlFieldService(
    private val bowlFieldRepository: BowlFieldRepository,
    private val bowlRepository: BowlRepository,
    private val gameRepository: GameRepository,
) {
    private val wallColorValidator = WallColorValidator()

    fun getAll(): List<BowlField> = bowlFieldRepository.findAll().toList()

    fun getField(bowl: String): BowlField = bowlFieldRepository.findById(bowl).orElseGet { newField(bowl) }

    fun updateField(
        bowl: String,
        request: BowlFieldRequest,
    ): BowlField {
        AuthContext().requireAdminOrCommissioner()
        val field = getField(bowl)
        applyFields(field, request)
        wallColorValidator.requireVisible(field.wallColor)
        return bowlFieldRepository.save(field)
    }

    fun applyFields(
        field: BowlField,
        request: BowlFieldRequest,
    ) {
        request.turfColor?.let { field.turfColor = it }
        request.endZoneFill?.let { field.endZoneFill = it }
        request.endZoneFont?.let { field.endZoneFont = it }
        request.showConferenceLogos?.let { field.showConferenceLogos = it }
        request.yardNumberSource?.let { field.yardNumberSource = it }
        field.yardNumberTeamSlot = request.yardNumberTeamSlot
        request.redZoneEnabled?.let { field.redZoneEnabled = it }
        request.wallDesign?.let { field.wallDesign = it }
        request.goalPostColor?.let { field.goalPostColor = it }
        request.goalPostStyle?.let { field.goalPostStyle = it }
        field.leftEndZoneLogoUrl = request.leftEndZoneLogoUrl
        field.rightEndZoneLogoUrl = request.rightEndZoneLogoUrl
        field.leftEndZoneText = request.leftEndZoneText
        field.rightEndZoneText = request.rightEndZoneText
        request.leftEndZoneLogoSource?.let { field.leftEndZoneLogoSource = it }
        request.rightEndZoneLogoSource?.let { field.rightEndZoneLogoSource = it }
        request.leftEndZoneFont?.let { field.leftEndZoneFont = it }
        request.rightEndZoneFont?.let { field.rightEndZoneFont = it }
        request.rightWallDesign?.let { field.rightWallDesign = it }
        field.rightWallText = request.rightWallText
        request.yardNumberFont?.let { field.yardNumberFont = it }
        request.yardNumberArrowAlign?.let { field.yardNumberArrowAlign = it }
        request.yardNumberOutlineWidth?.let { field.yardNumberOutlineWidth = it }
        request.endZoneOutlineWidth?.let { field.endZoneOutlineWidth = it }
        request.wallTextOutlineWidth?.let { field.wallTextOutlineWidth = it }
        request.conferenceLogoSize?.let { field.conferenceLogoSize = it }
        field.yardNumberOutlineColor = request.yardNumberOutlineColor
        field.leftOobLineColor = request.leftOobLineColor
        field.rightOobLineColor = request.rightOobLineColor
        field.redZoneBorderColor = request.redZoneBorderColor
        field.midfieldBorderColor = request.midfieldBorderColor
        field.wallColor = request.wallColor
        field.wallText = request.wallText
        field.wallTextOutlineColor = request.wallTextOutlineColor
        field.wallTextFont = request.wallTextFont
        field.stadiumTeam = request.stadiumTeam?.takeIf { it.isNotBlank() }
    }

    @Transactional
    fun updateMeta(
        bowl: String,
        request: BowlMetaRequest,
    ): BowlField {
        AuthContext().requireAdminOrCommissioner()
        request.logo?.let { bowlRepository.updateLogo(bowl, it.trim().takeIf(String::isNotBlank)) }
        val finalName =
            request.name?.trim()?.takeIf { it.isNotBlank() && it != bowl }?.let { newName ->
                if (bowlRepository.findById(newName).isPresent) throw InvalidUniformException("A bowl named $newName already exists")
                bowlRepository.renameBowl(bowl, newName)
                bowlFieldRepository.renameBowl(bowl, newName)
                gameRepository.renameBowlGames(bowl, newName)
                newName
            } ?: bowl
        return getField(finalName)
    }

    private fun newField(bowl: String): BowlField {
        val existing = bowlRepository.findById(bowl).orElseThrow { InvalidUniformException("No bowl named $bowl") }
        return BowlField().apply {
            this.bowl = existing.name
            wallText = existing.name.uppercase()
        }
    }
}
