package com.fcfb.arceus.service.fcfb.animation

enum class YardNumberArrowAlign {
    CENTER,
    TOP,
    BOTTOM,
    ;

    companion object {
        fun from(stored: String?): YardNumberArrowAlign = entries.firstOrNull { it.name == stored?.uppercase() } ?: TOP
    }
}
