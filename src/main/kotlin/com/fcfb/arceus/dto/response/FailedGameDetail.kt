package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.team.Subdivision

data class FailedGameDetail(
    @JsonProperty("homeTeam") val homeTeam: String,
    @JsonProperty("awayTeam") val awayTeam: String,
    @JsonProperty("subdivision") val subdivision: Subdivision,
    @JsonProperty("tvChannel") val tvChannel: TVChannel? = null,
    @JsonProperty("gameType") val gameType: GameType,
    @JsonProperty("postseasonGameLogo") val postseasonGameLogo: String? = null,
    @JsonProperty("postseasonGameName") val postseasonGameName: String? = null,
    @JsonProperty("error") val error: String,
)
