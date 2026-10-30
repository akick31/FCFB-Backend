package com.fcfb.arceus.dto.response

data class UpsetResponse(
    val gameId: Int,
    val winner: String,
    val loser: String,
    val winnerRank: Int?,
    val loserRank: Int?,
    val points: Double,
)
