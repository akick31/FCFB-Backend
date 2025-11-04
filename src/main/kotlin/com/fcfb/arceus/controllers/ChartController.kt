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
    ) = chartService.getScoreChart(gameId)

    /**
     * Generate score charts for all games between two teams in a season
     * @param season The season number
     * @param firstTeam
     * @param secondTeam
     * @return List of PNG images of score charts
     */
    @GetMapping("/score/matchup")
    fun getScoreChartsBySeasonAndMatchup(
        @RequestParam season: Int,
        @RequestParam firstTeam: String,
        @RequestParam secondTeam: String,
    ) = chartService.getScoreChartBySeasonAndMatchup(season, firstTeam, secondTeam)

    /**
     * Generate a win probability chart for a specific game
     * @param gameId The ID of the game to generate a chart for
     * @return PNG image of the win probability chart
     */
    @GetMapping("/win-probability")
    fun getWinProbabilityChart(
        @RequestParam gameId: Int,
    ) = chartService.getWinProbabilityChart(gameId)

    /**
     * Generate win probability charts for all games between two teams in a season
     * @param season The season number
     * @param firstTeam
     * @param secondTeam
     * @return List of PNG images of win probability charts
     */
    @GetMapping("/win-probability/matchup")
    fun getWinProbabilityChartsBySeasonAndMatchup(
        @RequestParam season: Int,
        @RequestParam firstTeam: String,
        @RequestParam secondTeam: String,
    ) = chartService.getWinProbabilityChartBySeasonAndMatchup(season, firstTeam, secondTeam)

    /**
     * Generate an ELO chart for all teams in a season
     * @param season The season number
     * @return PNG image of the ELO chart
     */
    @GetMapping("/elo")
    fun getEloChart(
        @RequestParam season: Int,
    ) = chartService.getEloChart(season)
}
