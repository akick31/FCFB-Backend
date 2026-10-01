package com.fcfb.arceus.dto.request

/** Editable team colors. Primary and secondary are admin-only; tertiary is editable by the team's own coach. */
data class TeamColorsRequest(
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val tertiaryColor: String? = null,
)
