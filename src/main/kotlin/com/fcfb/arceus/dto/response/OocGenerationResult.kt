package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class OocGenerationResult(
    @JsonProperty("gamesScheduled") val gamesScheduled: Int,
    @JsonProperty("unmatchedSlots") val unmatchedSlots: List<OocUnmatchedSlot>,
)
