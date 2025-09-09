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
    /**
     * Generate a score chart for a specific game
     * @param gameId The ID of the game to generate a chart for
     * @return PNG image of the score chart
     */
    @GetMapping("/score")
    fun getScoreChart(
        @RequestParam gameId: Int,
    ) = chartService.getScoreChartResponse(gameId)

    /**
     * Generate a win probability chart for a specific game
     * @param gameId The ID of the game to generate a chart for
     * @return PNG image of the win probability chart
     */
    @GetMapping("/win-probability")
    fun getWinProbabilityChart(
        @RequestParam gameId: Int,
    ) = chartService.getWinProbabilityChartResponse(gameId)

    /**
     * Generate an ELO chart for all teams in a season
     * @param season The season number
     * @return PNG image of the ELO chart
     */
    @GetMapping("/elo")
    fun getEloChart(
        @RequestParam season: Int,
    ) = chartService.getEloChartResponse(season)
}
