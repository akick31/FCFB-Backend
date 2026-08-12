package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class TeamResumeMetricResponse(
    @JsonProperty("season") val season: Int,
    @JsonProperty("week") val week: Int,
    @JsonProperty("teamId") val teamId: Int,
    @JsonProperty("teamName") val teamName: String?,
    @JsonProperty("overallWins") val overallWins: Int,
    @JsonProperty("overallLosses") val overallLosses: Int,
    @JsonProperty("conferenceWins") val conferenceWins: Int,
    @JsonProperty("conferenceLosses") val conferenceLosses: Int,
    @JsonProperty("q1Wins") val q1Wins: Int,
    @JsonProperty("q1Losses") val q1Losses: Int,
    @JsonProperty("q2Wins") val q2Wins: Int,
    @JsonProperty("q2Losses") val q2Losses: Int,
    @JsonProperty("thWins") val thWins: Int,
    @JsonProperty("thLosses") val thLosses: Int,
    @JsonProperty("q4Wins") val q4Wins: Int,
    @JsonProperty("q4Losses") val q4Losses: Int,
    @JsonProperty("t25Wins") val t25Wins: Int,
    @JsonProperty("t25Losses") val t25Losses: Int,
    @JsonProperty("t50Wins") val t50Wins: Int,
    @JsonProperty("t50Losses") val t50Losses: Int,
    @JsonProperty("t100Wins") val t100Wins: Int,
    @JsonProperty("t100Losses") val t100Losses: Int,
    @JsonProperty("avgOpponentCompositeRank") val avgOpponentCompositeRank: Double?,
    @JsonProperty("compositeSos") val compositeSos: Double?,
)
