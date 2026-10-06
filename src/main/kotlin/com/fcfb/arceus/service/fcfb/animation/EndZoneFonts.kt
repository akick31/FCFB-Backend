package com.fcfb.arceus.service.fcfb.animation

import java.awt.Font

/**
 * The end zone fonts a team may choose. Logical JVM families (SANS/SERIF/MONOSPACE) always resolve; named faces resolve
 * only if the host has them, otherwise they fall back silently. BLOCK is a bundled TTF registered in [AnimationFonts],
 * so to add a real collegiate face, drop its .ttf in resources, register it there, and add its family name here.
 */
object EndZoneFonts {
    const val YARD_NUMBER_FAMILY = "Arial"

    private const val DEFAULT_KEY = "CLASSIC"

    private val families =
        mapOf(
            DEFAULT_KEY to "Arial",
            "SANS" to Font.SANS_SERIF,
            "SERIF" to Font.SERIF,
            "MONOSPACE" to Font.MONOSPACED,
            "BLOCK" to AnimationFonts.graduate.family,
            "CONDENSED" to "Arial Narrow",
            "IMPACT" to "Impact",
            "SLAB" to "Rockwell",
            "TYPEWRITER" to "Courier New",
            "GEORGIA" to "Georgia",
        )

    fun familyOf(name: String?): String = FontRegistry.familyFor(name) ?: families[name?.uppercase()] ?: families.getValue(DEFAULT_KEY)

    fun choices(): Set<String> = families.keys
}
