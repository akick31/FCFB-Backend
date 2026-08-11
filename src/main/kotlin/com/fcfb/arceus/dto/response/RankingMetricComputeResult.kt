package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class RankingMetricComputeResult(
    @JsonProperty("season") val season: Int,
    @JsonProperty("week") val week: Int,
    @JsonProperty("metricTypesComputed") val metricTypesComputed: List<String>,
    @JsonProperty("teamsProcessed") val teamsProcessed: Int,
)
