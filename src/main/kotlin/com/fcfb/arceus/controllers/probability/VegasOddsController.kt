package com.fcfb.arceus.controllers.probability

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.service.fcfb.probability.VegasOddsService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/vegas-odds")
class VegasOddsController(
    private val vegasOddsService: VegasOddsService,
) {
    @Operation(summary = "Matchup Vegas odds")
    @GetMapping("")
    fun getVegasOddsByTeams(
        @RequestParam homeTeamName: String,
        @RequestParam awayTeamName: String,
    ) = vegasOddsService.getVegasOddsByTeams(homeTeamName, awayTeamName)

    @Operation(summary = "Vegas odds from ELO ratings")
    @GetMapping("/elo")
    fun getVegasOddsByElo(
        @RequestParam homeElo: Double,
        @RequestParam awayElo: Double,
    ) = vegasOddsService.getVegasOddsByElo(homeElo, awayElo)

    @Operation(summary = "Update Vegas spreads")
    @PostMapping("/update-spreads")
    fun updateSpreadsForSeasonAndWeek(
        @RequestParam season: Int,
        @RequestParam week: Int,
    ) = vegasOddsService.updateSpreadsForSeasonAndWeek(season, week)
}
