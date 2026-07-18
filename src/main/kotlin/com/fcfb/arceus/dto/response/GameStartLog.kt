package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class GameStartLog(
    @JsonProperty("homeTeam") val homeTeam: String,
    @JsonProperty("awayTeam") val awayTeam: String,
    @JsonProperty("status") val status: String,
    @JsonProperty("message") val message: String,
    @JsonProperty("timestamp") val timestamp: String,
    @JsonProperty("index") val index: Int = 0,
)
