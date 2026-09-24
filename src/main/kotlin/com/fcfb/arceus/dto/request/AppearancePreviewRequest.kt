package com.fcfb.arceus.dto.request

/**
 * A draft uniform and field to render without saving, so the editor can show unsaved edits. Either half may be omitted,
 * in which case the team's stored settings are used for that half.
 */
data class AppearancePreviewRequest(
    val team: String,
    val opponent: String? = null,
    val view: String = "FIELD",
    val uniform: TeamUniformRequest? = null,
    val field: TeamFieldRequest? = null,
)
