package com.fcfb.arceus.enums.animation

enum class ScoreboardShape {
    RECTANGLE,
    ROUNDED,
    CHAMFERED,
    ARCHED,
    MARQUEE,
    ;

    companion object {
        fun from(stored: String?): ScoreboardShape = entries.firstOrNull { it.name == stored?.uppercase() } ?: RECTANGLE
    }
}
