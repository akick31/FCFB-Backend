package com.fcfb.arceus.dto.response

data class RankedLossResponse(
    val gameId: Int,
    val team: String,
    val rank: Int,
    val opponent: String,
)
