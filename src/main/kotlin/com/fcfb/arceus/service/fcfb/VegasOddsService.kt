package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.VegasOddsResponse
import com.fcfb.arceus.model.Team
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service
class VegasOddsService {
    private val logger = LoggerFactory.getLogger(VegasOddsService::class.java)

    /**
     * Calculate Vegas odds for a matchup based on team ELO ratings
     * @param homeTeam Home team
     * @param awayTeam Away team
     * @return VegasOddsResponse with home and away spreads
     */
    fun calculateVegasOdds(
        homeTeam: Team,
        awayTeam: Team,
    ): VegasOddsResponse {
        val homeElo = homeTeam.currentElo
        val awayElo = awayTeam.currentElo

        logger.info("Calculating Vegas odds: ${homeTeam.name} (${homeElo.toInt()}) vs ${awayTeam.name} (${awayElo.toInt()})")

        val homeSpread = calculateVegasSpread(homeElo, awayElo)
        val awaySpread = calculateVegasSpread(awayElo, homeElo)

        return VegasOddsResponse(
            homeTeam = homeTeam.name ?: "Unknown",
            awayTeam = awayTeam.name ?: "Unknown",
            homeSpread = homeSpread,
            awaySpread = awaySpread,
            homeElo = homeElo,
            awayElo = awayElo,
        )
    }

    /**
     * Calculate Vegas odds for a matchup based on custom ELO ratings
     * @param homeElo Home team ELO rating
     * @param awayElo Away team ELO rating
     * @param homeTeamName Optional home team name for response
     * @param awayTeamName Optional away team name for response
     * @return VegasOddsResponse with home and away spreads
     */
    private fun calculateVegasOdds(
        homeElo: Double,
        awayElo: Double,
        homeTeamName: String? = null,
        awayTeamName: String? = null,
    ): VegasOddsResponse {
        val homeSpread = calculateVegasSpread(homeElo, awayElo)
        val awaySpread = calculateVegasSpread(awayElo, homeElo)

        return VegasOddsResponse(
            homeTeam = homeTeamName ?: "Home",
            awayTeam = awayTeamName ?: "Away",
            homeSpread = homeSpread,
            awaySpread = awaySpread,
            homeElo = homeElo,
            awayElo = awayElo,
        )
    }

    /**
     * Get Vegas odds for a matchup based on team names
     */
    fun getVegasOddsByTeams(
        homeTeamName: String,
        awayTeamName: String,
        teamService: TeamService,
    ): ResponseEntity<VegasOddsResponse> =
        try {
            logger.info("Getting Vegas odds for $homeTeamName vs $awayTeamName")

            val homeTeam = teamService.getTeamByName(homeTeamName)
            val awayTeam = teamService.getTeamByName(awayTeamName)

            val odds = calculateVegasOdds(homeTeam = homeTeam, awayTeam = awayTeam)
            ResponseEntity.ok(odds)
        } catch (e: Exception) {
            logger.error("Error getting Vegas odds for teams: ${e.message}", e)
            ResponseEntity.badRequest().build()
        }

    /**
     * Get Vegas odds for a matchup based on custom ELO ratings
     */
    fun getVegasOddsByElo(
        homeElo: Double,
        awayElo: Double,
    ): ResponseEntity<VegasOddsResponse> =
        try {
            logger.info("Getting Vegas odds for ELO: $homeElo vs $awayElo")

            val odds = calculateVegasOdds(homeElo, awayElo)
            ResponseEntity.ok(odds)
        } catch (e: Exception) {
            logger.error("Error getting Vegas odds for ELO: ${e.message}", e)
            ResponseEntity.internalServerError().build()
        }

    /**
     * Calculate the Vegas spread for a team based on ELO difference
     * This is based on the standard ELO to point spread conversion
     * @param teamElo The team's ELO rating
     * @param opponentElo The opponent's ELO rating
     * @return The point spread (negative means team is favored, positive means underdog)
     */
    private fun calculateVegasSpread(
        teamElo: Double,
        opponentElo: Double,
    ): Double {
        // Standard ELO to point spread conversion: ~3 points per 100 ELO difference
        // Add home field advantage (~2.5 points)
        val eloDifference = teamElo - opponentElo
        val spread = (eloDifference / 100.0) * 3.0 + 2.5

        // Round to nearest 0.5 (standard Vegas practice)
        // Return negative for favored team, positive for underdog
        return -((spread * 2).roundToInt() / 2.0)
    }
}
