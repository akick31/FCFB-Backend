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
            "WYOMING" to AnimationFonts.wyoming.family,
            "CONDENSED" to "Arial Narrow",
            "IMPACT" to "Impact",
            "SLAB" to "Rockwell",
            "TYPEWRITER" to "Courier New",
            "GEORGIA" to "Georgia",
        )

    const val BOLD_SUFFIX = "|BOLD"

    fun familyOf(value: String?): String {
        val key = value?.removeSuffix(BOLD_SUFFIX)
        return FontRegistry.familyFor(key) ?: families[key?.uppercase()] ?: families.getValue(DEFAULT_KEY)
    }

    fun styleFor(value: String?): Int = if (value != null && value.endsWith(BOLD_SUFFIX)) Font.BOLD else Font.PLAIN

    fun choices(): Set<String> = families.keys
}
