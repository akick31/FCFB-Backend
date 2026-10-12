package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class GameFinalizationRollbackRequest(
    val gameId: Int,
    val homeTeam: String,
    val awayTeam: String,
)
