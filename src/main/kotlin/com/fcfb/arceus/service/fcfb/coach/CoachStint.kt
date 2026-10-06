package com.fcfb.arceus.service.fcfb.coach

import java.time.LocalDateTime

data class CoachStint(
    val team: String,
    var start: LocalDateTime,
    val end: LocalDateTime?,
    val interim: Boolean,
)
