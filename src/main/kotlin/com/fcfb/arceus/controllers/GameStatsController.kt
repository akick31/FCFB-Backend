package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.GameStatsService
import io.swagger.v3.oas.annotations.Operation
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
    @Operation(summary = "Get team game stats")
    @GetMapping("")
    fun getGameStats(
        @RequestParam(required = false) gameId: Int?,
        @RequestParam team: String,
        @RequestParam(required = false) season: Int?,
    ): ResponseEntity<Any> = ResponseEntity.ok(gameStatsService.getGameStats(gameId, team, season))

    @Operation(summary = "Generate game stats")
    @PostMapping("/generate")
    fun generateGameStats(
        @RequestParam("gameId") gameId: Int,
    ) = gameStatsService.generateGameStats(gameId)

    @Operation(summary = "Generate game stats since ID")
    @PostMapping("/generate/all/more_recent_than")
    fun generateAllGameStatsMoreRecentThanGameId(
        @RequestParam("gameId") gameId: Int,
    ) = gameStatsService.generateGameStatsForGamesMoreRecentThanGameId(gameId)

    @Operation(summary = "Generate all game stats")
    @PostMapping("/generate/all")
    fun generateAllGameStats() = gameStatsService.generateAllGameStats()

    @Operation(summary = "Get team ELO history")
    @GetMapping("/elo-history")
    fun getEloHistory(
        @RequestParam team: String,
        @RequestParam(required = false) season: Int?,
    ) = gameStatsService.getEloHistory(team, season)

    @Operation(summary = "Get game stats by week")
    @GetMapping("/by-season-week")
    fun getGameStatsBySeasonAndWeek(
        @RequestParam season: Int,
        @RequestParam(required = false) week: Int?,
    ) = gameStatsService.getGameStatsBySeasonAndWeek(season, week)
}
