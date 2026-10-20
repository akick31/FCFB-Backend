package com.fcfb.arceus.model.animation

import com.fcfb.arceus.model.team.Team
import java.awt.Color

data class EndZoneDecoration(
    val team: Team,
    val fill: Color?,
    val textColor: Color,
    val outlineColor: Color,
    val logoUrl: String?,
    val text: String? = null,
    val logoScale: Float = 0f,
    val outlineEnabled: Boolean = true,
    val fontValue: String? = null,
    val wallDesign: String? = null,
    val wallText: String? = null,
    val wallColor: String? = null,
    val wallLogoUrl: String? = null,
    val wallLogoBlank: Boolean = false,
    val wallTextOutlineColor: String? = null,
)
