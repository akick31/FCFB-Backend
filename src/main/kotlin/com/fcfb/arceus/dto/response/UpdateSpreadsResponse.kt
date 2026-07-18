package com.fcfb.arceus.dto.response

data class UpdateSpreadsResponse(
    val message: String,
    val season: Int,
    val week: Int,
    val totalGames: Int,
    val updatedGames: Int,
    val results: List<GameSpreadResult>,
)
