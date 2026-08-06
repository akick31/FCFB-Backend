package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ScheduleValidationResult(
    @JsonProperty("valid") val valid: Boolean,
    @JsonProperty("incompleteTeams") val incompleteTeams: List<TeamScheduleGap>,
)
