package com.fcfb.arceus.dto.response

data class EloRatingResponse(
    val teamId: Int,
    val teamName: String,
    val currentElo: Double,
    val overallElo: Double,
)
