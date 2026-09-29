package com.fcfb.arceus.dto.response

data class EloRebuildResult(
    val applied: Boolean,
    val gamesReplayed: Int,
    val teamsChanged: List<EloTeamChange>,
    val gameSpreadsChanged: Int,
    val gameStatsRowsChanged: Int,
)
