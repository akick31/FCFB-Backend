package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.PostseasonFieldRequest
import com.fcfb.arceus.model.PostseasonField
import com.fcfb.arceus.service.fcfb.animation.ColorSimilarity
import com.fcfb.arceus.service.fcfb.animation.FieldBackgroundPainter
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.InvalidUniformException
import com.fcfb.arceus.util.UserForbiddenException
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class PostseasonFieldUpdater {
    fun <T : PostseasonField> applyTo(
        field: T,
        request: PostseasonFieldRequest,
    ): T {
        requireAdmin()
        request.turfColor?.let { field.turfColor = it }
        request.endZoneFont?.let { field.endZoneFont = it }
        request.wallDesign?.let { field.wallDesign = it }
        request.goalPostColor?.let { field.goalPostColor = it }
        request.goalPostStyle?.let { field.goalPostStyle = it }
        field.centerLogoUrl = request.centerLogoUrl
        field.wallColor = request.wallColor
        field.wallText = request.wallText
        field.wallTextOutlineColor = request.wallTextOutlineColor
        field.yardNumberOutlineColor = request.yardNumberOutlineColor
        field.redZoneBorderColor = request.redZoneBorderColor
        field.sidelineAccentColor = request.sidelineAccentColor
        requireVisibleWall(field)
        return field
    }

    /** Postseason games belong to no team, so there is no ownership to scope: admins and commissioners both qualify. */
    private fun requireAdmin() {
        if (!AuthContext.isAdmin()) throw UserForbiddenException()
    }

    /** White would vanish against the net and the end zone lines, the same reason it is barred on home fields. */
    private fun requireVisibleWall(field: PostseasonField) {
        val wall = field.wallColor ?: return
        if (ColorSimilarity.areSimilar(FieldBackgroundPainter.parseColor(wall), Color.WHITE)) {
            throw InvalidUniformException("The wall color cannot be white")
        }
    }
}
