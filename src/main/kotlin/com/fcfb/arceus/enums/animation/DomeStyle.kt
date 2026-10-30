package com.fcfb.arceus.enums.animation

enum class DomeStyle {
    NONE,
    SUITES,
    FABRIC,
    WINDOWED,
    ;

    companion object {
        fun from(stored: String?): DomeStyle = entries.firstOrNull { it.name == stored?.uppercase() } ?: NONE
    }
}
