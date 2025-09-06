package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.fcfb.PlayService
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.service.fcfb.WinProbabilityService
import org.springframework.core.io.ClassPathResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/win-probability")
@CrossOrigin(origins = ["*"])
class WinProbabilityController(
    private val winProbabilityService: WinProbabilityService,
    private val teamService: TeamService,
    private val gameService: GameService,
    private val playService: PlayService,
) {
    /**
     * Calculate win probability for a specific game state
     */
    @GetMapping("/calculate")
    fun calculateWinProbability(
        @RequestParam gameId: Int,
        @RequestParam playId: Int,
    ): ResponseEntity<Any> {
        return try {
            val game = gameService.getGameById(gameId)
            val play = playService.getPlayById(playId)
            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            val winProbability = winProbabilityService.calculateWinProbability(game, play, homeTeam, awayTeam)
            val homeElo = winProbabilityService.getCurrentElo(homeTeam)
            val awayElo = winProbabilityService.getCurrentElo(awayTeam)

            val response =
                mapOf(
                    "gameId" to gameId,
                    "playId" to playId,
                    "homeTeam" to game.homeTeam,
                    "awayTeam" to game.awayTeam,
                    "homeScore" to game.homeScore,
                    "awayScore" to game.awayScore,
                    "winProbability" to winProbability,
                    "possession" to play.possession.name,
                    "possessionTeam" to if (play.possession == TeamSide.HOME) game.homeTeam else game.awayTeam,
                    "ballLocation" to play.ballLocation,
                    "down" to play.down,
                    "distance" to play.yardsToGo,
                    "homeElo" to homeElo,
                    "awayElo" to awayElo,
                    "eloDiff" to (homeElo - awayElo),
                )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    /**
     * Get ELO ratings for all teams
     */
    @GetMapping("/elo-ratings")
    fun getEloRatings(): ResponseEntity<Any> {
        return try {
            val teams = teamService.getAllTeams()
            val eloRatings =
                teams.map { team ->
                    mapOf(
                        "teamId" to team.id,
                        "teamName" to team.name,
                        "currentElo" to winProbabilityService.getCurrentElo(team),
                        "overallElo" to team.overallElo,
                    )
                }.sortedByDescending { it["currentElo"] as Double }

            ResponseEntity.ok(eloRatings)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(listOf(mapOf("error" to e.message)))
        }
    }

    /**
     * Initialize ELO ratings for all teams
     */
    @PostMapping("/initialize-elo")
    fun initializeEloRatings(): ResponseEntity<Any> {
        return try {
            val teams = teamService.getAllTeams()
            var initializedCount = 0

            teams.forEach { team ->
                winProbabilityService.initializeEloRatings(team)
                teamService.updateTeam(team)
                initializedCount++
            }

            val response =
                mapOf(
                    "message" to "ELO ratings initialized for $initializedCount teams",
                    "initializedCount" to initializedCount,
                )
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    /**
     * Calculate win probability for all plays in a specific game
     */
    @PostMapping("/calculate-game")
    fun calculateWinProbabilityForGame(
        @RequestParam gameId: Int,
    ): ResponseEntity<Any> {
        return try {
            val game = gameService.getGameById(gameId)
            val plays = playService.getAllPlaysByGameId(gameId)
            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            // Initialize ELO ratings if needed
            winProbabilityService.initializeEloRatings(homeTeam)
            winProbabilityService.initializeEloRatings(awayTeam)

            var processedPlays = 0
            var currentHomeElo = homeTeam.currentElo
            var currentAwayElo = awayTeam.currentElo

            val results =
                plays.sortedBy { it.playNumber }.map { play ->
                    // Calculate win probability for this play
                    val winProbability = winProbabilityService.calculateWinProbability(game, play, homeTeam, awayTeam)

                    // Update play with calculated values
                    play.winProbability = winProbability
                    play.winProbabilityAdded =
                        if (processedPlays > 0) {
                            val previousPlay = plays.find { it.playNumber == play.playNumber - 1 }
                            winProbabilityService.calculateWinProbabilityAdded(play, previousPlay)
                        } else {
                            0.0
                        }

                    // Save the updated play
                    playService.updatePlay(play)
                    processedPlays++

                    mapOf(
                        "playId" to play.playId,
                        "playNumber" to play.playNumber,
                        "quarter" to play.quarter,
                        "clock" to play.clock,
                        "homeScore" to play.homeScore,
                        "awayScore" to play.awayScore,
                        "winProbability" to winProbability,
                        "winProbabilityAdded" to play.winProbabilityAdded,
                        "possession" to play.possession.name,
                        "possessionTeam" to if (play.possession == TeamSide.HOME) game.homeTeam else game.awayTeam,
                        "ballLocation" to play.ballLocation,
                        "down" to play.down,
                        "distance" to play.yardsToGo,
                        "homeElo" to currentHomeElo,
                        "awayElo" to currentAwayElo,
                    )
                }

            val response =
                mapOf(
                    "gameId" to gameId,
                    "homeTeam" to game.homeTeam,
                    "awayTeam" to game.awayTeam,
                    "totalPlays" to plays.size,
                    "processedPlays" to processedPlays,
                    "plays" to results,
                )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    /**
     * Calculate win probability for ALL games in the database
     */
    @PostMapping("/calculate-all-games")
    fun calculateWinProbabilityForAllGames(): ResponseEntity<Any> {
        return try {
            // Get all games (you'll need to add this method to GameService if it doesn't exist)
            val games = gameService.getAllGames()
            var totalGamesProcessed = 0
            var totalPlaysProcessed = 0
            val gameResults = mutableListOf<Map<String, Any>>()

            games.forEach { game ->
                try {
                    val plays = playService.getAllPlaysByGameId(game.gameId)
                    if (plays.isNotEmpty()) {
                        val homeTeam = teamService.getTeamByName(game.homeTeam)
                        val awayTeam = teamService.getTeamByName(game.awayTeam)

                        // Initialize ELO ratings if needed
                        winProbabilityService.initializeEloRatings(homeTeam)
                        winProbabilityService.initializeEloRatings(awayTeam)

                        var playsProcessed = 0
                        plays.sortedBy { it.playNumber }.forEach { play ->
                            val winProbability = winProbabilityService.calculateWinProbability(game, play, homeTeam, awayTeam)

                            play.winProbability = winProbability
                            play.winProbabilityAdded =
                                if (playsProcessed > 0) {
                                    val previousPlay = plays.find { it.playNumber == play.playNumber - 1 }
                                    winProbabilityService.calculateWinProbabilityAdded(play, previousPlay)
                                } else {
                                    0.0
                                }

                            playService.updatePlay(play)
                            playsProcessed++
                            totalPlaysProcessed++
                        }

                        gameResults.add(
                            mapOf(
                                "gameId" to game.gameId,
                                "homeTeam" to game.homeTeam,
                                "awayTeam" to game.awayTeam,
                                "playsProcessed" to playsProcessed,
                            ),
                        )

                        totalGamesProcessed++
                    }
                } catch (e: Exception) {
                    // Log error but continue with other games
                    println("Error processing game ${game.gameId}: ${e.message}")
                }
            }

            val response =
                mapOf(
                    "totalGames" to games.size,
                    "gamesProcessed" to totalGamesProcessed,
                    "totalPlaysProcessed" to totalPlaysProcessed,
                    "gameResults" to gameResults,
                )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    /**
     * Get the win probability model configuration
     */
    @GetMapping("/model-config")
    fun getModelConfig(): ResponseEntity<Any> {
        return try {
            val resource = ClassPathResource("win-probability-model.json")
            val modelJson = resource.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            ResponseEntity.ok(modelJson)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    /**
     * Get model information and parameters
     */
    @GetMapping("/model-info")
    fun getModelInfo(): ResponseEntity<Any> {
        return try {
            val modelInfo = winProbabilityService.getModelInfo()
            ResponseEntity.ok(modelInfo)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    /**
     * Get win probability for a specific team
     * @param gameId The game ID
     * @param playId The play ID
     * @param teamSide The team side (HOME or AWAY)
     * @return Win probability for the specified team
     */
    @GetMapping("/team-probability")
    fun getWinProbabilityForTeam(
        @RequestParam gameId: Int,
        @RequestParam playId: Int,
        @RequestParam teamSide: String,
    ): ResponseEntity<Any> {
        return try {
            val game = gameService.getGameById(gameId)
            val play = playService.getPlayById(playId)
            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            val teamSideEnum =
                when (teamSide.uppercase()) {
                    "HOME" -> TeamSide.HOME
                    "AWAY" -> TeamSide.AWAY
                    else -> throw IllegalArgumentException("teamSide must be 'HOME' or 'AWAY'")
                }

            val winProbability =
                winProbabilityService.getWinProbabilityForTeam(
                    game,
                    play,
                    homeTeam,
                    awayTeam,
                    teamSideEnum,
                )

            ResponseEntity.ok(
                mapOf(
                    "gameId" to gameId,
                    "playId" to playId,
                    "teamSide" to teamSide,
                    "teamName" to if (teamSideEnum == TeamSide.HOME) game.homeTeam else game.awayTeam,
                    "winProbability" to winProbability,
                    "possession" to play.possession.name,
                    "possessionTeam" to if (play.possession == TeamSide.HOME) game.homeTeam else game.awayTeam,
                    "note" to "Win probability is always calculated for HOME team, then flipped for AWAY team",
                ),
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    /**
     * Get win probability for both teams (convenience endpoint)
     * @param gameId The game ID
     * @param playId The play ID
     * @return Win probabilities for both teams
     */
    @GetMapping("/both-teams-probability")
    fun getBothTeamsWinProbability(
        @RequestParam gameId: Int,
        @RequestParam playId: Int,
    ): ResponseEntity<Any> {
        return try {
            val game = gameService.getGameById(gameId)
            val play = playService.getPlayById(playId)
            val homeTeam = teamService.getTeamByName(game.homeTeam)
            val awayTeam = teamService.getTeamByName(game.awayTeam)

            val homeTeamProbability = winProbabilityService.calculateWinProbability(game, play, homeTeam, awayTeam)
            val awayTeamProbability = winProbabilityService.getAwayTeamWinProbability(game, play, homeTeam, awayTeam)

            ResponseEntity.ok(
                mapOf(
                    "gameId" to gameId,
                    "playId" to playId,
                    "homeTeam" to game.homeTeam,
                    "awayTeam" to game.awayTeam,
                    "homeTeamWinProbability" to homeTeamProbability,
                    "awayTeamWinProbability" to awayTeamProbability,
                    "possession" to play.possession.name,
                    "possessionTeam" to if (play.possession == TeamSide.HOME) game.homeTeam else game.awayTeam,
                ),
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
}
