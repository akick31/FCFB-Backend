package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.ChartService
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
    @GetMapping("/score")
    fun getScoreChart(
        @RequestParam gameId: Int,
    ) = chartService.getScoreChart(gameId)

    @GetMapping("/score/matchup")
    fun getScoreChartsBySeasonAndMatchup(
        @RequestParam season: Int,
        @RequestParam firstTeam: String,
        @RequestParam secondTeam: String,
    ) = chartService.getScoreChartBySeasonAndMatchup(season, firstTeam, secondTeam)

    @GetMapping("/win-probability")
    fun getWinProbabilityChart(
        @RequestParam gameId: Int,
    ) = chartService.getWinProbabilityChart(gameId)

    @GetMapping("/win-probability/matchup")
    fun getWinProbabilityChartsBySeasonAndMatchup(
        @RequestParam season: Int,
        @RequestParam firstTeam: String,
        @RequestParam secondTeam: String,
    ) = chartService.getWinProbabilityChartBySeasonAndMatchup(season, firstTeam, secondTeam)

    @GetMapping("/elo")
    fun getEloChart(
        @RequestParam season: Int,
    ) = chartService.getEloChart(season)
}
