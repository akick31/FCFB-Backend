package com.fcfb.arceus.dto.response

data class GameSpreadResult(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeElo: Double,
    val awayElo: Double,
    val homeSpread: Double,
    val awaySpread: Double,
)
