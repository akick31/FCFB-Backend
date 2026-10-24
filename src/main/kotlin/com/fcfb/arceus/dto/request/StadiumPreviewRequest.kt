package com.fcfb.arceus.dto.request

data class StadiumPreviewRequest(
    val venue: String? = null,
    val design: StadiumDesignRequest? = null,
    val team: String? = null,
    val opponent: String? = null,
)
