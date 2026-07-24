package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class RankingResponse(
    @JsonProperty("season") val season: Int,
    @JsonProperty("week") val week: Int,
    @JsonProperty("pollType") val pollType: String,
    @JsonProperty("rank") val rank: Int,
    @JsonProperty("teamId") val teamId: Int,
    @JsonProperty("teamName") val teamName: String?,
)
