package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.game.GameType

data class StadiumGameResponse(
    val gameId: Int?,
    val season: Int?,
    val week: Int?,
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val final: Boolean,
    val gameType: GameType?,
    val postseasonGameName: String?,
)
