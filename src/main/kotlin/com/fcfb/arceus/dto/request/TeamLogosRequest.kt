package com.fcfb.arceus.dto.request

/** Editable team logos. League identity, so admin/commissioner only. */
data class TeamLogosRequest(
    val logo: String? = null,
    val logoDark: String? = null,
    val secondaryLogo: String? = null,
)
