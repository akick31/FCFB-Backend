package com.fcfb.arceus.dto.response

data class SpreadResponse(
    val gameId: Int,
    val favorite: String,
    val underdog: String,
    val points: Double,
    val favoriteWon: Boolean,
)
