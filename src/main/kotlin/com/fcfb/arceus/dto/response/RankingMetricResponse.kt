package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class RankingMetricResponse(
    @JsonProperty("season") val season: Int,
    @JsonProperty("week") val week: Int,
    @JsonProperty("metricType") val metricType: String,
    @JsonProperty("teamId") val teamId: Int,
    @JsonProperty("teamName") val teamName: String?,
    @JsonProperty("value") val value: Double,
    @JsonProperty("wins") val wins: Int?,
    @JsonProperty("losses") val losses: Int?,
)
