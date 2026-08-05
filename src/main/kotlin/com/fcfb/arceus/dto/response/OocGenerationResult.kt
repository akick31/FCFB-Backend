package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class OocUnmatchedSlot(
    @JsonProperty("team") val team: String,
    @JsonProperty("week") val week: Int,
)

data class OocGenerationResult(
    @JsonProperty("gamesScheduled") val gamesScheduled: Int,
    @JsonProperty("unmatchedSlots") val unmatchedSlots: List<OocUnmatchedSlot>,
)
