package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.domain.appearance.WallColorValidator
import com.fcfb.arceus.dto.request.PostseasonFieldRequest
import com.fcfb.arceus.model.appearance.PostseasonField
import com.fcfb.arceus.util.AuthContext
import org.springframework.stereotype.Component

@Component
class PostseasonFieldUpdateService {
    private val wallColorValidator = WallColorValidator()

    fun <T : PostseasonField> applyTo(
        field: T,
        request: PostseasonFieldRequest,
    ): T {
        AuthContext().requireAdminOrCommissioner()
        applyFields(field, request)
        wallColorValidator.requireVisible(field.wallColor)
        return field
    }

    fun <T : PostseasonField> applyFields(
        field: T,
        request: PostseasonFieldRequest,
    ): T {
        request.turfColor?.let { field.turfColor = it }
        request.endZoneFont?.let { field.endZoneFont = it }
        request.wallDesign?.let { field.wallDesign = it }
        request.goalPostColor?.let { field.goalPostColor = it }
        request.goalPostStyle?.let { field.goalPostStyle = it }
        field.centerLogoUrl = request.centerLogoUrl
        field.wallColor = request.wallColor
        field.wallText = request.wallText
        field.wallTextOutlineColor = request.wallTextOutlineColor
        field.wallTextFont = request.wallTextFont
        field.yardNumberOutlineColor = request.yardNumberOutlineColor
        field.redZoneBorderColor = request.redZoneBorderColor
        field.midfieldBorderColor = request.midfieldBorderColor
        field.sidelineAccentColor = request.sidelineAccentColor
        field.leftSidelineColor = request.leftSidelineColor
        field.rightSidelineColor = request.rightSidelineColor
        field.leftRedZoneColor = request.leftRedZoneColor
        field.rightRedZoneColor = request.rightRedZoneColor
        field.leftEndZoneText = request.leftEndZoneText
        field.rightEndZoneText = request.rightEndZoneText
        field.leftEndZoneLogoUrl = request.leftEndZoneLogoUrl
        field.rightEndZoneLogoUrl = request.rightEndZoneLogoUrl
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
        request.yardNumberSource?.let { field.yardNumberSource = it }
        field.yardNumberTeamSlot = request.yardNumberTeamSlot
        return field
    }
}
