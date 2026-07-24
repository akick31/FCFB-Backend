package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class UploadRankingsRequest(
    @JsonProperty("season") val season: Int,
    @JsonProperty("week") val week: Int,
    @JsonProperty("pollType") val pollType: String,
    @JsonProperty("teams") val teams: List<String>,
)
