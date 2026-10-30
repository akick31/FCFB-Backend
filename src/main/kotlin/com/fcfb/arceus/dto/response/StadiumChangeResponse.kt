package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.animation.StadiumChangeAction
import com.fcfb.arceus.model.appearance.StadiumSnapshot
import java.time.LocalDateTime

data class StadiumChangeResponse(
    val id: Long,
    val action: StadiumChangeAction,
    val summary: String?,
    val changedBy: String?,
    val changedAt: LocalDateTime,
    val before: StadiumSnapshot?,
)
