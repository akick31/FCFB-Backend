package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class ResetPasswordRequest(
    val token: String,
    @JsonProperty("newPassword")
    val newPassword: String,
)
