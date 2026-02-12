package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.GameStatsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/game-stats")
class GameStatsController(
    private var gameStatsService: GameStatsService,
) {
    /**
     * Get game stats by game id and team, or get all game stats for a team and season
     * @param gameId Game ID (optional)
     * @param team Team name
     * @param season Season number (optional)
     * @return GameStats or List of GameStats
     */
    @GetMapping("")
    fun getGameStats(
        @RequestParam(required = false) gameId: Int?,
        @RequestParam team: String,
        @RequestParam(required = false) season: Int?,
    ): ResponseEntity<Any> {
        return when {
            gameId != null -> {
                val gameStats = gameStatsService.getGameStatsByIdAndTeam(gameId, team)
                ResponseEntity.ok(gameStats)
            }
            season != null -> {
                val gameStats = gameStatsService.getAllGameStatsForTeamAndSeason(team, season)
                ResponseEntity.ok(gameStats)
            }
            else -> {
                ResponseEntity.badRequest().body("Either gameId or season parameter is required")
            }
        }
    }

    /**
     * Generate game stats for a game
     * @return
     */
    @PostMapping("/generate")
    fun generateGameStats(
        @RequestParam("gameId") gameId: Int,
    ) = gameStatsService.generateGameStats(gameId)

    /**
     * Generate game stats for all games more recent than a game id
     */
    @PostMapping("/generate/all/more_recent_than")
    fun generateAllGameStatsMoreRecentThanGameId(
        @RequestParam("gameId") gameId: Int,
    ) = gameStatsService.generateGameStatsForGamesMoreRecentThanGameId(gameId)

    /**
     * Generate game stats for all games
     * @return
     */
    @PostMapping("/generate/all")
    fun generateAllGameStats() = gameStatsService.generateAllGameStats()

    /**
     * Get ELO history for a team
     * @param team Team name
     * @param season Season number (optional, null for all-time)
     * @return List of ELO history entries
     */
    @GetMapping("/elo-history")
    fun getEloHistory(
        @RequestParam team: String,
        @RequestParam(required = false) season: Int?,
    ) = gameStatsService.getEloHistory(team, season)

    /**
     * Get game stats by season and week
     * @param season Season number
     * @param week Week number (optional, null for entire season)
     * @return List of game stats
     */
    @GetMapping("/by-season-week")
    fun getGameStatsBySeasonAndWeek(
        @RequestParam season: Int,
        @RequestParam(required = false) week: Int?,
    ) = gameStatsService.getGameStatsBySeasonAndWeek(season, week)
}
