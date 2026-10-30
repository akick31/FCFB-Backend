package com.fcfb.arceus.enums.animation

enum class MowingPattern {
    STRIPES,
    NONE,
    ;

    companion object {
        fun from(stored: String?): MowingPattern = entries.firstOrNull { it.name == stored?.uppercase() } ?: STRIPES
    }
}
