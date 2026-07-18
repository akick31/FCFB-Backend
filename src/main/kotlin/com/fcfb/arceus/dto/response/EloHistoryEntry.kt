package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class EloHistoryEntry(
    @JsonProperty("team") val team: String,
    @JsonProperty("season") val season: Int,
    @JsonProperty("week") val week: Int?,
    @JsonProperty("elo") val elo: Double,
    @JsonProperty("game_id") val gameId: Int?,
    @JsonProperty("opponent") val opponent: String?,
    // "W", "L", or null if not finished
    @JsonProperty("result") val result: String?,
)
