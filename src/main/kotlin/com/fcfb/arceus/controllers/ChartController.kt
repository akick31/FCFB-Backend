package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.ChartService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/chart")
class ChartController(
    private val chartService: ChartService,
) {
    @Operation(summary = "Get the score chart for a game")
    @GetMapping("/score")
    fun getScoreChart(
        @RequestParam gameId: Int,
    ) = chartService.getScoreChart(gameId)

    @Operation(summary = "Get the score charts for a season matchup between two teams")
    @GetMapping("/score/matchup")
    fun getScoreChartsBySeasonAndMatchup(
        @RequestParam season: Int,
        @RequestParam firstTeam: String,
        @RequestParam secondTeam: String,
    ) = chartService.getScoreChartBySeasonAndMatchup(season, firstTeam, secondTeam)

    @Operation(summary = "Get the win probability chart for a game")
    @GetMapping("/win-probability")
    fun getWinProbabilityChart(
        @RequestParam gameId: Int,
    ) = chartService.getWinProbabilityChart(gameId)

    @Operation(summary = "Get the win probability charts for a season matchup between two teams")
    @GetMapping("/win-probability/matchup")
    fun getWinProbabilityChartsBySeasonAndMatchup(
        @RequestParam season: Int,
        @RequestParam firstTeam: String,
        @RequestParam secondTeam: String,
    ) = chartService.getWinProbabilityChartBySeasonAndMatchup(season, firstTeam, secondTeam)

    @Operation(summary = "Get the Elo chart for a season")
    @GetMapping("/elo")
    fun getEloChart(
        @RequestParam season: Int,
    ) = chartService.getEloChart(season)
}
