package com.fcfb.arceus.enums.animation

enum class YardNumberSource {
    TEAM_PER_SIDE,
    FIXED,
    ;

    companion object {
        fun from(stored: String?): YardNumberSource = entries.firstOrNull { it.name == stored?.uppercase() } ?: TEAM_PER_SIDE
    }
}
