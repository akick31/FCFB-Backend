package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ApiKeyResponse(
    @JsonProperty("apiKey") val apiKey: String,
)
