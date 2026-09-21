package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class UserDelayOfGameResponse(
    @JsonProperty("username") val username: String,
    @JsonProperty("discord_tag") val discordTag: String?,
    @JsonProperty("discord_id") val discordId: String,
    @JsonProperty("team") val team: String?,
    @JsonProperty("delay_of_game_instances") val delayOfGameInstances: Int,
)
