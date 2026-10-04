package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.model.FieldAppearance
import com.fcfb.arceus.service.fcfb.animation.ColorSimilarity
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.util.InvalidUniformException
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class FieldAppearanceApplier {
    fun <T : FieldAppearance> apply(
        field: T,
        request: TeamFieldRequest,
    ): T {
        request.turfColor?.let { field.turfColor = it }
        request.endZoneColor?.let { field.endZoneColor = it }
        field.endZoneTextColor = request.endZoneTextColor
        field.endZoneOutlineColor = request.endZoneOutlineColor
        field.endZoneTextLeft = request.endZoneTextLeft
        field.endZoneTextRight = request.endZoneTextRight
        request.endZoneLogoEnabled?.let { field.endZoneLogoEnabled = it }
        request.endZoneLogoSource?.let { field.endZoneLogoSource = it }
        field.endZoneLogoUrl = request.endZoneLogoUrl
        request.endZoneLogoSize?.let { field.endZoneLogoSize = it }
        request.endZoneOutlineEnabled?.let { field.endZoneOutlineEnabled = it }
        request.wallLogoSource?.let { field.wallLogoSource = it }
        field.wallLogoUrl = request.wallLogoUrl
        request.endZoneFont?.let { field.endZoneFont = it }
        request.midfieldLogoUrl?.let { field.midfieldLogoUrl = it }
        request.midfieldLogoSource?.let { field.midfieldLogoSource = it }
        request.quarterLogoUrl?.let { field.quarterLogoUrl = it }
        request.quarterLogoSource?.let { field.quarterLogoSource = it }
        request.wallDesign?.let { field.wallDesign = it }
        request.wallColor?.let { field.wallColor = it }
        request.goalPostColor?.let { field.goalPostColor = it }
        request.goalPostStyle?.let { field.goalPostStyle = it }
        field.fieldNumberOutlineColor = request.fieldNumberOutlineColor
        field.redZoneBorderColor = request.redZoneBorderColor
        field.oobLineColor = request.oobLineColor
        field.wallText = request.wallText
        field.wallTextOutlineColor = request.wallTextOutlineColor
        request.recolorConferenceLogo?.let { field.recolorConferenceLogo = it }
        request.midfieldLogoSize?.let { field.midfieldLogoSize = it }
        request.leftEndZoneFont?.let { field.leftEndZoneFont = it }
        request.rightEndZoneFont?.let { field.rightEndZoneFont = it }
        request.rightWallDesign?.let { field.rightWallDesign = it }
        field.rightWallText = request.rightWallText
        request.yardNumberFont?.let { field.yardNumberFont = it }
        field.conferenceLogoColorMap = request.conferenceLogoColorMap
        requireVisibleWall(field)
        return field
    }

    /** White would vanish against the net and the end zone lines, so it is not an allowed wall color. */
    private fun requireVisibleWall(field: FieldAppearance) {
        val wall = field.wallColor ?: return
        if (ColorSimilarity.areSimilar(FieldBackgroundPainter.parseColor(wall), Color.WHITE)) {
            throw InvalidUniformException("The wall color cannot be white")
        }
    }
}
