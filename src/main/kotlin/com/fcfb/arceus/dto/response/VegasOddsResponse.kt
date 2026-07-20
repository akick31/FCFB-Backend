package com.fcfb.arceus.dto.response

data class VegasOddsResponse(
    val homeTeam: String,
    val awayTeam: String,
    val homeSpread: Double,
    val awaySpread: Double,
    val homeElo: Double,
    val awayElo: Double,
)
