package com.fcfb.arceus.dto.standard

import com.fasterxml.jackson.annotation.JsonProperty

data class ProtectedRivalry(
    @JsonProperty("team1") val team1: String,
    @JsonProperty("team2") val team2: String,
    @JsonProperty("week") val week: Int? = null,
)
