package com.fcfb.arceus.model.record

import com.fcfb.arceus.model.statistics.GameStats

internal data class TeamSeasonAgainst(
    val team: String,
    val season: Int,
    val value: Double,
    val ownRow: GameStats,
)
