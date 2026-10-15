package com.fcfb.arceus.model.ranking

data class TeamSeasonAggregate(
    val teamId: Int,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val pointsFor: Int,
    val pointsAgainst: Int,
    val cappedPointDifferential: Int,
)
