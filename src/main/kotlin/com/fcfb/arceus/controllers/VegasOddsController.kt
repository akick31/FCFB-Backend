package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.service.fcfb.VegasOddsService
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
    private val teamService: TeamService,
) {
    @Operation(summary = "Get Vegas odds for a matchup between two teams")
    @GetMapping("")
    fun getVegasOddsByTeams(
        @RequestParam homeTeamName: String,
        @RequestParam awayTeamName: String,
    ) = vegasOddsService.getVegasOddsByTeams(homeTeamName, awayTeamName, teamService)

    @Operation(summary = "Get Vegas odds calculated from two teams' Elo ratings")
    @GetMapping("/elo")
    fun getVegasOddsByElo(
        @RequestParam homeElo: Double,
        @RequestParam awayElo: Double,
    ) = vegasOddsService.getVegasOddsByElo(homeElo, awayElo)

    @Operation(summary = "Update Vegas spreads for all games in a season and week")
    @PostMapping("/update-spreads")
    fun updateSpreadsForSeasonAndWeek(
        @RequestParam season: Int,
        @RequestParam week: Int,
    ) = vegasOddsService.updateSpreadsForSeasonAndWeek(season, week)
}
