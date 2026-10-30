package com.fcfb.arceus.dto.response

data class WeekHighlightsResponse(
    val upsets: List<UpsetResponse>,
    val rankedLosses: List<RankedLossResponse>,
    val biggestSpread: SpreadResponse?,
    val closestGameId: Int?,
    val biggestBlowoutGameId: Int?,
    val highestScoringGameId: Int?,
    val fastestGame: FastestGameResponse?,
    val overtimeGameIds: List<Int>,
    val biggestSwing: WinProbabilitySwingResponse?,
)
