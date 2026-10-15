package com.fcfb.arceus.model.record

import com.fcfb.arceus.model.statistics.GameStats

internal data class SeasonCandidate(
    val team: String,
    val season: Int,
    val value: Double,
    val games: List<GameStats>,
)
