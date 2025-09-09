package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.GameSpreadResult
import com.fcfb.arceus.dto.UpdateSpreadsResponse
import com.fcfb.arceus.dto.VegasOddsResponse
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service
class VegasOddsService(
    private val gameRepository: GameRepository,
    private val gameStatsRepository: GameStatsRepository,
) {
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

    /**
     * Calculate and update Vegas spreads for all games in a specific season and week
     * using team_elo from game_stats
     * @param season Season number
     * @param week Week number
     * @return Response indicating success and number of games updated
     */
    fun updateSpreadsForSeasonAndWeek(
        season: Int,
        week: Int,
    ): ResponseEntity<UpdateSpreadsResponse> {
        return try {
            logger.info("Updating Vegas spreads for season $season, week $week")

            // Get all games for the specified season and week
            val games = gameRepository.getGamesBySeasonAndWeek(season, week)
            if (games.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(
                        UpdateSpreadsResponse(
                            message = "No games found for season $season, week $week",
                            season = season,
                            week = week,
                            totalGames = 0,
                            updatedGames = 0,
                            results = emptyList(),
                        ),
                    )
            }

            // Get all game stats for the specified season and week
            val gameStatsList = gameStatsRepository.getGameStatsBySeasonAndWeek(season, week)
            if (gameStatsList.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(
                        UpdateSpreadsResponse(
                            message = "No game stats found for season $season, week $week",
                            season = season,
                            week = week,
                            totalGames = games.size,
                            updatedGames = 0,
                            results = emptyList(),
                        ),
                    )
            }

            // Group game stats by game ID for easy lookup
            val gameStatsByGameId = gameStatsList.groupBy { it.gameId }

            var updatedGames = 0
            val results = mutableListOf<GameSpreadResult>()

            for (game in games) {
                val gameStats = gameStatsByGameId[game.gameId]
                if (gameStats == null || gameStats.size != 2) {
                    logger.warn("Skipping game ${game.gameId}: Expected 2 game stats, found ${gameStats?.size ?: 0}")
                    continue
                }

                // Find home and away team stats
                val homeStats = gameStats.find { it.team == game.homeTeam }
                val awayStats = gameStats.find { it.team == game.awayTeam }

                if (homeStats == null || awayStats == null) {
                    logger.warn("Skipping game ${game.gameId}: Missing team stats for home=${game.homeTeam} or away=${game.awayTeam}")
                    continue
                }

                // Calculate spreads using team_elo from game_stats
                val homeSpread = calculateVegasSpread(homeStats.teamElo, awayStats.teamElo)
                val awaySpread = calculateVegasSpread(awayStats.teamElo, homeStats.teamElo)

                // Update the game with the calculated spreads
                game.homeVegasSpread = homeSpread
                game.awayVegasSpread = awaySpread

                // Save the updated game
                gameRepository.save(game)

                updatedGames++
                results.add(
                    GameSpreadResult(
                        gameId = game.gameId,
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        homeElo = homeStats.teamElo,
                        awayElo = awayStats.teamElo,
                        homeSpread = homeSpread,
                        awaySpread = awaySpread,
                    ),
                )

                logger.info(
                    "Updated spreads for game ${game.gameId}: ${game.homeTeam} (${homeStats.teamElo.toInt()}) " +
                        "vs ${game.awayTeam} (${awayStats.teamElo.toInt()}) - Home: $homeSpread, Away: $awaySpread",
                )
            }

            ResponseEntity.ok(
                UpdateSpreadsResponse(
                    message = "Successfully updated Vegas spreads",
                    season = season,
                    week = week,
                    totalGames = games.size,
                    updatedGames = updatedGames,
                    results = results,
                ),
            )
        } catch (e: Exception) {
            logger.error("Error updating Vegas spreads for season $season, week $week: ${e.message}", e)
            return ResponseEntity.internalServerError()
                .body(
                    UpdateSpreadsResponse(
                        message = "Failed to update Vegas spreads: ${e.message}",
                        season = season,
                        week = week,
                        totalGames = 0,
                        updatedGames = 0,
                        results = emptyList(),
                    ),
                )
        }
    }
}
