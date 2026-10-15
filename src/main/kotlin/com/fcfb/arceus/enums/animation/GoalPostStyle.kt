package com.fcfb.arceus.enums.animation

enum class GoalPostStyle {
    Y,
    H,
    ;

    companion object {
        fun from(stored: String?): GoalPostStyle = entries.firstOrNull { it.name == stored?.uppercase() } ?: Y
    }
}
