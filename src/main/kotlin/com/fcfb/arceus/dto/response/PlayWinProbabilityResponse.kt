package com.fcfb.arceus.dto.response

data class PlayWinProbabilityResponse(
    val playNumber: Int,
    val quarter: Int,
    val clock: Int,
    val homeScore: Int,
    val awayScore: Int,
    val homeTeamWinProbability: Double,
    val awayTeamWinProbability: Double,
)
