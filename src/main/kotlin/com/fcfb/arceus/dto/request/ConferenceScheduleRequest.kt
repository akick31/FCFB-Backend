package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.dto.standard.ProtectedRivalry
import com.fcfb.arceus.enums.team.Subdivision

data class ConferenceScheduleRequest(
    @JsonProperty("season") val season: Int,
    @JsonProperty("conference") val conference: String,
    @JsonProperty("subdivision") val subdivision: Subdivision,
    @JsonProperty("numConferenceGames") val numConferenceGames: Int = 9,
    @JsonProperty("protectedRivalries") val protectedRivalries: List<ProtectedRivalry> = emptyList(),
    @JsonProperty("startWeek") val startWeek: Int = 1,
)
