package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.dto.standard.ProtectedRivalry

data class ConferenceRulesRequest(
    @JsonProperty("conference") val conference: String,
    @JsonProperty("numConferenceGames") val numConferenceGames: Int,
    @JsonProperty("protectedRivalries") val protectedRivalries: List<ProtectedRivalry> = emptyList(),
)
