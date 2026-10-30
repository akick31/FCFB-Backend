package com.fcfb.arceus.enums.animation

enum class FieldSurface {
    TURF,
    GRASS,
    ;

    companion object {
        fun from(stored: String?): FieldSurface = entries.firstOrNull { it.name == stored?.uppercase() } ?: TURF
    }
}
