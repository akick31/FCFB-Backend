package com.fcfb.arceus.domain.attendance

import com.fcfb.arceus.enums.game.GameType

object AttendanceCalculator {
    const val FULL = 1.0
    private const val FIRST_WEEK_OF_DECLINE = 7
    private const val LOSS_PENALTY = 0.12
    private const val MINIMUM_FILL = 0.45

    private val ALWAYS_FULL =
        setOf(GameType.BOWL, GameType.CONFERENCE_CHAMPIONSHIP, GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP)

    fun fill(
        wins: Int?,
        losses: Int?,
        week: Int?,
        gameType: GameType?,
    ): Double {
        if (gameType in ALWAYS_FULL || (week ?: 0) < FIRST_WEEK_OF_DECLINE) {
            return FULL
        }
        val deficit = (losses ?: 0) - (wins ?: 0)
        if (deficit <= 0) {
            return FULL
        }
        return (FULL - LOSS_PENALTY * deficit).coerceAtLeast(MINIMUM_FILL)
    }
}
