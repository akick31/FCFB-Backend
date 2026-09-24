package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.BowlFieldRequest
import com.fcfb.arceus.model.BowlField
import com.fcfb.arceus.repositories.BowlFieldRepository
import com.fcfb.arceus.repositories.BowlRepository
import com.fcfb.arceus.service.fcfb.animation.ColorSimilarity
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.InvalidUniformException
import com.fcfb.arceus.util.UserForbiddenException
import org.springframework.stereotype.Service
import java.awt.Color

@Service
class BowlFieldService(
    private val bowlFieldRepository: BowlFieldRepository,
    private val bowlRepository: BowlRepository,
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
        request.wallColor?.let { field.wallColor = it }
        field.leftEndZoneLogoUrl = request.leftEndZoneLogoUrl
        field.rightEndZoneLogoUrl = request.rightEndZoneLogoUrl
        field.yardNumberOutlineColor = request.yardNumberOutlineColor
        field.leftOobLineColor = request.leftOobLineColor
        field.rightOobLineColor = request.rightOobLineColor
        field.redZoneBorderColor = request.redZoneBorderColor
        field.wallText = request.wallText
        field.wallTextOutlineColor = request.wallTextOutlineColor
        requireVisibleWall(field)
        return bowlFieldRepository.save(field)
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
