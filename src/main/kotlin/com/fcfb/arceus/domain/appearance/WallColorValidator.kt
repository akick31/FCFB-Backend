package com.fcfb.arceus.domain.appearance

import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.rendering.animation.appearance.ColorSimilarity
import com.fcfb.arceus.rendering.animation.appearance.FieldColors
import java.awt.Color

class WallColorValidator {
    fun requireVisible(wallColor: String?) {
        wallColor ?: return
        if (ColorSimilarity.areSimilar(FieldColors.parseColor(wallColor), Color.WHITE)) {
            throw InvalidUniformException("The wall color cannot be white")
        }
    }
}
