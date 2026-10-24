package com.fcfb.arceus.dto.response

import com.fcfb.arceus.model.appearance.StadiumDesign

data class StadiumResponse(
    val name: String,
    val city: String?,
    val state: String?,
    val capacity: Int?,
    val active: Boolean,
    val design: StadiumDesign,
    val teams: List<String>,
    val bowls: List<String>,
    val conferences: List<String>,
    val playoffRounds: List<String>,
)
