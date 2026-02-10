package com.fcfb.arceus.dto

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
    @JsonProperty("bowlGameName") val bowlGameName: String? = null,
    @JsonProperty("postseasonGameLogo") val postseasonGameLogo: String? = null,
)

data class BulkScheduleRequest(
    @JsonProperty("entries") val entries: List<ScheduleEntry>,
)

data class MoveGameRequest(
    @JsonProperty("scheduleId") val scheduleId: Int,
    @JsonProperty("newWeek") val newWeek: Int,
)

data class ConferenceScheduleRequest(
    @JsonProperty("season") val season: Int,
    @JsonProperty("conference") val conference: String,
    @JsonProperty("subdivision") val subdivision: Subdivision,
    @JsonProperty("numConferenceGames") val numConferenceGames: Int = 9,
    @JsonProperty("protectedRivalries") val protectedRivalries: List<ProtectedRivalry> = emptyList(),
    @JsonProperty("startWeek") val startWeek: Int = 1,
)

data class ProtectedRivalry(
    @JsonProperty("team1") val team1: String,
    @JsonProperty("team2") val team2: String,
    @JsonProperty("week") val week: Int? = null,
)

// ===== Game Week Job DTOs =====

enum class GameWeekJobStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

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

data class GameStartLog(
    @JsonProperty("homeTeam") val homeTeam: String,
    @JsonProperty("awayTeam") val awayTeam: String,
    @JsonProperty("status") val status: String,
    @JsonProperty("message") val message: String,
    @JsonProperty("timestamp") val timestamp: String,
    @JsonProperty("index") val index: Int = 0,
)

data class FailedGameDetail(
    @JsonProperty("homeTeam") val homeTeam: String,
    @JsonProperty("awayTeam") val awayTeam: String,
    @JsonProperty("subdivision") val subdivision: Subdivision,
    @JsonProperty("tvChannel") val tvChannel: TVChannel? = null,
    @JsonProperty("gameType") val gameType: GameType,
    @JsonProperty("error") val error: String,
)

data class GameWeekJobResponse(
    @JsonProperty("jobId") val jobId: String,
    @JsonProperty("message") val message: String,
)

// ===== Schedule Generation Job DTOs =====

enum class ScheduleGenJobStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

data class ScheduleGenLog(
    @JsonProperty("conference") val conference: String,
    @JsonProperty("status") val status: String,
    @JsonProperty("gamesGenerated") val gamesGenerated: Int,
    @JsonProperty("message") val message: String,
    @JsonProperty("timestamp") val timestamp: String,
)

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

data class ScheduleGenJobResponse(
    @JsonProperty("jobId") val jobId: String,
    @JsonProperty("message") val message: String,
)
