package com.fcfb.arceus.dto.response

data class WeekHistoryResponse(
    val season: Int,
    val week: Int,
    val games: List<HistoryGameResponse>,
    val highlights: WeekHighlightsResponse,
)
