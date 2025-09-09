package com.fcfb.arceus.dto

data class UpdateSpreadsResponse(
    val message: String,
    val season: Int,
    val week: Int,
    val totalGames: Int,
    val updatedGames: Int,
    val results: List<GameSpreadResult>,
)

data class GameSpreadResult(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeElo: Double,
    val awayElo: Double,
    val homeSpread: Double,
    val awaySpread: Double,
)
