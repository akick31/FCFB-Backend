package com.fcfb.arceus.dto.response

data class EloTeamChange(
    val team: String,
    val storedElo: Double,
    val rebuiltElo: Double,
)
