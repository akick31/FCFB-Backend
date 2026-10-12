package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class TeamRankingMetricTrendResponse(
    @JsonProperty("metricType") val metricType: String,
    @JsonProperty("week") val week: Int,
    @JsonProperty("rank") val rank: Int?,
    @JsonProperty("total") val total: Int,
)
