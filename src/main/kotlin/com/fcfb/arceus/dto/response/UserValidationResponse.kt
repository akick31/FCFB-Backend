package com.fcfb.arceus.dto.response

data class UserValidationResponse(
    var discordIdExists: Boolean,
    var discordTagExists: Boolean,
    var usernameExists: Boolean,
    var emailExists: Boolean,
)
