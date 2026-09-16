package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Team
import java.awt.Color

data class EndZoneDecoration(
    val team: Team,
    val fill: Color?,
    val textColor: Color,
    val outlineColor: Color,
    val logoUrl: String?,
)
