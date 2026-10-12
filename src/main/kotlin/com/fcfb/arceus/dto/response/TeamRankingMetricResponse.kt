package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class TeamRankingMetricResponse(
    @JsonProperty("metricType") val metricType: String,
    @JsonProperty("value") val value: Double?,
    @JsonProperty("rank") val rank: Int?,
    @JsonProperty("total") val total: Int,
)
