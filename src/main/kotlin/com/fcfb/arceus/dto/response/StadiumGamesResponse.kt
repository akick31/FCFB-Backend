package com.fcfb.arceus.dto.response

data class StadiumGamesResponse(
    val games: List<StadiumGameResponse>,
    val records: List<StadiumTeamRecordResponse>,
)
