package com.fcfb.arceus.model.ranking

internal data class TeamDiffAccumulator(
    var count: Int = 0,
    var offensiveDiffSum: Double = 0.0,
    var defensiveDiffSum: Double = 0.0,
    var offensiveSpecialTeamsDiffSum: Double = 0.0,
    var defensiveSpecialTeamsDiffSum: Double = 0.0,
)
