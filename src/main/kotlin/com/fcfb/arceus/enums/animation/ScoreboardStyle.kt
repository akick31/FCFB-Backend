package com.fcfb.arceus.enums.animation

enum class ScoreboardStyle {
    CLASSIC,
    VIDEO,
    VINTAGE,
    SEGMENT,
    ;

    companion object {
        fun from(stored: String?): ScoreboardStyle = entries.firstOrNull { it.name == stored?.uppercase() } ?: CLASSIC
    }
}
