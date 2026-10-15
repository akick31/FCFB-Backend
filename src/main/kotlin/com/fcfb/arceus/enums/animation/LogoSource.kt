package com.fcfb.arceus.enums.animation

enum class LogoSource {
    CUSTOM,
    PRIMARY,
    SECONDARY,
    NONE,
    ;

    companion object {
        fun from(stored: String?): LogoSource = entries.firstOrNull { it.name == stored?.uppercase() } ?: CUSTOM
    }
}
