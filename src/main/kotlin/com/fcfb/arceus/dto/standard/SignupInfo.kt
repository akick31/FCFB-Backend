package com.fcfb.arceus.dto.standard

import com.fasterxml.jackson.annotation.JsonProperty

data class SignupInfo(
    @JsonProperty("discord_tag") val discordTag: String,
    @JsonProperty("discord_id") val discordId: String,
    @JsonProperty("team_choice_one") val teamChoiceOne: String,
    @JsonProperty("team_choice_two") val teamChoiceTwo: String,
    @JsonProperty("team_choice_three") val teamChoiceThree: String,
)
