package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class LoginRequest(
    @JsonProperty("usernameOrEmail")
    val usernameOrEmail: String,
    val password: String,
)
