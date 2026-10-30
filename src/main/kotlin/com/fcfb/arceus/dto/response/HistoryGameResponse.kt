package com.fcfb.arceus.dto.response

data class HistoryGameResponse(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int,
    val awayScore: Int,
    val homeRank: Int?,
    val awayRank: Int?,
    val homeSpread: Double?,
    val gameType: String?,
    val postseasonName: String?,
    val overtime: Boolean,
)
