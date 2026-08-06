package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.response.GameSpreadResult
import com.fcfb.arceus.dto.response.UpdateSpreadsResponse
import com.fcfb.arceus.dto.response.VegasOddsResponse
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

    fun calculateVegasOdds(
        homeTeam: Team,
        awayTeam: Team,
    ): VegasOddsResponse {
        val homeElo = homeTeam.currentElo
        val awayElo = awayTeam.currentElo

        logger.info("Calculating Vegas odds: ${homeTeam.name} (${homeElo.toInt()}) vs ${awayTeam.name} (${awayElo.toInt()})")

        val homeSpread = calculateVegasSpread(homeElo, awayElo)
        val awaySpread = -homeSpread

        return VegasOddsResponse(
            homeTeam = homeTeam.name ?: "Unknown",
            awayTeam = awayTeam.name ?: "Unknown",
            homeSpread = homeSpread,
            awaySpread = awaySpread,
            homeElo = homeElo,
            awayElo = awayElo,
        )
    }

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

    private fun calculateVegasSpread(
        homeElo: Double,
        awayElo: Double,
    ): Double {
        val eloDifference = homeElo - awayElo
        val spread = (eloDifference / 100.0) * 3.0 + 2.5

        return -((spread * 2).roundToInt() / 2.0)
    }

    fun updateSpreadsForSeasonAndWeek(
        season: Int,
        week: Int,
    ): ResponseEntity<UpdateSpreadsResponse> {
        return try {
            logger.info("Updating Vegas spreads for season $season, week $week")

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

            val gameStatsByGameId = gameStatsList.groupBy { it.gameId }

            var updatedGames = 0
            val results = mutableListOf<GameSpreadResult>()

            for (game in games) {
                val gameStats = gameStatsByGameId[game.gameId]
                if (gameStats == null || gameStats.size != 2) {
                    logger.warn("Skipping game ${game.gameId}: Expected 2 game stats, found ${gameStats?.size ?: 0}")
                    continue
                }

                val homeStats = gameStats.find { it.team == game.homeTeam }
                val awayStats = gameStats.find { it.team == game.awayTeam }

                if (homeStats == null || awayStats == null) {
                    logger.warn("Skipping game ${game.gameId}: Missing team stats for home=${game.homeTeam} or away=${game.awayTeam}")
                    continue
                }

                val spread = calculateVegasSpread(homeStats.teamElo, awayStats.teamElo)
                val homeSpread = spread
                val awaySpread = -spread

                game.homeVegasSpread = homeSpread
                game.awayVegasSpread = awaySpread

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
