package com.fcfb.arceus.model.ranking

internal data class TeamDiffAggregate(
    val averageOffensiveDiff: Double,
    val averageDefensiveDiff: Double,
    val averageOffensiveSpecialTeamsDiff: Double,
    val averageDefensiveSpecialTeamsDiff: Double,
)
