package com.fcfb.arceus.service.fcfb

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
    private val xgboostPredictor: XGBoostPredictor
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
        previousWinProbability: Double? = null
    ): Double {
        try {
            val scoreDiff = play.homeScore - play.awayScore
            val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
            val down = play.down
            val yardsToGo = play.yardsToGo
            val ballLocation = play.ballLocation
            val timeoutDiff = play.homeTimeouts - play.awayTimeouts
            val quarter = play.quarter
            
            // Calculate additional features needed for model
            val secondsLeftGame = timeRemaining
            val secondsLeftHalf = calculateSecondsLeftHalf(play.quarter, play.clock)
            val half = if (play.quarter <= 2) 1 else 2
            val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
            val eloDiffTime = if (play.possession == TeamSide.HOME) {
                calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, secondsLeftGame)
            } else {
                calculateEloDiffTime(awayTeam.currentElo, homeTeam.currentElo, secondsLeftGame)
            }

            // Create feature array for XGBoost model
            // Calculate margin from the possessing team's perspective
            val margin = if (play.possession == TeamSide.HOME) {
                scoreDiff  // HOME possession: positive if HOME ahead
            } else {
                -scoreDiff  // AWAY possession: positive if AWAY ahead
            }
            
            val features = xgboostPredictor.createFeatureArray(
                down = down,
                distance = yardsToGo,
                position = ballLocation,
                margin = margin,
                secondsLeftGame = secondsLeftGame,
                secondsLeftHalf = secondsLeftHalf,
                half = half,
                hadFirstPossession = hadFirstPossession,
                eloDiffTime = eloDiffTime
            )

            // Log feature values for debugging
            logger.info("=== WIN PROBABILITY DEBUG ===")
            logger.info("Play #${play.playNumber}: ${play.possession} possession")
            logger.info("Score: HOME ${play.homeScore} - AWAY ${play.awayScore}")
            logger.info("Coin toss: winner=${game.coinTossWinner}, choice=${game.coinTossChoice}")
            logger.info("ELO: HOME=${homeTeam.currentElo}, AWAY=${awayTeam.currentElo}")
            logger.info("Feature values: scoreDiff=$scoreDiff, margin=$margin, timeRemaining=$timeRemaining, down=$down, yardsToGo=$yardsToGo, ballLocation=$ballLocation, timeoutDiff=$timeoutDiff, quarter=$quarter")
            logger.info("Additional features: secondsLeftGame=$secondsLeftGame, secondsLeftHalf=$secondsLeftHalf, half=$half, hadFirstPossession=$hadFirstPossession, eloDiffTime=$eloDiffTime")
            logger.info("Feature array: ${features.contentToString()}")

            // Handle special play types first (like Python code)
            val winProbability = when (play.playCall) {
                PlayCall.PAT -> calculatePatWinProbability(game, play, homeTeam, awayTeam)
                PlayCall.TWO_POINT -> calculateTwoPointWinProbability(game, play, homeTeam, awayTeam)
                PlayCall.KICKOFF_NORMAL -> calculateKickoffWinProbability(game, play, homeTeam, awayTeam)
                PlayCall.KICKOFF_ONSIDE -> calculateKickoffOnsideWinProbability(game, play, homeTeam, awayTeam)
                PlayCall.KICKOFF_SQUIB -> calculateKickoffSquibWinProbability(game, play, homeTeam, awayTeam)
                else -> {
                    // Regular play - use XGBoost model
                    val rawWinProbability = xgboostPredictor.predict(features)
                    logger.info("Raw XGBoost prediction: $rawWinProbability")
                    rawWinProbability
                }
            }

            // Calculate win probability change
            val previousProbability = previousWinProbability ?: 0.5 // Default to 0.5 for first play
            val winProbabilityChange = winProbability - previousProbability
            
            // Set the win probability and change on the play
            play.winProbability = winProbability
            play.winProbabilityAdded = winProbabilityChange

            logger.info("Win probability calculation (possessing team): scoreDiff=$scoreDiff, timeRemaining=$timeRemaining, down=$down, yardsToGo=$yardsToGo, ballLocation=$ballLocation, timeoutDiff=$timeoutDiff, quarter=$quarter, possession=${play.possession}, winProbability=$winProbability, change=$winProbabilityChange")
            
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
     * Get win probability for a specific team
     * @param game The game object
     * @param play The play object
     * @param homeTeam The home team
     * @param awayTeam The away team
     * @param teamSide Which team to get win probability for
     * @param previousWinProbability Previous win probability for the HOME team
     * @return Win probability for the specified team (0.0 to 1.0)
     */
    fun getWinProbabilityForTeam(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
        teamSide: TeamSide,
        previousWinProbability: Double? = null
    ): Double {
        val homeTeamProbability = calculateWinProbability(game, play, homeTeam, awayTeam, previousWinProbability)
        
        return when (teamSide) {
            TeamSide.HOME -> homeTeamProbability
            TeamSide.AWAY -> 1.0 - homeTeamProbability
        }
    }

    /**
     * Get win probability for the AWAY team (convenience method)
     * @param game The game object
     * @param play The play object
     * @param homeTeam The home team
     * @param awayTeam The away team
     * @param previousWinProbability Previous win probability for the HOME team
     * @return Win probability for the AWAY team (0.0 to 1.0)
     */
    fun getAwayTeamWinProbability(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team,
        previousWinProbability: Double? = null
    ): Double {
        val homeTeamProbability = calculateWinProbability(game, play, homeTeam, awayTeam, previousWinProbability)
        return 1.0 - homeTeamProbability
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
        previousPlay: Play? = null
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
    fun updateEloRatings(game: Game, homeTeam: Team, awayTeam: Team) {
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
    private fun calculateTimeRemaining(quarter: Int, clock: Int): Double {
        // Python: seconds_left_game = 1680-(420-int(clock)) for Q1, etc.
        val timeRemaining = when (quarter) {
            1 -> 1680 - (420 - clock) // 1680 - (420 - clock)
            2 -> 1260 - (420 - clock) // 1260 - (420 - clock) 
            3 -> 840 - (420 - clock)  // 840 - (420 - clock)
            4 -> 420 - (420 - clock)  // 420 - (420 - clock)
            else -> 0.0
        }
        return maxOf(0.0, timeRemaining.toDouble())
    }

    /**
     * Calculate seconds left in half - matches Python implementation
     */
    private fun calculateSecondsLeftHalf(quarter: Int, clock: Int): Int {
        val secondsLeftHalf = when (quarter) {
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
    private fun calculateEloDiffTime(offenseElo: Double, defenseElo: Double, secondsLeftGame: Int): Double {
        return (offenseElo - defenseElo) * Math.exp(-2.0 * (1.0 - (secondsLeftGame.toDouble() / 1680.0)))
    }



    /**
     * Calculate who had first possession based on coin toss
     * Returns 1 if the team with current possession had first possession, 0 otherwise
     */
    private fun calculateHadFirstPossession(game: Game, currentPossession: TeamSide): Int {
        val coinTossWinner = game.coinTossWinner
        val coinTossChoice = game.coinTossChoice
        
        // Check if coin toss data is missing
        if (coinTossWinner == null || coinTossChoice == null) {
            logger.warn("Missing coin toss data for game ${game.gameId}. Using default: HOME had first possession")
            return if (currentPossession == TeamSide.HOME) 1 else 0
        }
        
        // Determine who actually had first possession
        val whoHadFirstPossession = when {
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
        awayTeam: Team
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = calculateSecondsLeftHalf(play.quarter, play.clock)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)
        
        // Calculate probabilities for different PAT outcomes
        val probIfSuccess = calculateWinProbabilityForScenario(1, 10, 75, -(scoreDiff + 1), timeRemaining, secondsLeftHalf, half, hadFirstPossession, -eloDiffTime)
        val probIfFail = calculateWinProbabilityForScenario(1, 10, 75, -scoreDiff, timeRemaining, secondsLeftHalf, half, 1 - hadFirstPossession, -eloDiffTime)
        val probIfReturn = calculateWinProbabilityForScenario(1, 10, 75, -(scoreDiff - 2), timeRemaining, secondsLeftHalf, half, 1 - hadFirstPossession, -eloDiffTime)
        
        return 1.0 - (((721.0 / 751.0) * probIfSuccess) + ((27.0 / 751.0) * probIfFail) + ((3.0 / 751.0) * probIfReturn))
    }

    /**
     * Calculate win probability for TWO_POINT - matches Python implementation
     */
    private fun calculateTwoPointWinProbability(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = calculateSecondsLeftHalf(play.quarter, play.clock)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)
        
        // Calculate probabilities for different TWO_POINT outcomes
        val probIfSuccess = calculateWinProbabilityForScenario(1, 10, 75, -(scoreDiff + 2), timeRemaining, secondsLeftHalf, half, hadFirstPossession, -eloDiffTime)
        val probIfFail = calculateWinProbabilityForScenario(1, 10, 75, -scoreDiff, timeRemaining, secondsLeftHalf, half, 1 - hadFirstPossession, -eloDiffTime)
        val probIfReturn = calculateWinProbabilityForScenario(1, 10, 75, -(scoreDiff - 2), timeRemaining, secondsLeftHalf, half, 1 - hadFirstPossession, -eloDiffTime)
        
        return 1.0 - (((301.0 / 751.0) * probIfSuccess) + ((447.0 / 751.0) * probIfFail) + ((3.0 / 751.0) * probIfReturn))
    }

    /**
     * Calculate win probability for KICKOFF_NORMAL - matches Python implementation
     */
    private fun calculateKickoffWinProbability(
        game: Game, play: Play,
        homeTeam: Team,
        awayTeam: Team
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = calculateSecondsLeftHalf(play.quarter, play.clock)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)
        
        return 1.0 - calculateWinProbabilityForScenario(1, 10, 75, -scoreDiff, timeRemaining, secondsLeftHalf, half, 1 - hadFirstPossession, -eloDiffTime)
    }

    /**
     * Calculate win probability for KICKOFF_SQUIB - matches Python implementation
     */
    private fun calculateKickoffSquibWinProbability(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = maxOf(calculateSecondsLeftHalf(play.quarter, play.clock) - 5, 0)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)
        val slg = ((2 - half) * 840) + secondsLeftHalf
        
        return 1.0 - calculateWinProbabilityForScenario(1, 10, 65, -scoreDiff, slg, secondsLeftHalf, half, 1 - hadFirstPossession, -eloDiffTime)
    }

    /**
     * Calculate win probability for KICKOFF_ONSIDE - matches Python implementation
     */
    private fun calculateKickoffOnsideWinProbability(
        game: Game,
        play: Play,
        homeTeam: Team,
        awayTeam: Team
    ): Double {
        val scoreDiff = play.homeScore - play.awayScore
        val timeRemaining = calculateTimeRemaining(play.quarter, play.clock).toInt()
        val secondsLeftHalf = maxOf(calculateSecondsLeftHalf(play.quarter, play.clock) - 3, 0)
        val half = if (play.quarter <= 2) 1 else 2
        val hadFirstPossession = calculateHadFirstPossession(game, play.possession)
        val eloDiffTime = calculateEloDiffTime(homeTeam.currentElo, awayTeam.currentElo, timeRemaining)
        val slg = ((2 - half) * 840) + secondsLeftHalf
        
        val probIfSuccess = calculateWinProbabilityForScenario(1, 10, 55, scoreDiff, slg, secondsLeftHalf, half, hadFirstPossession, eloDiffTime)
        val probIfFail = 1.0 - calculateWinProbabilityForScenario(1, 10, 45, -scoreDiff, slg, secondsLeftHalf, half, 1 - hadFirstPossession, -eloDiffTime)
        val slhr = maxOf(secondsLeftHalf - 10, 0)
        val slgr = ((2 - half) * 840) + slhr
        val probIfReturn = 1.0 - calculateWinProbabilityForScenario(1, 10, 75, scoreDiff - 6, slgr, slhr, half, 1 - hadFirstPossession, -eloDiffTime)
        
        return ((140.0 / 751.0) * probIfSuccess) + ((611.0 / 751.0) * probIfFail) + ((1.0 / 751.0) * probIfReturn)
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
        eloDiffTime: Double
    ): Double {
        val features = xgboostPredictor.createFeatureArray(
            down = down,
            distance = distance,
            position = position,
            margin = margin,
            secondsLeftGame = secondsLeftGame,
            secondsLeftHalf = secondsLeftHalf,
            half = half,
            hadFirstPossession = hadFirstPossession,
            eloDiffTime = eloDiffTime
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
    fun calculateWinProbabilityAdded(currentPlay: Play, previousPlay: Play?): Double {
        if (previousPlay == null) {
            return 0.0
        }
        
        val currentWinProb = currentPlay.winProbability ?: 0.0
        val previousWinProb = previousPlay.winProbability ?: 0.0
        
        // If possession changed, we need to account for the perspective flip
        if (currentPlay.possession != previousPlay.possession) {
            // When possession changes, the win probability flips perspective
            // We need to convert both to HOME team's perspective to calculate the actual change
            
            val previousHomeWinProb = if (previousPlay.possession == TeamSide.HOME) {
                previousWinProb  // Already HOME perspective
            } else {
                1.0 - previousWinProb  // Convert AWAY perspective to HOME perspective
            }
            
            val currentHomeWinProb = if (currentPlay.possession == TeamSide.HOME) {
                currentWinProb  // Already HOME perspective
            } else {
                1.0 - currentWinProb  // Convert AWAY perspective to HOME perspective
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
    private fun calculateExpectedScore(ratingA: Double, ratingB: Double): Double {
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
    fun getCurrentElo(team: Team): Double {
        initializeEloRatings(team)
        return team.currentElo
    }

    /**
     * Get model information for debugging and monitoring
     */
    fun getModelInfo(): Map<String, Any> {
        return mapOf(
            "model_type" to "XGBoost",
            "num_trees" to xgboostPredictor.modelLoader.getNumTrees(),
            "num_features" to xgboostPredictor.modelLoader.getNumFeatures(),
            "feature_names" to xgboostPredictor.modelLoader.getFeatureNames(),
            "base_score" to xgboostPredictor.modelLoader.getBaseScore(),
            "k_factor" to kFactor,
            "default_elo" to defaultElo
        )
    }

}
