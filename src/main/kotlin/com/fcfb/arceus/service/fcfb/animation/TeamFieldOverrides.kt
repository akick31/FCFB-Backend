package com.fcfb.arceus.service.fcfb.animation

import java.awt.Color

/** Home fields that aren't green. Until teams can set their own field on the website, the known ones live here. */
object TeamFieldOverrides {
    private val BOISE_BLUE = Color(0, 51, 160)
    private val BOISE_ORANGE = Color(214, 67, 9)

    private val overrides = mapOf("Boise State" to TeamFieldOverride(BOISE_BLUE, BOISE_ORANGE))

    fun forTeam(name: String?): TeamFieldOverride? = name?.let { overrides[it] }
}
