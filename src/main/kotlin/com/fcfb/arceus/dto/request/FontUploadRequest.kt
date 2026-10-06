package com.fcfb.arceus.dto.request

data class FontUploadRequest(
    val label: String,
    val url: String,
    val acknowledged: Boolean = false,
)
