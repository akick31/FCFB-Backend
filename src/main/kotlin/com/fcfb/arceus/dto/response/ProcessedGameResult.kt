package com.fcfb.arceus.dto.response

data class ProcessedGameResult(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val playsProcessed: Int,
)
