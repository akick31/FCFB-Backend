package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.scoreboard

import java.awt.Rectangle

class ScoreboardLayout(
    val bounds: Rectangle,
    val homeSide: IntRange,
    val awaySide: IntRange,
    clockHalfWidth: Int,
) {
    val centerX: Int get() = bounds.x + bounds.width / 2
    val sideWidth: Int get() = homeSide.last - homeSide.first
    val homeCenterX: Int get() = (homeSide.first + homeSide.last) / 2
    val awayCenterX: Int get() = (awaySide.first + awaySide.last) / 2
    val centerWidth: Int = (clockHalfWidth * 2 * CLOCK_FILL_SHARE).toInt()
    val wide: Boolean = sideWidth > bounds.height * WIDE_SIDE_SHARE
    val scoreWidth: Int = if (wide) (bounds.height * WIDE_SCORE_SHARE).toInt() else sideWidth
    val homeScoreLeft: Int get() = if (wide) homeSide.last - scoreWidth else homeSide.first
    val awayScoreLeft: Int get() = awaySide.first
    val homeScoreCenterX: Int get() = homeScoreLeft + scoreWidth / 2
    val awayScoreCenterX: Int get() = awayScoreLeft + scoreWidth / 2
    val homeInfo: IntRange? = if (wide) homeSide.first..(homeSide.last - scoreWidth - infoGap) else null
    val awayInfo: IntRange? = if (wide) (awaySide.first + scoreWidth + infoGap)..awaySide.last else null

    private val infoGap: Int get() = (bounds.height * INFO_GAP_SHARE).toInt()

    fun rowTop(share: Float): Int = bounds.y + (bounds.height * share).toInt()

    fun rowHeight(share: Float): Int = (bounds.height * share).toInt().coerceAtLeast(1)

    companion object {
        private const val CLOCK_FILL_SHARE = 0.92f
        private const val MARGIN_SHARE = 0.06f
        private const val WIDE_SIDE_SHARE = 1.45f
        private const val WIDE_SCORE_SHARE = 0.95f
        private const val INFO_GAP_SHARE = 0.06f

        fun of(
            bounds: Rectangle,
            centerHalfWidth: Int,
            clockHalfWidth: Int,
        ): ScoreboardLayout {
            val margin = (bounds.height * MARGIN_SHARE).toInt()
            val centerX = bounds.x + bounds.width / 2
            return ScoreboardLayout(
                bounds = bounds,
                homeSide = bounds.x + margin..centerX - centerHalfWidth,
                awaySide = centerX + centerHalfWidth..bounds.x + bounds.width - margin,
                clockHalfWidth = clockHalfWidth,
            )
        }
    }
}
