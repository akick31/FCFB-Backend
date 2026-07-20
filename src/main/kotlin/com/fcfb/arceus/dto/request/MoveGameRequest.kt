package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class MoveGameRequest(
    @JsonProperty("scheduleId") val scheduleId: Int,
    @JsonProperty("newWeek") val newWeek: Int,
)
