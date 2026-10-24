package com.fcfb.arceus.dto.request

data class TeamStadiumRequest(
    val team: String,
    val venue: String,
    val buildNew: Boolean = false,
)
