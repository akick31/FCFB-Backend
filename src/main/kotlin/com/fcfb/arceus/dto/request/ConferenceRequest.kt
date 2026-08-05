package com.fcfb.arceus.dto.request

data class ConferenceRequest(
    val code: String,
    val label: String,
    val logoUrl: String?,
    val logoUrlDark: String?,
    val abbreviation: String? = null,
    val displayOrder: Int = 0,
)
