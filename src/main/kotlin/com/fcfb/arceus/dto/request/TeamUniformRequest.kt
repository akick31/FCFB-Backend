package com.fcfb.arceus.dto.request

/**
 * Editable uniform settings. Primary and secondary colors are deliberately absent: those belong to the team record and
 * are copied into the uniform, so exposing them here would give them two sources of truth.
 */
data class TeamUniformRequest(
    val tertiaryColor: String? = null,
    val helmetColor: String? = null,
    val secondaryHelmetColor: String? = null,
    val facemaskColor: String? = null,
    val helmetLogoMode: String? = null,
    val helmetNumberColor: String? = null,
    val logoUrl: String? = null,
    val hasLogo: Boolean? = null,
    val hasStripe: Boolean? = null,
    val stripeColor: String? = null,
    val jerseyColor: String? = null,
    val numberColor: String? = null,
    val numberOutlineColor: String? = null,
    val pantsColor: String? = null,
)
