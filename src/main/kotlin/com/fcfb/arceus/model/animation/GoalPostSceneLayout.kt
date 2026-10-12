package com.fcfb.arceus.model.animation

data class GoalPostSceneLayout(
    val scale: Float,
    val endZoneBottomY: Int,
    val endZoneTopY: Int,
    val crossbarY: Int,
    val uprightTopY: Int,
    val netTopY: Int,
    val leftUprightX: Int,
    val rightUprightX: Int,
    val lineOfScrimmageYards: Float,
    val visibleYards: Float,
) {
    val uprightMidY: Int get() = (crossbarY + uprightTopY) / 2

    val pixelsPerYard: Float get() = (SCENE_HEIGHT - endZoneBottomY) / visibleYards

    fun mapY(referenceY: Int): Int = endZoneTopY - ((REFERENCE_END_ZONE_TOP_Y - referenceY) * scale).toInt()

    fun yardY(yardsFromGoal: Float): Float = endZoneBottomY + pixelsPerYard * yardsFromGoal

    fun groundY(yardsFromGoal: Float): Float {
        if (yardsFromGoal >= 0f) return yardY(yardsFromGoal)
        return endZoneBottomY + (endZoneTopY - endZoneBottomY) * (-yardsFromGoal / END_ZONE_DEPTH_YARDS)
    }

    internal companion object {
        const val SCENE_HEIGHT = 600
        const val REFERENCE_END_ZONE_TOP_Y = 190
        const val END_ZONE_DEPTH_YARDS = 10f
    }
}
