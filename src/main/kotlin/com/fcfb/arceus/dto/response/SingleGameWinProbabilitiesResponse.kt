package com.fcfb.arceus.dto.response

data class SingleGameWinProbabilitiesResponse(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val totalPlays: Int,
    val processedPlays: Int,
    val plays: List<SinglePlayWinProbabilityResponse>,
)
