package com.fcfb.arceus.enums.animation

/** [Y] drops a single stem from the crossbar's center; [H] drops one under each upright. Both share width and height. */
enum class GoalPostStyle {
    Y,
    H,
    ;

    companion object {
        fun from(stored: String?): GoalPostStyle = entries.firstOrNull { it.name == stored?.uppercase() } ?: Y
    }
}
