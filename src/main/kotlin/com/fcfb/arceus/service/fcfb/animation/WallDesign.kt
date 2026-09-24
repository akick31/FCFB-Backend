package com.fcfb.arceus.service.fcfb.animation

/** What a team paints on the wall behind the end zone. Text outlining is a separate color, not a design of its own. */
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
