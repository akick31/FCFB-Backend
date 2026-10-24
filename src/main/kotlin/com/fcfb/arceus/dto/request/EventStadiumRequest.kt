package com.fcfb.arceus.dto.request

import com.fcfb.arceus.enums.animation.PostseasonCategory

data class EventStadiumRequest(
    val category: PostseasonCategory,
    val key: String,
    val venue: String? = null,
)
