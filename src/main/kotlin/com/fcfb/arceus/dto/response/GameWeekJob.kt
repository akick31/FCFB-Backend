package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class GameWeekJob(
    @JsonProperty("jobId") val jobId: String,
    @JsonProperty("season") val season: Int,
    @JsonProperty("week") val week: Int,
    @JsonProperty("status") var status: GameWeekJobStatus = GameWeekJobStatus.PENDING,
    @JsonProperty("totalGames") var totalGames: Int = 0,
    @JsonProperty("startedGames") var startedGames: Int = 0,
    @JsonProperty("failedGames") var failedGames: Int = 0,
    @JsonProperty("currentIndex") var currentIndex: Int = 0,
    @JsonProperty("logs") val logs: MutableList<GameStartLog> = mutableListOf(),
    @JsonProperty("failedGameDetails") val failedGameDetails: MutableList<FailedGameDetail> = mutableListOf(),
    @JsonProperty("startedAt") var startedAt: String? = null,
    @JsonProperty("completedAt") var completedAt: String? = null,
)
