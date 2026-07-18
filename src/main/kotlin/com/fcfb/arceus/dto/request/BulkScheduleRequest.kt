package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class BulkScheduleRequest(
    @JsonProperty("entries") val entries: List<ScheduleEntry>,
)
