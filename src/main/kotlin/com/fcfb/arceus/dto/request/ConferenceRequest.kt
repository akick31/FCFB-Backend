package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class ConferenceRequest(
    @JsonProperty("code") val code: String,
    @JsonProperty("label") val label: String,
    @JsonProperty("logoUrl") val logoUrl: String?,
    @JsonProperty("logoUrlDark") val logoUrlDark: String?,
    @JsonProperty("abbreviation") val abbreviation: String? = null,
)
