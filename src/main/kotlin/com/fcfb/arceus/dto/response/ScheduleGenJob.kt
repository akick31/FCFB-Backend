package com.fcfb.arceus.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ScheduleGenJob(
    @JsonProperty("jobId") val jobId: String,
    @JsonProperty("season") val season: Int,
    @JsonProperty("status") var status: ScheduleGenJobStatus = ScheduleGenJobStatus.PENDING,
    @JsonProperty("totalConferences") var totalConferences: Int = 0,
    @JsonProperty("completedConferences") var completedConferences: Int = 0,
    @JsonProperty("failedConferences") var failedConferences: Int = 0,
    @JsonProperty("totalGamesGenerated") var totalGamesGenerated: Int = 0,
    @JsonProperty("logs") val logs: MutableList<ScheduleGenLog> = mutableListOf(),
    @JsonProperty("startedAt") var startedAt: String? = null,
    @JsonProperty("completedAt") var completedAt: String? = null,
)
