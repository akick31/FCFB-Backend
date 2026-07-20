package com.fcfb.arceus.dto.response

data class GameWinProbabilitiesResponse(
    val gameId: Int,
    val totalPlays: Int,
    val plays: List<PlayWinProbabilityResponse>,
)
