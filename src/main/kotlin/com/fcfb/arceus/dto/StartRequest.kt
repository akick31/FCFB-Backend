package com.fcfb.arceus.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.team.Subdivision

data class StartRequest(
    @JsonProperty("homePlatform") val homePlatform: com.fcfb.arceus.enums.system.Platform,
    @JsonProperty("awayPlatform") val awayPlatform: com.fcfb.arceus.enums.system.Platform,
    @JsonProperty("subdivision") val subdivision: Subdivision,
    @JsonProperty("homeTeam") val homeTeam: String,
    @JsonProperty("awayTeam") val awayTeam: String,
    @JsonProperty("tvChannel") val tvChannel: TVChannel?,
    @JsonProperty("gameType") val gameType: GameType,
    @JsonProperty("postseasonGameLogo") val postseasonGameLogo: String? = null,
)
