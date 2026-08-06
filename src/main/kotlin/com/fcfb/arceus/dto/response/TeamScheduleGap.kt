package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class TeamScheduleGap(
    @JsonProperty("team") val team: String,
    @JsonProperty("missingWeeks") val missingWeeks: List<Int>,
)
