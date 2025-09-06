package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.EloRatingResponse
import com.fcfb.arceus.dto.GameWinProbabilitiesResponse
import com.fcfb.arceus.dto.InitializeEloResponse
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
import com.fcfb.arceus.util.ml.XGBoostPredictor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.pow

@Service
class WinProbabilityService(
    private val xgboostPredictor: XGBoostPredictor,
) {
    private val logger = LoggerFactory.getLogger(WinProbabilityService::class.java)

    // ELO parameters
    private val kFactor = 32.0
    private val defaultElo = 1500.0

    /**
     * Calculate win probability for a given game state
     * @param game The game object
     * @param play The play object (will be updated with win probability)
     * @param homeTeam The home team
     * @param awayTeam The away team
     * @param previousWinProbability Previous win probability for the HOME team
     * @return Win probability for the HOME team (0.0 to 1.0)
     */
    fun calculateWinProbability(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
        previousWinProbability: Double? = null,
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
                    calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, secondsLeftGame)
                } else {
                    calculateEloDiffTime(awayTeam.currentElo, homeTeam.currentElo, secondsLeftGame)
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
                    PlayCall.PAT -> calculatePatWinProbability(game, play, homeTeam, awayTeam)
                    PlayCall.TWO_POINT -> calculateTwoPointWinProbability(game, play, homeTeam, awayTeam)
                    PlayCall.KICKOFF_NORMAL -> calculateKickoffWinProbability(game, play, homeTeam, awayTeam)
                    PlayCall.KICKOFF_ONSIDE -> calculateKickoffOnsideWinProbability(game, play, homeTeam, awayTeam)
                    PlayCall.KICKOFF_SQUIB -> calculateKickoffSquibWinProbability(game, play, homeTeam, awayTeam)
                    else -> {
                        // Regular play - use XGBoost model
                        val rawWinProbability = xgboostPredictor.predict(features)
                        rawWinProbability
                    }
                }

            // Calculate win probability change
            val previousProbability = previousWinProbability ?: 0.5 // Default to 0.5 for first play
            val winProbabilityChange = winProbability - previousProbability

            // Set the win probability and change on the play
            play.winProbability = winProbability
            play.winProbabilityAdded = winProbabilityChange

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
    fun calculateWinProbabilityAdded(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
        previousWinProbability: Double,
        previousPlay: Play? = null,
    ): Double {
        // This method is used by GameService during live play
        if (previousPlay != null) {
            // Use the new method that accounts for possession changes
            return calculateWinProbabilityAdded(play, previousPlay)
        } else {
            // Fallback to simple difference for first play
            val currentWinProbability = calculateWinProbability(game, play, homeTeam, awayTeam)
            return currentWinProbability - previousWinProbability
        }
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
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val margin =
            if (play.possession == TeamSide.HOME) {
                scoreDiff
            } else {
                -scoreDiff
            }
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = calculateSecondsLeftHalf(play.quarter, play.clock)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)

        // Calculate probabilities for different PAT outcomes
        val probIfSuccess =
            calculateWinProbabilityForScenario(
                1, 10, 75, -(margin + 1), timeRemaining, secondsLeftHalf,
                half, hadFirstPossession, -eloDiffTime,
            )
        val probIfFail =
            calculateWinProbabilityForScenario(
                1, 10, 75, -margin, timeRemaining, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
            )
        val probIfReturn =
            calculateWinProbabilityForScenario(
                1, 10, 75, -(margin - 2), timeRemaining, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
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
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val margin =
            if (play.possession == TeamSide.HOME) {
                scoreDiff
            } else {
                -scoreDiff
            }
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = calculateSecondsLeftHalf(play.quarter, play.clock)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)

        // Calculate probabilities for different TWO_POINT outcomes
        val probIfSuccess =
            calculateWinProbabilityForScenario(
                1, 10, 75, -(margin + 2), timeRemaining, secondsLeftHalf,
                half, hadFirstPossession, -eloDiffTime,
            )
        val probIfFail =
            calculateWinProbabilityForScenario(
                1, 10, 75, -margin, timeRemaining, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
            )
        val probIfReturn =
            calculateWinProbabilityForScenario(
                1, 10, 75, -(margin - 2), timeRemaining, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
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
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val margin =
            if (play.possession == TeamSide.HOME) {
                scoreDiff
            } else {
                -scoreDiff
            }
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = calculateSecondsLeftHalf(play.quarter, play.clock)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)

        return 1.0 -
            calculateWinProbabilityForScenario(
                1, 10, 75, -margin, timeRemaining, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
            )
    }

    /**
     * Calculate win probability for KICKOFF_SQUIB - matches Python implementation
     */
    private fun calculateKickoffSquibWinProbability(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val margin =
            if (play.possession == TeamSide.HOME) {
                scoreDiff
            } else {
                -scoreDiff
            }
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = maxOf(calculateSecondsLeftHalf(play.quarter, play.clock) - 5, 0)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)
        val slg = ((2 - half) * 840) + secondsLeftHalf

        return 1.0 -
            calculateWinProbabilityForScenario(
                1, 10, 65, -margin, slg, secondsLeftHalf,
                half, 1 - hadFirstPossession, -eloDiffTime,
            )
    }

    /**
     * Calculate win probability for KICKOFF_ONSIDE - matches Python implementation
     */
    private fun calculateKickoffOnsideWinProbability(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val margin =
            if (play.possession == TeamSide.HOME) {
                scoreDiff
            } else {
                -scoreDiff
            }
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = maxOf(calculateSecondsLeftHalf(play.quarter, play.clock) - 3, 0)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)
        val slg = ((2 - half) * 840) + secondsLeftHalf

        val probIfSuccess =
            calculateWinProbabilityForScenario(1, 10, 55, margin, slg, secondsLeftHalf, half, hadFirstPossession, eloDiffTime)
        val probIfFail =
            1.0 -
                calculateWinProbabilityForScenario(
                    1, 10, 45, -margin, slg, secondsLeftHalf,
                    half, 1 - hadFirstPossession, -eloDiffTime,
                )
        val slhr = maxOf(secondsLeftHalf - 10, 0)
        val slgr = ((2 - half) * 840) + slhr
        val probIfReturn =
            1.0 -
                calculateWinProbabilityForScenario(
                    1, 10, 75, margin - 6, slgr, slhr,
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
        down: Int,
        distance: Int,
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
                down = down,
                distance = distance,
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
     * Calculate win probability added accounting for possession changes
     * When possession changes, we need to account for the flip in perspective
     */
    private fun calculateWinProbabilityAdded(
        currentPlay: Play,
        previousPlay: Play?,
    ): Double {
        if (previousPlay == null) {
            return 0.0
        }

        val currentWinProb = currentPlay.winProbability ?: 0.0
        val previousWinProb = previousPlay.winProbability ?: 0.0

        // If possession changed, we need to account for the perspective flip
        if (currentPlay.possession != previousPlay.possession) {
            // When possession changes, the win probability flips perspective
            // We need to convert both to HOME team's perspective to calculate the actual change

            val previousHomeWinProb =
                if (previousPlay.possession == TeamSide.HOME) {
                    previousWinProb // Already HOME perspective
                } else {
                    1.0 - previousWinProb // Convert AWAY perspective to HOME perspective
                }

            val currentHomeWinProb =
                if (currentPlay.possession == TeamSide.HOME) {
                    currentWinProb // Already HOME perspective
                } else {
                    1.0 - currentWinProb // Convert AWAY perspective to HOME perspective
                }

            return currentHomeWinProb - previousHomeWinProb
        } else {
            // Same possession, so direct difference
            return currentWinProb - previousWinProb
        }
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

    /**
     * Initialize ELO ratings for teams that don't have them
     */
    fun initializeEloRatings(team: Team) {
        if (team.currentElo == 0.0) {
            team.currentElo = defaultElo // Use default ELO from model parameters
            logger.info("Initialized ELO rating for ${team.name}: ${team.currentElo}")
        }
    }

    /**
     * Get current ELO rating for a team
     */
    private fun getCurrentElo(team: Team): Double {
        initializeEloRatings(team)
        return team.currentElo
    }

    fun getWinProbabilityForEachTeam(play: Play): Pair<Double, Double> {
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
                    currentElo = getCurrentElo(team),
                    overallElo = team.overallElo,
                )
            }.sortedByDescending { it.currentElo }
        } catch (e: Exception) {
            logger.error("Error getting ELO ratings response: ${e.message}", e)
            throw e
        }

    /**
     * Initialize ELO ratings for all teams
     */
    fun initializeAllEloRatings(
        teams: List<Team>,
        teamService: TeamService,
    ): InitializeEloResponse =
        try {
            var initializedCount = 0

            teams.forEach { team ->
                initializeEloRatings(team)
                teamService.updateTeam(team)
                initializedCount++
            }

            InitializeEloResponse(
                message = "ELO ratings initialized for $initializedCount teams",
                initializedCount = initializedCount,
            )
        } catch (e: Exception) {
            logger.error("Error initializing ELO ratings: ${e.message}", e)
            throw e
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
    ): SingleGameWinProbabilitiesResponse =
        try {
            // Initialize ELO ratings if needed
            initializeEloRatings(homeTeam)
            initializeEloRatings(awayTeam)

            var processedPlays = 0
            val currentHomeElo = homeTeam.currentElo
            val currentAwayElo = awayTeam.currentElo

            val results =
                plays.sortedBy { it.playNumber }.map { play ->
                    // Calculate win probability for this play
                    val winProbability = calculateWinProbability(game, play, homeTeam, awayTeam)

                    // Update play with calculated values
                    play.winProbability = winProbability
                    play.winProbabilityAdded =
                        if (processedPlays > 0) {
                            val previousPlay = plays.find { it.playNumber == play.playNumber - 1 }
                            calculateWinProbabilityAdded(play, previousPlay)
                        } else {
                            0.0
                        }

                    processedPlays++

                    SinglePlayWinProbabilityResponse(
                        playId = play.playId,
                        playNumber = play.playNumber,
                        quarter = play.quarter,
                        clock = play.clock,
                        homeScore = play.homeScore,
                        awayScore = play.awayScore,
                        winProbability = winProbability,
                        winProbabilityAdded = play.winProbabilityAdded ?: 0.0,
                        possession = play.possession.name,
                        possessionTeam = if (play.possession == TeamSide.HOME) game.homeTeam else game.awayTeam,
                        ballLocation = play.ballLocation,
                        down = play.down,
                        distance = play.yardsToGo,
                        playCall = play.playCall?.name,
                        homeElo = currentHomeElo,
                        awayElo = currentAwayElo,
                    )
                }

            // Save the updated plays
            plays.forEach { playService.updatePlay(it) }

            SingleGameWinProbabilitiesResponse(
                gameId = gameId,
                homeTeam = game.homeTeam,
                awayTeam = game.awayTeam,
                totalPlays = plays.size,
                processedPlays = processedPlays,
                plays = results,
            )
        } catch (e: Exception) {
            logger.error("Error calculating win probability for game response: ${e.message}", e)
            throw e
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
