package com.fcfb.arceus.dto.response

data class SinglePlayWinProbabilityResponse(
    val playId: Int,
    val playNumber: Int,
    val quarter: Int,
    val clock: Int,
    val homeScore: Int,
    val awayScore: Int,
    val winProbability: Double,
    val winProbabilityAdded: Double,
    val possession: String,
    val possessionTeam: String,
    val ballLocation: Int,
    val down: Int,
    val distance: Int,
    val playCall: String?,
    val homeElo: Double,
    val awayElo: Double,
)
