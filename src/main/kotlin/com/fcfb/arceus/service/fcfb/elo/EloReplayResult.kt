package com.fcfb.arceus.service.fcfb.elo

data class EloReplayResult(
    val preGameElo: Map<Pair<Int, String>, Double>,
    val finalElo: Map<String, Double>,
)
