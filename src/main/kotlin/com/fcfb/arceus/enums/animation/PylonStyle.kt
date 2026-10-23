package com.fcfb.arceus.enums.animation

enum class PylonStyle {
    STEEL,
    TRUSS,
    BRICK,
    CONCRETE,
    ;

    companion object {
        fun from(stored: String?): PylonStyle = entries.firstOrNull { it.name == stored?.uppercase() } ?: STEEL
    }
}
