package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.game.GameStatus

data class ScorebugResponse(
    val gameId: Int,
    val scorebug: ByteArray?,
    val homeTeam: String,
    val awayTeam: String,
    val status: GameStatus?,
)
