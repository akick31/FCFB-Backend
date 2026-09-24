package com.fcfb.arceus.dto.request

/** Editable bowl field settings. Left and right are the two ends of the field. */
data class BowlFieldRequest(
    val turfColor: String? = null,
    val endZoneFill: String? = null,
    val endZoneFont: String? = null,
    val leftEndZoneLogoUrl: String? = null,
    val rightEndZoneLogoUrl: String? = null,
    val showConferenceLogos: Boolean? = null,
    val yardNumberSource: String? = null,
    val yardNumberOutlineColor: String? = null,
    val leftOobLineColor: String? = null,
    val rightOobLineColor: String? = null,
    val redZoneEnabled: Boolean? = null,
    val redZoneBorderColor: String? = null,
    val wallColor: String? = null,
    val wallDesign: String? = null,
    val wallText: String? = null,
    val wallTextOutlineColor: String? = null,
)
