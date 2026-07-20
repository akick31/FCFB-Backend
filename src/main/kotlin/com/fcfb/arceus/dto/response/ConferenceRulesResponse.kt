package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.dto.standard.ProtectedRivalry

data class ConferenceRulesResponse(
    @JsonProperty("conference") val conference: String,
    @JsonProperty("numConferenceGames") val numConferenceGames: Int,
    @JsonProperty("protectedRivalries") val protectedRivalries: List<ProtectedRivalry> = emptyList(),
)
