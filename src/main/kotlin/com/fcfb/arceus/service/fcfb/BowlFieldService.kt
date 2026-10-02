package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.BowlFieldRequest
import com.fcfb.arceus.dto.request.BowlMetaRequest
import com.fcfb.arceus.model.BowlField
import com.fcfb.arceus.repositories.BowlFieldRepository
import com.fcfb.arceus.repositories.BowlRepository
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.service.fcfb.animation.ColorSimilarity
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.InvalidUniformException
import com.fcfb.arceus.util.UserForbiddenException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Color

@Service
class BowlFieldService(
    private val bowlFieldRepository: BowlFieldRepository,
    private val bowlRepository: BowlRepository,
    private val gameRepository: GameRepository,
) {
    fun getAll(): List<BowlField> = bowlFieldRepository.findAll().toList()

    fun getField(bowl: String): BowlField = bowlFieldRepository.findById(bowl).orElseGet { newField(bowl) }

    fun updateField(
        bowl: String,
        request: BowlFieldRequest,
    ): BowlField {
        requireAdmin()
        val field = getField(bowl)
        request.turfColor?.let { field.turfColor = it }
        request.endZoneFill?.let { field.endZoneFill = it }
        request.endZoneFont?.let { field.endZoneFont = it }
        request.showConferenceLogos?.let { field.showConferenceLogos = it }
        request.yardNumberSource?.let { field.yardNumberSource = it }
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
        field.yardNumberOutlineColor = request.yardNumberOutlineColor
        field.leftOobLineColor = request.leftOobLineColor
        field.rightOobLineColor = request.rightOobLineColor
        field.redZoneBorderColor = request.redZoneBorderColor
        field.wallColor = request.wallColor
        field.wallText = request.wallText
        field.wallTextOutlineColor = request.wallTextOutlineColor
        requireVisibleWall(field)
        return bowlFieldRepository.save(field)
    }

    @Transactional
    fun updateMeta(
        bowl: String,
        request: BowlMetaRequest,
    ): BowlField {
        requireAdmin()
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

    /** Bowls belong to no conference, so there is no ownership to scope: admins and commissioners both qualify. */
    private fun requireAdmin() {
        if (!AuthContext.isAdmin()) throw UserForbiddenException()
    }

    /** White would vanish against the net and the end zone lines, the same reason it is barred on home fields. */
    private fun requireVisibleWall(field: BowlField) {
        val wall = field.wallColor ?: return
        if (ColorSimilarity.areSimilar(FieldBackgroundPainter.parseColor(wall), Color.WHITE)) {
            throw InvalidUniformException("The wall color cannot be white")
        }
    }

    private fun newField(bowl: String): BowlField {
        val existing = bowlRepository.findById(bowl).orElseThrow { InvalidUniformException("No bowl named $bowl") }
        return BowlField().apply {
            this.bowl = existing.name
            wallText = existing.name.uppercase()
        }
    }
}
