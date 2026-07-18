package com.fcfb.arceus.dto

data class FrontendErrorRequest(
    var message: String,
    var stack: String?,
    var url: String?,
)
