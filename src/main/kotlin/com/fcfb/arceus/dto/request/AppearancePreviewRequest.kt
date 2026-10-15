package com.fcfb.arceus.dto.request

data class AppearancePreviewRequest(
    val team: String,
    val opponent: String? = null,
    val view: String = "FIELD",
    val uniform: TeamUniformRequest? = null,
    val field: TeamFieldRequest? = null,
    val colors: TeamColorsRequest? = null,
    val logo: String? = null,
    val logoDark: String? = null,
    val secondaryLogo: String? = null,
)
