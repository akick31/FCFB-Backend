package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.domain.appearance.WallColorValidator
import com.fcfb.arceus.dto.request.TeamFieldRequest
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.model.appearance.FieldAppearance
import org.springframework.stereotype.Component

@Component
class FieldAppearanceService {
    private val wallColorValidator = WallColorValidator()

    fun <T : FieldAppearance> apply(
        field: T,
        request: TeamFieldRequest,
    ): T {
        applyFields(field, request)
        wallColorValidator.requireVisible(field.wallColor)
        return field
    }

    fun <T : FieldAppearance> applyFields(
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
        field.midfieldBorderColor = request.midfieldBorderColor
        field.oobLineColor = request.oobLineColor
        field.wallText = request.wallText
        field.wallTextOutlineColor = request.wallTextOutlineColor
        field.wallTextFont = request.wallTextFont
        field.rightWallLogoSource = request.rightWallLogoSource
        field.rightWallLogoUrl = request.rightWallLogoUrl
        field.rightWallColor = request.rightWallColor
        field.rightWallTextOutlineColor = request.rightWallTextOutlineColor
        request.midfieldLogoSize?.let { field.midfieldLogoSize = it }
        request.conferenceLogoSize?.let { field.conferenceLogoSize = it }
        request.leftEndZoneFont?.let { field.leftEndZoneFont = it }
        request.rightEndZoneFont?.let { field.rightEndZoneFont = it }
        request.rightWallDesign?.let { field.rightWallDesign = it }
        field.rightWallText = request.rightWallText
        request.yardNumberFont?.let { field.yardNumberFont = it }
        request.yardNumberArrowAlign?.let { field.yardNumberArrowAlign = it }
        request.yardNumberOutlineWidth?.let { field.yardNumberOutlineWidth = it }
        request.endZoneOutlineWidth?.let { field.endZoneOutlineWidth = it }
        request.wallTextOutlineWidth?.let { field.wallTextOutlineWidth = it }
        request.stadiumBackdrop?.let { field.stadiumBackdrop = it }
        request.stadiumSize?.let { field.stadiumSize = it }
        field.rightStadiumBackdrop = request.rightStadiumBackdrop
        field.rightStadiumSize = request.rightStadiumSize
        request.jumbotronEnabled?.let { field.jumbotronEnabled = it }
        request.rightJumbotronEnabled?.let { field.rightJumbotronEnabled = it }
        request.jumbotronPylonMount?.let { field.jumbotronPylonMount = it }
        request.rightJumbotronPylonMount?.let { field.rightJumbotronPylonMount = it }
        field.conferenceLogoColorMap =
            request.conferenceLogoColorMap?.also {
                if (it.length > MAX_COLOR_MAP_LENGTH) throw InvalidUniformException("The conference logo color map is too large")
            }
        return field
    }

    companion object {
        private const val MAX_COLOR_MAP_LENGTH = 4000
    }
}
