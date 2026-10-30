package com.fcfb.arceus.dto.response

data class WinProbabilitySwingResponse(
    val gameId: Int,
    val playId: Int,
    val team: String,
    val opponent: String,
    val swing: Double,
    val quarter: Int,
    val clock: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int,
    val awayScore: Int,
    val result: String?,
)
