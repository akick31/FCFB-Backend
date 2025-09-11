package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.EloRatingResponse
import com.fcfb.arceus.dto.GameWinProbabilitiesResponse
import com.fcfb.arceus.dto.PlayWinProbabilityResponse
import com.fcfb.arceus.dto.ProcessedGameResult
import com.fcfb.arceus.dto.SingleGameWinProbabilitiesResponse
import com.fcfb.arceus.dto.SinglePlayWinProbabilityResponse
import com.fcfb.arceus.dto.WinProbabilitiesForAllGamesResponse
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.util.ml.XGBoostPredictor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class WinProbabilityService(
    private val xgboostPredictor: XGBoostPredictor,
    private val playRepository: PlayRepository,
    private val gameStatsService: GameStatsService,
) {
    private val logger = LoggerFactory.getLogger(WinProbabilityService::class.java)

    // ELO parameters
    private val kFactor = 32.0

    fun calculateWinProbability(
        game: Game,
        play: Play,
        homeElo: Double,
        awayElo: Double,
    ): Double {
        try {
            val scoreDiff = play.homeScore - play.awayScore
            val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
            val down = play.down
            val yardsToGo = play.yardsToGo
            val ballLocation = 100 - play.ballLocation

            // Calculate additional features needed for model
            val secondsLeftGame = timeRemaining
            val secondsLeftHalf = calculateSecondsLeftHalf(play.quarter, play.clock)
            val half = if (play.quarter <= 2) 1 else 2
            val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
            val eloDiffTime =
                if (play.possession == TeamSide.HOME) {
                    calculateEloDiffTime(homeElo, awayElo, secondsLeftGame)
                } else {
                    calculateEloDiffTime(awayElo, homeElo, secondsLeftGame)
                }

            // Create feature array for XGBoost model
            // Calculate margin from the possessing team's perspective
            val margin =
                if (play.possession == TeamSide.HOME) {
                    scoreDiff
                } else {
                    -scoreDiff
                }

            val features =
                xgboostPredictor.createFeatureArray(
                    down = down,
                    distance = yardsToGo,
                    position = ballLocation,
                    margin = margin,
                    secondsLeftGame = secondsLeftGame,
                    secondsLeftHalf = secondsLeftHalf,
                    half = half,
                    hadFirstPossession = hadFirstPossession,
                    eloDiffTime = eloDiffTime,
                )

            // Handle special play types first (like Python code)
            val winProbability =
                when (play.playCall) {
                    PlayCall.PAT ->
                        calculatePatWinProbability(
                            margin,
                            timeRemaining,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    PlayCall.TWO_POINT ->
                        calculateTwoPointWinProbability(
                            margin,
                            timeRemaining,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    PlayCall.KICKOFF_NORMAL ->
                        calculateKickoffWinProbability(
                            margin,
                            timeRemaining,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    PlayCall.KICKOFF_ONSIDE ->
                        calculateKickoffOnsideWinProbability(
                            margin,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    PlayCall.KICKOFF_SQUIB ->
                        calculateKickoffSquibWinProbability(
                            margin,
                            secondsLeftHalf,
                            half,
                            hadFirstPossession,
                            eloDiffTime,
                        )
                    else -> {
                        // Regular play - use XGBoost model
                        val rawWinProbability = xgboostPredictor.predict(features)
                        rawWinProbability
                    }
                }

            // Calculate win probability added
            val winProbabilityAdded = calculateWinProbabilityAdded(game, play, winProbability)

            // Set the win probability and change on the play
            play.winProbability = winProbability
            play.winProbabilityAdded = winProbabilityAdded

            return winProbability
        } catch (e: Exception) {
            logger.error("Error calculating win probability: ${e.message}", e)
            val defaultProbability = 0.5
            play.winProbability = defaultProbability
            play.winProbabilityAdded = 0.0
            return defaultProbability
        }
    }

    /**
     * Calculate win probability added for a play
     */
    private fun calculateWinProbabilityAdded(
        game: Game,
        play: Play,
        currentWinProbability: Double,
    ): Double {
        // Calculate win probability added using the game's overall win probability as baseline
        val previousWinProbability = game.winProbability ?: 0.5

        // Get the previous play for proper WPA calculation
        val previousPlay =
            try {
                val allPlays = playRepository.getAllPlaysByGameId(game.gameId)
                allPlays.find { it.playNumber == play.playNumber - 1 }
            } catch (e: Exception) {
                null
            }

        val winProbabilityAdded =
            if (previousPlay != null) {
                val currentWinProb = play.winProbability ?: 0.0
                val previousWinProb = previousPlay.winProbability ?: 0.0

                // If possession changed, we need to account for the perspective flip
                if (play.possession != previousPlay.possession) {
                    // When possession changes, the win probability flips perspective
                    // We need to convert both to HOME team's perspective to calculate the actual change

                    val previousHomeWinProb =
                        if (previousPlay.possession == TeamSide.HOME) {
                            previousWinProb
                        } else {
                            1.0 - previousWinProb
                        }

                    val currentHomeWinProb =
                        if (play.possession == TeamSide.HOME) {
                            currentWinProb
                        } else {
                            1.0 - currentWinProb
                        }

                    return currentHomeWinProb - previousHomeWinProb
                } else {
                    // Same possession, so direct difference
                    return currentWinProb - previousWinProb
                }
            } else {
                // Fallback to simple difference for first play
                currentWinProbability - previousWinProbability
            }
        return winProbabilityAdded
    }

    /**
     * Update ELO ratings after a game
     */
    fun updateEloRatings(
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
    ) {
        try {
            val homeScore = game.homeScore
            val awayScore = game.awayScore
            val homeWon = homeScore > awayScore

            // Use K-factor from model parameters
            val expectedHome = calculateExpectedScore(homeTeam.currentElo, awayTeam.currentElo)
            val expectedAway = 1.0 - expectedHome

            val actualHome = if (homeWon) 1.0 else 0.0
            val actualAway = 1.0 - actualHome

            val newHomeElo = homeTeam.currentElo + kFactor * (actualHome - expectedHome)
            val newAwayElo = awayTeam.currentElo + kFactor * (actualAway - expectedAway)

            homeTeam.currentElo = newHomeElo
            awayTeam.currentElo = newAwayElo

            logger.info("Updated ELO ratings - ${game.homeTeam}: ${newHomeElo.toInt()}, ${game.awayTeam}: ${newAwayElo.toInt()}")
        } catch (e: Exception) {
            logger.error("Error updating ELO ratings: ${e.message}", e)
        }
    }

    /**
     * Calculate time remaining in seconds - matches Python implementation
     */
    private fun calculateTimeRemaining(
        quarter: Int,
        clock: Int,
    ): Double {
        // Python: seconds_left_game = 1680-(420-int(clock)) for Q1, etc.
        val timeRemaining =
            when (quarter) {
                1 -> 1680 - (420 - clock) // 1680 - (420 - clock)
                2 -> 1260 - (420 - clock) // 1260 - (420 - clock)
                3 -> 840 - (420 - clock) // 840 - (420 - clock)
                4 -> 420 - (420 - clock) // 420 - (420 - clock)
                else -> 0.0
            }
        return maxOf(0.0, timeRemaining.toDouble())
    }

    /**
     * Calculate seconds left in half - matches Python implementation
     */
    private fun calculateSecondsLeftHalf(
        quarter: Int,
        clock: Int,
    ): Int {
        val secondsLeftHalf =
            when (quarter) {
                1 -> 840 - (420 - clock) // 840 - (420 - clock)
                2 -> 420 - (420 - clock) // 420 - (420 - clock)
                3 -> 840 - (420 - clock) // 840 - (420 - clock)
                4 -> 420 - (420 - clock) // 420 - (420 - clock)
                else -> 0
            }
        return maxOf(0, secondsLeftHalf)
    }

    /**
     * Calculate ELO difference with time decay - matches Python implementation
     * elo_diff_time = (float(offense_elo) - float(defense_elo)) * math.exp(-2 * (1 - (seconds_left_game / 1680)))
     */
    private fun calculateEloDiffTime(
        offenseElo: Double,
        defenseElo: Double,
        secondsLeftGame: Int,
    ): Double {
        return (offenseElo - defenseElo) * Math.exp(-2.0 * (1.0 - (secondsLeftGame.toDouble() / 1680.0)))
    }

    /**
     * Calculate who had first possession based on coin toss
     * Returns 1 if the team with current possession had first possession, 0 otherwise
     */
    private fun calculateHadFirstPossession(
        game: Game,
        currentPossession: TeamSide,
    ): Int {
        val coinTossWinner = game.coinTossWinner
        val coinTossChoice = game.coinTossChoice

        // Check if coin toss data is missing
        if (coinTossWinner == null || coinTossChoice == null) {
            logger.warn("Missing coin toss data for game ${game.gameId}. Using default: HOME had first possession")
            return if (currentPossession == TeamSide.HOME) 1 else 0
        }

        // Determine who actually had first possession
        val whoHadFirstPossession =
            when {
                // AWAY wins and defers -> HOME had first possession
                coinTossWinner == TeamSide.AWAY && coinTossChoice == CoinTossChoice.DEFER -> TeamSide.HOME
                // HOME wins and defers -> AWAY had first possession
                coinTossWinner == TeamSide.HOME && coinTossChoice == CoinTossChoice.DEFER -> TeamSide.AWAY
                // AWAY wins and receives -> AWAY had first possession
                coinTossWinner == TeamSide.AWAY && coinTossChoice == CoinTossChoice.RECEIVE -> TeamSide.AWAY
                // HOME wins and receives -> HOME had first possession
                coinTossWinner == TeamSide.HOME && coinTossChoice == CoinTossChoice.RECEIVE -> TeamSide.HOME
                // Default fallback (shouldn't happen in normal games)
                else -> {
                    logger.warn("Unexpected coin toss combination for game ${game.gameId}: winner=$coinTossWinner, choice=$coinTossChoice")
                    TeamSide.HOME
                }
            }

        // Return 1 if current possession team had first possession, 0 otherwise
        return if (currentPossession == whoHadFirstPossession) 1 else 0
    }

    /**
     * Calculate win probability for PAT - matches Python implementation
     */
    private fun calculatePatWinProbability(
        margin: Int,
        timeRemaining: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double {
        // Calculate probabilities for different PAT outcomes
        val probIfSuccess =
            calculateWinProbabilityForScenario(
                75,
                -(margin + 1),
                timeRemaining,
                secondsLeftHalf,
                half,
                hadFirstPossession,
                -eloDiffTime,
            )
        val probIfFail =
            calculateWinProbabilityForScenario(
                75,
                -margin,
                timeRemaining,
                secondsLeftHalf,
                half,
                1 - hadFirstPossession,
                -eloDiffTime,
            )
        val probIfReturn =
            calculateWinProbabilityForScenario(
                75,
                -(margin - 2),
                timeRemaining,
                secondsLeftHalf,
                half,
                1 - hadFirstPossession,
                -eloDiffTime,
            )

        return 1.0 - (
            ((721.0 / 751.0) * probIfSuccess) +
                ((27.0 / 751.0) * probIfFail) +
                ((3.0 / 751.0) * probIfReturn)
        )
    }

    /**
     * Calculate win probability for TWO_POINT - matches Python implementation
     */
    private fun calculateTwoPointWinProbability(
        margin: Int,
        timeRemaining: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double {
        // Calculate probabilities for different TWO_POINT outcomes
        val probIfSuccess =
            calculateWinProbabilityForScenario(
                75,
                -(margin + 2),
                timeRemaining,
                secondsLeftHalf,
                half,
                hadFirstPossession,
                -eloDiffTime,
            )
        val probIfFail =
            calculateWinProbabilityForScenario(
                75,
                -margin,
                timeRemaining,
                secondsLeftHalf,
                half,
                1 - hadFirstPossession,
                -eloDiffTime,
            )
        val probIfReturn =
            calculateWinProbabilityForScenario(
                75,
                -(margin - 2),
                timeRemaining,
                secondsLeftHalf,
                half,
                1 - hadFirstPossession,
                -eloDiffTime,
            )

        return 1.0 - (
            ((301.0 / 751.0) * probIfSuccess) +
                ((447.0 / 751.0) * probIfFail) +
                ((3.0 / 751.0) * probIfReturn)
        )
    }

    /**
     * Calculate win probability for KICKOFF_NORMAL - matches Python implementation
     */
    private fun calculateKickoffWinProbability(
        margin: Int,
        timeRemaining: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double {
        return 1.0 -
            calculateWinProbabilityForScenario(
                75, -margin, timeRemaining, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
            )
    }

    /**
     * Calculate win probability for KICKOFF_SQUIB - matches Python implementation
     */
    private fun calculateKickoffSquibWinProbability(
        margin: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double {
        val slg = ((2 - half) * 840) + secondsLeftHalf

        return 1.0 -
            calculateWinProbabilityForScenario(
                65, -margin, slg, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
            )
    }

    /**
     * Calculate win probability for KICKOFF_ONSIDE - matches Python implementation
     */
    private fun calculateKickoffOnsideWinProbability(
        margin: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double {
        val slg = ((2 - half) * 840) + secondsLeftHalf

        val probIfSuccess =
            calculateWinProbabilityForScenario(55, margin, slg, secondsLeftHalf, half, hadFirstPossession, eloDiffTime)
        val probIfFail =
            1.0 -
                calculateWinProbabilityForScenario(
                    45, -margin, slg, secondsLeftHalf,
                    half, 1 - hadFirstPossession, -eloDiffTime,
                )
        val slhr = maxOf(secondsLeftHalf - 10, 0)
        val slgr = ((2 - half) * 840) + slhr
        val probIfReturn =
            1.0 -
                calculateWinProbabilityForScenario(
                    75, margin - 6, slgr, slhr,
                    half, 1 - hadFirstPossession, -eloDiffTime,
                )

        return (
            ((140.0 / 751.0) * probIfSuccess) +
                ((611.0 / 751.0) * probIfFail) +
                ((1.0 / 751.0) * probIfReturn)
        )
    }

    /**
     * Calculate win probability for a specific scenario - matches Python implementation
     */
    private fun calculateWinProbabilityForScenario(
        position: Int,
        margin: Int,
        secondsLeftGame: Int,
        secondsLeftHalf: Int,
        half: Int,
        hadFirstPossession: Int,
        eloDiffTime: Double,
    ): Double {
        val features =
            xgboostPredictor.createFeatureArray(
                down = 1,
                distance = 10,
                position = position,
                margin = margin,
                secondsLeftGame = secondsLeftGame,
                secondsLeftHalf = secondsLeftHalf,
                half = half,
                hadFirstPossession = hadFirstPossession,
                eloDiffTime = eloDiffTime,
            )

        val rawWinProbability = xgboostPredictor.predict(features)

        // The model outputs probability for the team with possession
        // Return the raw probability (for whoever has possession)
        return rawWinProbability
    }

    /**
     * Calculate expected score for ELO
     */
    private fun calculateExpectedScore(
        ratingA: Double,
        ratingB: Double,
    ): Double {
        return 1.0 / (1.0 + 10.0.pow((ratingB - ratingA) / 400.0))
    }

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

    /**
     * Get ELO ratings for all teams
     */
    fun getEloRatings(teams: List<Team>): List<EloRatingResponse> =
        try {
            teams.map { team ->
                EloRatingResponse(
                    teamId = team.id,
                    teamName = team.name ?: "",
                    currentElo = team.currentElo,
                    overallElo = team.overallElo,
                )
            }.sortedByDescending { it.currentElo }
        } catch (e: Exception) {
            logger.error("Error getting ELO ratings response: ${e.message}", e)
            throw e
        }

    /**
     * Get team ELO from game stats for a specific game
     */
    private fun getTeamEloFromGameStats(
        gameId: Int,
        teamName: String,
    ): Double? {
        return try {
            val gameStats = gameStatsService.getGameStatsById(gameId)
            val teamGameStats = gameStats.find { it.team == teamName }
            teamGameStats?.teamElo
        } catch (e: Exception) {
            logger.warn("Could not retrieve game stats for game $gameId: ${e.message}")
            null
        }
    }

    /**
     * Calculate win probability for all plays in a specific game
     */
    fun calculateWinProbabilitiesForSingleGame(
        gameId: Int,
        game: Game,
        plays: List<Play>,
        homeTeam: Team,
        awayTeam: Team,
        playService: PlayService,
    ): SingleGameWinProbabilitiesResponse {
        try {
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

                val winProbability = calculateWinProbability(game, play, homeElo, awayElo)

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

                        if (play.possession != prev.possession) {
                            currentWinProb - prevWinProb
                        } else {
                            currentWinProb - prevWinProb
                        }
                    } ?: 0.0

                play.winProbability = winProbability
                play.winProbabilityAdded = winProbabilityAdded

                // Update each play individually
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
        } catch (e: Exception) {
            logger.error("Error calculating win probability for game response: ${e.message}", e)
            throw e
        }
    }

    /**
     * Calculate win probability for ALL games in the database
     */
    fun calculateWinProbabilitiesForAllGames(
        games: List<Game>,
        playService: PlayService,
        teamService: TeamService,
    ): WinProbabilitiesForAllGamesResponse =
        try {
            var totalGamesProcessed = 0
            var totalPlaysProcessed = 0
            val processedGames = mutableListOf<ProcessedGameResult>()

            games.forEach { game ->
                try {
                    val plays = playService.getAllPlaysByGameId(game.gameId)
                    if (plays.isNotEmpty()) {
                        val homeTeam = teamService.getTeamByName(game.homeTeam)
                        val awayTeam = teamService.getTeamByName(game.awayTeam)

                        // Use the single game method to calculate win probabilities
                        val singleGameResult =
                            calculateWinProbabilitiesForSingleGame(
                                game.gameId,
                                game,
                                plays,
                                homeTeam,
                                awayTeam,
                                playService,
                            )

                        // Add to our results
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
                    // Log error but continue with other games
                    logger.error("Error processing game ${game.gameId}: ${e.message}")
                }
            }

            WinProbabilitiesForAllGamesResponse(
                totalGames = games.size,
                gamesProcessed = totalGamesProcessed,
                totalPlaysProcessed = totalPlaysProcessed,
                processedGames = processedGames,
            )
        } catch (e: Exception) {
            logger.error("Error calculating win probability for all games response: ${e.message}", e)
            throw e
        }

    /**
     * Get win probability for each team for all plays in a game
     */
    fun getWinProbabilitiesForGame(
        gameId: Int,
        plays: List<Play>,
    ): GameWinProbabilitiesResponse =
        try {
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

            GameWinProbabilitiesResponse(
                gameId = gameId,
                totalPlays = plays.size,
                plays = results,
            )
        } catch (e: Exception) {
            logger.error("Error getting team win probabilities response: ${e.message}", e)
            throw e
        }
}
