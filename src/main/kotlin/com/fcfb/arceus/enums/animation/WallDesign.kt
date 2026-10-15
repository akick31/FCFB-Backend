package com.fcfb.arceus.enums.animation

enum class WallDesign {
    BLANK,
    REPEATING_LOGOS,
    TEXT_WITH_LOGOS,
    TEXT_ONLY,
    ;

    val showsText: Boolean get() = this == TEXT_WITH_LOGOS || this == TEXT_ONLY

    companion object {
        fun from(stored: String?): WallDesign = entries.firstOrNull { it.name == stored?.uppercase() } ?: REPEATING_LOGOS
    }
}
