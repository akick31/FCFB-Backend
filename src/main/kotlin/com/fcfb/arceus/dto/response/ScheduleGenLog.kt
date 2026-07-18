package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ScheduleGenLog(
    @JsonProperty("conference") val conference: String,
    @JsonProperty("status") val status: String,
    @JsonProperty("gamesGenerated") val gamesGenerated: Int,
    @JsonProperty("message") val message: String,
    @JsonProperty("timestamp") val timestamp: String,
)
