package com.fcfb.arceus.service.fcfb.probability

import com.fcfb.arceus.dto.response.GameWinProbabilitiesResponse
import com.fcfb.arceus.dto.response.PlayWinProbabilityResponse
import com.fcfb.arceus.dto.response.ProcessedGameResult
import com.fcfb.arceus.dto.response.SingleGameWinProbabilitiesResponse
import com.fcfb.arceus.dto.response.SinglePlayWinProbabilityResponse
import com.fcfb.arceus.dto.response.WinProbabilitiesForAllGamesResponse
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.play.PlayService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WinProbabilityOrchestrationService(
    private val winProbabilityService: WinProbabilityService,
    private val gameService: GameService,
    private val playService: PlayService,
    private val teamService: TeamService,
    private val gameStatsService: GameStatsService,
) {
    private val logger = LoggerFactory.getLogger(WinProbabilityOrchestrationService::class.java)

    fun calculateWinProbabilityForGame(gameId: Int): SingleGameWinProbabilitiesResponse {
        val game = gameService.getGameById(gameId)
        return calculateWinProbabilitiesForSingleGame(
            gameId,
            game,
            playService.getAllPlaysByGameId(gameId),
            teamService.getTeamByName(game.homeTeam),
            teamService.getTeamByName(game.awayTeam),
        )
    }

    fun calculateWinProbabilityForAllGames(): WinProbabilitiesForAllGamesResponse =
        calculateWinProbabilitiesForAllGames(gameService.getAllGames())

    fun getWinProbabilitiesForGame(gameId: Int): GameWinProbabilitiesResponse =
        getWinProbabilitiesForGame(gameId, playService.getAllPlaysByGameId(gameId))

    private fun getWinProbabilityForEachTeam(play: Play): Pair<Double, Double> {
        val homeTeamWinProbability =
            if (play.possession == TeamSide.HOME) {
                play.winProbability ?: 0.0
            } else {
                1.0 - (play.winProbability ?: 0.0)
            }
        val awayTeamWinProbability =
            if (play.possession == TeamSide.AWAY) {
                play.winProbability ?: 0.0
            } else {
                1.0 - (play.winProbability ?: 0.0)
            }
        return Pair(homeTeamWinProbability, awayTeamWinProbability)
    }

    fun calculateWinProbabilitiesForSingleGame(
        gameId: Int,
        game: Game,
        plays: List<Play>,
        homeTeam: Team,
        awayTeam: Team,
    ): SingleGameWinProbabilitiesResponse {
        val gameStats = gameStatsService.getGameStatsById(gameId)
        val statsMap = gameStats.associateBy { it.team }

        val currentHomeElo = statsMap[game.homeTeam]?.teamElo ?: homeTeam.currentElo
        val currentAwayElo = statsMap[game.awayTeam]?.teamElo ?: awayTeam.currentElo

        var previousPlay: Play? = null
        val processedPlays = mutableListOf<SinglePlayWinProbabilityResponse>()

        val sortedPlays = plays.sortedBy { it.playNumber }

        for (play in sortedPlays) {
            val homeElo = statsMap[game.homeTeam]?.teamElo ?: homeTeam.currentElo
            val awayElo = statsMap[game.awayTeam]?.teamElo ?: awayTeam.currentElo

            val winProbability = winProbabilityService.calculateWinProbability(game, play, homeElo, awayElo)

            val winProbabilityAdded =
                previousPlay?.let { prev ->
                    val prevWinProb =
                        if (prev.possession == TeamSide.HOME) {
                            prev.winProbability ?: 0.0
                        } else {
                            1.0 - (prev.winProbability ?: 0.0)
                        }
                    val currentWinProb =
                        if (play.possession == TeamSide.HOME) {
                            winProbability
                        } else {
                            1.0 - winProbability
                        }

                    currentWinProb - prevWinProb
                } ?: 0.0

            play.winProbability = winProbability
            play.winProbabilityAdded = winProbabilityAdded

            playService.updatePlay(play)

            processedPlays.add(
                SinglePlayWinProbabilityResponse(
                    playId = play.playId,
                    playNumber = play.playNumber,
                    quarter = play.quarter,
                    clock = play.clock,
                    homeScore = play.homeScore,
                    awayScore = play.awayScore,
                    winProbability = winProbability,
                    winProbabilityAdded = winProbabilityAdded,
                    possession = play.possession.name,
                    possessionTeam = if (play.possession == TeamSide.HOME) game.homeTeam else game.awayTeam,
                    ballLocation = play.ballLocation,
                    down = play.down,
                    distance = play.yardsToGo,
                    playCall = play.playCall?.name,
                    homeElo = currentHomeElo,
                    awayElo = currentAwayElo,
                ),
            )

            previousPlay = play
        }

        return SingleGameWinProbabilitiesResponse(
            gameId = gameId,
            homeTeam = game.homeTeam,
            awayTeam = game.awayTeam,
            totalPlays = plays.size,
            processedPlays = processedPlays.size,
            plays = processedPlays,
        )
    }

    fun calculateWinProbabilitiesForAllGames(games: List<Game>): WinProbabilitiesForAllGamesResponse {
        var totalGamesProcessed = 0
        var totalPlaysProcessed = 0
        val processedGames = mutableListOf<ProcessedGameResult>()

        games.forEach { game ->
            try {
                val plays = playService.getAllPlaysByGameId(game.gameId)
                if (plays.isNotEmpty()) {
                    val homeTeam = teamService.getTeamByName(game.homeTeam)
                    val awayTeam = teamService.getTeamByName(game.awayTeam)

                    val singleGameResult =
                        calculateWinProbabilitiesForSingleGame(
                            game.gameId,
                            game,
                            plays,
                            homeTeam,
                            awayTeam,
                        )

                    processedGames.add(
                        ProcessedGameResult(
                            gameId = game.gameId,
                            homeTeam = game.homeTeam,
                            awayTeam = game.awayTeam,
                            playsProcessed = singleGameResult.processedPlays,
                        ),
                    )

                    totalPlaysProcessed += singleGameResult.processedPlays
                    totalGamesProcessed++
                }
            } catch (e: Exception) {
                logger.error("Error processing game ${game.gameId}: ${e.message}")
            }
        }

        return WinProbabilitiesForAllGamesResponse(
            totalGames = games.size,
            gamesProcessed = totalGamesProcessed,
            totalPlaysProcessed = totalPlaysProcessed,
            processedGames = processedGames,
        )
    }

    fun getWinProbabilitiesForGame(
        gameId: Int,
        plays: List<Play>,
    ): GameWinProbabilitiesResponse {
        val results =
            plays.sortedBy { it.playNumber }.map { play ->
                val (homeTeamWinProbability, awayTeamWinProbability) = getWinProbabilityForEachTeam(play)

                PlayWinProbabilityResponse(
                    playNumber = play.playNumber,
                    quarter = play.quarter,
                    clock = play.clock,
                    homeScore = play.homeScore,
                    awayScore = play.awayScore,
                    homeTeamWinProbability = homeTeamWinProbability,
                    awayTeamWinProbability = awayTeamWinProbability,
                )
            }

        return GameWinProbabilitiesResponse(
            gameId = gameId,
            totalPlays = plays.size,
            plays = results,
        )
    }
}
