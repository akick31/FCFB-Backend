package com.fcfb.arceus.dto.request

data class FrontendErrorRequest(
    var message: String,
    var stack: String?,
    var url: String?,
)
