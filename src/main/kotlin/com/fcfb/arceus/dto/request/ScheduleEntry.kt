package com.fcfb.arceus.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.team.Subdivision

data class ScheduleEntry(
    @JsonProperty("id") val id: Int? = null,
    @JsonProperty("season") val season: Int,
    @JsonProperty("week") val week: Int,
    @JsonProperty("subdivision") val subdivision: Subdivision,
    @JsonProperty("homeTeam") val homeTeam: String,
    @JsonProperty("awayTeam") val awayTeam: String,
    @JsonProperty("tvChannel") val tvChannel: TVChannel? = null,
    @JsonProperty("gameType") val gameType: GameType,
    @JsonProperty("playoffRound") val playoffRound: Int? = null,
    @JsonProperty("playoffHomeSeed") val playoffHomeSeed: Int? = null,
    @JsonProperty("playoffAwaySeed") val playoffAwaySeed: Int? = null,
    @JsonProperty("postseasonGameName") val postseasonGameName: String? = null,
    @JsonProperty("postseasonGameLogo") val postseasonGameLogo: String? = null,
)
