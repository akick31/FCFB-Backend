package com.fcfb.arceus.dto.request

data class UserValidationRequest(
    var discordId: String,
    var discordTag: String,
    var username: String,
    var email: String,
)
