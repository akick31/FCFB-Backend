package com.fcfb.arceus.enums.animation

enum class StadiumBackdrop {
    CROWD,
    BUILDING,
    CAMPUS,
    FOREST,
    FIELD,
    LAKE,
    MOUNTAINS,
    HILL,
    NONE,
    ;

    companion object {
        fun from(stored: String?): StadiumBackdrop = entries.firstOrNull { it.name == stored?.uppercase() } ?: CROWD
    }
}
