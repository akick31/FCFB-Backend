package com.fcfb.arceus.model.ranking

internal data class TeamSeasonAccumulator(
    var gamesPlayed: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var pointsFor: Int = 0,
    var pointsAgainst: Int = 0,
    var cappedPointDifferential: Int = 0,
)
