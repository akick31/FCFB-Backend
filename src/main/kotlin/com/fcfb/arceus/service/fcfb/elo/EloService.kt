package com.fcfb.arceus.service.fcfb.elo

import com.fcfb.arceus.dto.response.EloHistoryEntry
import com.fcfb.arceus.dto.response.EloRatingResponse
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.service.fcfb.TeamService
import org.springframework.stereotype.Service

@Service
class EloService(
    private val teamService: TeamService,
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
) {
    fun getEloRatings(): List<EloRatingResponse> =
        teamService.getAllTeams()
            .map { EloRatingResponse(it.id, it.name ?: "", it.currentElo, it.overallElo) }
            .sortedByDescending { it.currentElo }

    fun getEloHistory(
        team: String,
        season: Int?,
    ): List<EloHistoryEntry> {
        val gameStatsList =
            if (team.lowercase() == "all") {
                if (season != null) {
                    gameStatsRepository.findBySeasonOrderByGameIdAsc(season)
                } else {
                    val latestSeason = gameStatsRepository.findMaxSeason() ?: 0
                    if (latestSeason == 0) {
                        emptyList()
                    } else {
                        val minSeason = (latestSeason - 9).coerceAtLeast(1)
                        gameStatsRepository.findBySeasonGreaterThanEqualOrderBySeasonDescGameIdAsc(minSeason)
                    }
                }
            } else {
                if (season != null) {
                    gameStatsRepository.findByTeamAndSeason(team, season)
                } else {
                    gameStatsRepository.findByTeam(team)
                }
            }

        if (gameStatsList.isEmpty()) {
            return emptyList()
        }

        val gameIds = gameStatsList.mapNotNull { it.gameId }.distinct().take(10000)
        val gamesMap =
            if (gameIds.isNotEmpty()) {
                gameRepository.findAllById(gameIds).associateBy { it.gameId }
            } else {
                emptyMap()
            }

        val sortedStats =
            gameStatsList.sortedWith(
                compareBy<GameStats> { it.team ?: "" }
                    .thenBy { it.season ?: 0 }
                    .thenBy { it.week ?: 0 }
                    .thenBy { it.gameId },
            )

        return sortedStats.map { stats ->
            val teamName = stats.team ?: ""
            val game = stats.gameId?.let { gamesMap[it] }
            val opponent =
                if (game != null) {
                    if (game.homeTeam == teamName) game.awayTeam else game.homeTeam
                } else {
                    null
                }

            val result =
                if (game != null && game.gameStatus?.name == "FINAL") {
                    val teamScore = if (game.homeTeam == teamName) game.homeScore else game.awayScore
                    val oppScore = if (game.homeTeam == teamName) game.awayScore else game.homeScore
                    when {
                        teamScore != null && oppScore != null -> {
                            if (teamScore > oppScore) {
                                "W"
                            } else if (teamScore < oppScore) {
                                "L"
                            } else {
                                null
                            }
                        }
                        else -> null
                    }
                } else {
                    null
                }

            EloHistoryEntry(
                team = teamName,
                season = stats.season ?: 0,
                week = stats.week,
                elo = stats.teamElo,
                gameId = stats.gameId,
                opponent = opponent,
                result = result,
            )
        }
    }
}
