package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.service.fcfb.VegasOddsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/vegas-odds")
class VegasOddsController(
    private val vegasOddsService: VegasOddsService,
    private val teamService: TeamService,
) {
    /**
     * Get Vegas odds for a matchup based on team names
     * @param homeTeamName Home team name
     * @param awayTeamName Away team name
     * @return Vegas odds response
     */
    @GetMapping("")
    fun getVegasOddsByTeams(
        @RequestParam homeTeamName: String,
        @RequestParam awayTeamName: String,
    ) = vegasOddsService.getVegasOddsByTeams(homeTeamName, awayTeamName, teamService)

    /**
     * Get Vegas odds for a matchup based on custom ELO ratings
     * @param homeElo Home team ELO rating
     * @param awayElo Away team ELO rating
     * @return Vegas odds response
     */
    @GetMapping("/elo")
    fun getVegasOddsByElo(
        @RequestParam homeElo: Double,
        @RequestParam awayElo: Double,
    ) = vegasOddsService.getVegasOddsByElo(homeElo, awayElo)
}
