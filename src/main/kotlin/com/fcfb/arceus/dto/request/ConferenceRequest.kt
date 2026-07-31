package com.fcfb.arceus.dto.request

data class ConferenceRequest(
    val code: String,
    val label: String,
    val logoUrl: String?,
    val logoUrlDark: String?,
    val displayOrder: Int = 0,
)
