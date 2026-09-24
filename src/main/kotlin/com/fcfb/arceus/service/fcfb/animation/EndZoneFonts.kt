package com.fcfb.arceus.service.fcfb.animation

import java.awt.Font

/**
 * The end zone fonts a team may choose. These resolve to JVM families, so a face the host lacks falls back silently —
 * shipping real bundled faces means registering TTFs here via [Font.createFont] rather than adding names.
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
        )

    fun familyOf(name: String?): String = families[name?.uppercase()] ?: families.getValue(DEFAULT_KEY)

    fun choices(): Set<String> = families.keys
}
