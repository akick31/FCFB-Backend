package com.fcfb.arceus.model.elo

data class EloReplayResult(
    val preGameElo: Map<Pair<Int, String>, Double>,
    val finalElo: Map<String, Double>,
)
