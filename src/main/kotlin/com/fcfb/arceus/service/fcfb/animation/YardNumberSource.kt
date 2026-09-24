package com.fcfb.arceus.service.fcfb.animation

/**
 * Where a bowl's yard number outline color comes from. [TEAM_PER_SIDE] uses the team defending that half, so at the 50
 * the home team owns the top number and the away team the bottom; [FIXED] paints the whole field one color.
 */
enum class YardNumberSource {
    TEAM_PER_SIDE,
    FIXED,
    ;

    companion object {
        fun from(stored: String?): YardNumberSource = entries.firstOrNull { it.name == stored?.uppercase() } ?: TEAM_PER_SIDE
    }
}
