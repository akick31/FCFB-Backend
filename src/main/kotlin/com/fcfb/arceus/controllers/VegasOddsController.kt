package com.fcfb.arceus.controllers

import com.fcfb.arceus.models.response.VegasOddsResponse
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.service.fcfb.VegasOddsService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
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
    private val logger = LoggerFactory.getLogger(VegasOddsController::class.java)

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
    ): ResponseEntity<VegasOddsResponse> {
        try {
            logger.info("Getting Vegas odds for $homeTeamName vs $awayTeamName")

            val homeTeam = teamService.getTeamByName(homeTeamName)
            val awayTeam = teamService.getTeamByName(awayTeamName)

            val odds = vegasOddsService.calculateVegasOdds(homeTeam = homeTeam, awayTeam = awayTeam)
            return ResponseEntity.ok(odds)
        } catch (e: Exception) {
            logger.error("Error getting Vegas odds for teams: ${e.message}", e)
            return ResponseEntity.badRequest().build()
        }
    }

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
    ): ResponseEntity<VegasOddsResponse> {
        try {
            logger.info("Getting Vegas odds for ELO: $homeElo vs $awayElo")

            val odds = vegasOddsService.calculateVegasOdds(homeElo, awayElo)
            return ResponseEntity.ok(odds)
        } catch (e: Exception) {
            logger.error("Error getting Vegas odds for ELO: ${e.message}", e)
            return ResponseEntity.internalServerError().build()
        }
    }
}
