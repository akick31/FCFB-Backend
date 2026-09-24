package com.fcfb.arceus.dto.request

/** Editable home field settings. A null [fieldNumberOutlineColor] means the yard numbers are drawn without an outline. */
data class TeamFieldRequest(
    val turfColor: String? = null,
    val endZoneColor: String? = null,
    val endZoneFont: String? = null,
    val midfieldLogoUrl: String? = null,
    val fieldNumberOutlineColor: String? = null,
    val redZoneBorderColor: String? = null,
    val oobLineColor: String? = null,
    val wallColor: String? = null,
    val wallDesign: String? = null,
    val wallText: String? = null,
    val wallTextOutlineColor: String? = null,
    val goalPostColor: String? = null,
    val goalPostStyle: String? = null,
    val quarterLogoUrl: String? = null,
)
