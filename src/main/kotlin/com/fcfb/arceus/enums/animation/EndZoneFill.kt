package com.fcfb.arceus.enums.animation

enum class EndZoneFill {
    NONE,
    PRIMARY,
    SECONDARY,
    ;

    companion object {
        fun from(stored: String?): EndZoneFill = entries.firstOrNull { it.name == stored?.uppercase() } ?: PRIMARY
    }
}
