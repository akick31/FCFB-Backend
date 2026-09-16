package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color

/** Home fields that aren't green. Until teams can set their own field on the website, the known ones live here. */
object TeamFieldOverrides {
    private val BOISE_BLUE = Color(0, 51, 160)
    private val BOISE_ORANGE = Color(214, 67, 9)
    private val COASTAL_TEAL = Color(0, 111, 113)
    private val COASTAL_BRONZE = Color(162, 119, 82)
    private val EASTERN_GREY = Color(110, 110, 112)
    private val EASTERN_GREEN = Color(0, 102, 51)

    private val overrides =
        mapOf(
            "Boise State" to TeamFieldOverride(BOISE_BLUE, BOISE_ORANGE),
            "Coastal Carolina" to TeamFieldOverride(COASTAL_TEAL, COASTAL_BRONZE),
            "Eastern Michigan" to TeamFieldOverride(EASTERN_GREY, EASTERN_GREEN),
        )

    fun forTeam(name: String?): TeamFieldOverride? = name?.let { overrides[it] }
}
