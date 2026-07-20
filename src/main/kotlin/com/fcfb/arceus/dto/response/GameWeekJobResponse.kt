package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class GameWeekJobResponse(
    @JsonProperty("jobId") val jobId: String,
    @JsonProperty("message") val message: String,
)
