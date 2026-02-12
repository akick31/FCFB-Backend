package com.fcfb.arceus.dto

import com.fcfb.arceus.model.GameStats

/**
 * DTO for GameStats with opponent stats included
 * Used for scatter plot data where we need both offensive and defensive stats
 */
data class GameStatsWithOpponent(
    val teamStats: GameStats,
    val opponentStats: GameStats? = null,
)
