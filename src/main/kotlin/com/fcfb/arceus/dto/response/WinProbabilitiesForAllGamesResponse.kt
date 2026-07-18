package com.fcfb.arceus.dto.response

data class WinProbabilitiesForAllGamesResponse(
    val totalGames: Int,
    val gamesProcessed: Int,
    val totalPlaysProcessed: Int,
    val processedGames: List<ProcessedGameResult>,
)
