package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Ranges
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.util.DefensiveNumberNotFound
import com.fcfb.arceus.util.EncryptionUtils
import com.fcfb.arceus.util.InvalidActualResultException
import com.fcfb.arceus.util.InvalidPlayTypeException
import com.fcfb.arceus.util.InvalidScenarioException
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.NumberNotFoundException
import com.fcfb.arceus.util.PlayNotFoundException
import com.fcfb.arceus.util.ResultNotFoundException
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class PlayService(
    private val playRepository: PlayRepository,
    private val encryptionUtils: EncryptionUtils,
    private val gameService: GameService,
    private val gameStatsService: GameStatsService,
    private val rangesService: RangesService,
    private val scorebugService: ScorebugService,
) {
    /**
     * Start a new play, the defensive number was submitted. The defensive number is encrypted
     * @param gameId
     * @param defensiveSubmitter
     * @param defensiveNumber
     * @param timeoutCalled
     * @return
     */
    fun defensiveNumberSubmitted(
        gameId: Int,
        defensiveSubmitter: String,
        defensiveSubmitterId: String,
        defensiveNumber: Int,
        timeoutCalled: Boolean = false,
    ): Play {
        try {
            val game = gameService.getGameById(gameId)
            val responseSpeed =
                if (game.gameStatus != GameStatus.PREGAME) {
                    getResponseSpeed(game)
                } else {
                    null
                }

            val encryptedDefensiveNumber = encryptionUtils.encrypt(defensiveNumber.toString())
            val clock = gameService.convertClockToSeconds(game.clock)
            val gamePlay: Play =
                playRepository.save(
                    Play(
                        gameId = gameId,
                        playNumber = game.numPlays.plus(1),
                        homeScore = game.homeScore,
                        awayScore = game.awayScore,
                        quarter = game.quarter,
                        clock = clock,
                        ballLocation = game.ballLocation,
                        possession = game.possession,
                        down = game.down,
                        yardsToGo = game.yardsToGo,
                        defensiveNumber = encryptedDefensiveNumber,
                        offensiveNumber = "0",
                        offensiveSubmitter = null,
                        offensiveSubmitterId = null,
                        defensiveSubmitter = defensiveSubmitter,
                        defensiveSubmitterId = defensiveSubmitterId,
                        playCall = null,
                        result = null,
                        actualResult = null,
                        yards = 0,
                        playTime = 0,
                        runoffTime = 0,
                        winProbability = game.winProbability,
                        winProbabilityAdded = 0.0,
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        difference = 0,
                        timeoutUsed = timeoutCalled,
                        offensiveTimeoutCalled = false,
                        defensiveTimeoutCalled = timeoutCalled,
                        homeTimeouts = game.homeTimeouts,
                        awayTimeouts = game.awayTimeouts,
                        playFinished = false,
                        offensiveResponseSpeed = null,
                        defensiveResponseSpeed = responseSpeed,
                    ),
                )

            gameService.updateWithDefensiveNumberSubmission(game, gamePlay)
            return gamePlay
        } catch (e: Exception) {
            Logger.error("There was an error submitting the defensive number for game $gameId: " + e.message)
            throw e
        }
    }

    /**
     * The offensive number was submitted, run the play
     * @param gameId
     * @param offensiveSubmitter
     * @param offensiveNumber
     * @param playCall
     * @param runoffType
     * @param offensiveTimeoutCalled
     * @return
     */
    fun offensiveNumberSubmitted(
        gameId: Int,
        offensiveSubmitter: String,
        offensiveSubmitterId: String,
        offensiveNumber: Int?,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensiveTimeoutCalled: Boolean,
    ): Play {
        try {
            val game = gameService.getGameById(gameId)
            var gamePlay = getPlayById(game.currentPlayId!!)
            val responseSpeed = getResponseSpeed(game)

            gamePlay.offensiveResponseSpeed = responseSpeed
            gamePlay.offensiveSubmitter = offensiveSubmitter
            gamePlay.offensiveSubmitterId = offensiveSubmitterId

            val decryptedDefensiveNumber = encryptionUtils.decrypt(gamePlay.defensiveNumber ?: throw DefensiveNumberNotFound())

            when (playCall) {
                PlayCall.PASS, PlayCall.RUN, PlayCall.SPIKE, PlayCall.KNEEL ->
                    gamePlay =
                        runNormalPlay(
                            gamePlay,
                            game,
                            playCall,
                            runoffType,
                            offensiveTimeoutCalled,
                            offensiveNumber,
                            decryptedDefensiveNumber,
                        )

                PlayCall.PAT, PlayCall.TWO_POINT ->
                    gamePlay =
                        runPointAfterPlay(
                            gamePlay,
                            game,
                            playCall,
                            offensiveNumber,
                            decryptedDefensiveNumber,
                        )

                PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_ONSIDE, PlayCall.KICKOFF_SQUIB ->
                    gamePlay =
                        runKickoffPlay(
                            gamePlay,
                            game,
                            playCall,
                            offensiveNumber,
                            decryptedDefensiveNumber,
                        )

                PlayCall.FIELD_GOAL ->
                    gamePlay =
                        runFieldGoalPlay(
                            gamePlay,
                            game,
                            playCall,
                            runoffType,
                            offensiveTimeoutCalled,
                            offensiveNumber,
                            decryptedDefensiveNumber,
                        )

                PlayCall.PUNT ->
                    gamePlay =
                        runPuntPlay(
                            gamePlay,
                            game,
                            playCall,
                            runoffType,
                            offensiveTimeoutCalled,
                            offensiveNumber,
                            decryptedDefensiveNumber,
                        )
            }

            return gamePlay
        } catch (e: Exception) {
            Logger.error("There was an error submitting the offensive number for game $gameId: " + e.message)
            throw e
        }
    }

    /**
     * Rollback the play to the previous play
     * @param gameId
     */
    fun rollbackPlay(gameId: Int): Play {
        try {
            val game = gameService.getGameById(gameId)
            val previousPlay = getPreviousPlay(gameId)
            val gamePlay = getPlayById(game.currentPlayId!!)
            gameService.rollbackPlay(game, previousPlay, gamePlay)
            playRepository.deleteById(gamePlay.playId)
            return previousPlay
        } catch (e: Exception) {
            Logger.error("There was an error rolling back the play for game $gameId: " + e.message)
            throw e
        }
    }

    /**
     * Get the response speed
     * @param game
     */
    private fun getResponseSpeed(game: Game): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val parsedDateTime = game.lastMessageTimestamp?.let { LocalDateTime.parse(it, formatter) }
        val currentDateTime = LocalDateTime.now()
        return Duration.between(parsedDateTime, currentDateTime).seconds
    }

    /**
     * Get a play by its id
     * @param playId
     * @return
     */
    fun getPlayById(playId: Int) =
        playRepository.getPlayById(playId)
            ?: throw PlayNotFoundException("Play with id $playId not found")

    /**
     * Get the previous play of a game
     * @param gameId
     * @return
     */
    fun getPreviousPlay(gameId: Int) =
        playRepository.getPreviousPlay(gameId)
            ?: throw PlayNotFoundException("No previous play found for game $gameId")

    /**
     * Get the current play of a game
     * @param gameId
     */
    fun getCurrentPlay(gameId: Int) =
        playRepository.getCurrentPlay(gameId)
            ?: throw PlayNotFoundException("No current play found for game $gameId")

    /**
     * Get the current play of a game or null
     * @param gameId
     */
    fun getCurrentPlayOrNull(gameId: Int) = playRepository.getCurrentPlay(gameId)

    /**
     * Get all plays for a game
     * @param gameId
     */
    fun getAllPlaysByGameId(gameId: Int) =
        playRepository.getAllPlaysByGameId(gameId).ifEmpty {
            throw PlayNotFoundException("No plays found for game $gameId")
        }

    /**
     * Get all plays with a user
     * @param discordTag
     */
    fun getAllPlaysByDiscordTag(discordTag: String) =
        playRepository.getAllPlaysByDiscordTag(discordTag).ifEmpty {
            throw PlayNotFoundException("No plays found for user $discordTag")
        }

    /**
     * Update a play
     * @param play
     */
    fun updatePlay(play: Play): Play {
        val existingPlay = getPlayById(play.playId)

        existingPlay.apply {
            this.playId = play.playId
            this.gameId = play.gameId
            this.playNumber = play.playNumber
            this.homeScore = play.homeScore
            this.awayScore = play.awayScore
            this.quarter = play.quarter
            this.clock = play.clock
            this.ballLocation = play.ballLocation
            this.possession = play.possession
            this.down = play.down
            this.yardsToGo = play.yardsToGo
            this.defensiveNumber = play.defensiveNumber
            this.offensiveNumber = play.offensiveNumber
            this.defensiveSubmitter = play.defensiveSubmitter
            this.defensiveSubmitterId = play.defensiveSubmitterId
            this.offensiveSubmitter = play.offensiveSubmitter
            this.offensiveSubmitterId = play.offensiveSubmitterId
            this.playCall = play.playCall
            this.result = play.result
            this.difference = play.difference
            this.actualResult = play.actualResult
            this.yards = play.yards
            this.playTime = play.playTime
            this.runoffTime = play.runoffTime
            this.winProbability = play.winProbability
            this.winProbabilityAdded = play.winProbabilityAdded
            this.homeTeam = play.homeTeam
            this.awayTeam = play.awayTeam
            this.timeoutUsed = play.timeoutUsed
            this.offensiveTimeoutCalled = play.offensiveTimeoutCalled
            this.defensiveTimeoutCalled = play.defensiveTimeoutCalled
            this.homeTimeouts = play.homeTimeouts
            this.awayTimeouts = play.awayTimeouts
            this.playFinished = play.playFinished
            this.offensiveResponseSpeed = play.offensiveResponseSpeed
            this.defensiveResponseSpeed = play.defensiveResponseSpeed
        }
        playRepository.save(existingPlay)
        return existingPlay
    }

    /**
     * Get the number of delay of game instances for a home team
     * @param gameId
     */
    fun getHomeDelayOfGameInstances(gameId: Int) =
        playRepository.getHomeDelayOfGameInstances(gameId)
            ?: throw PlayNotFoundException("No delay of game instances found for game $gameId")

    /**
     * Get the number of delay of game instances for an away team
     * @param gameId
     */
    fun getAwayDelayOfGameInstances(gameId: Int) =
        playRepository.getAwayDelayOfGameInstances(gameId)
            ?: throw PlayNotFoundException("No delay of game instances found for game $gameId")

    /**
     * Get the average response time for a user
     * @param discordTag
     * @param season
     */
    fun getUserAverageResponseTime(
        discordTag: String,
        season: Int,
    ) = playRepository.getUserAverageResponseTime(discordTag, season)
        ?: throw Exception("Could not get average response time for user $discordTag")

    /**
     * Runs the play, returns the updated gamePlay
     * @param gamePlay
     * @param game
     * @param playCall
     * @param offensiveNumber
     * @param decryptedDefensiveNumber
     * @return
     */
    private fun runNormalPlay(
        gamePlay: Play,
        game: Game,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensiveTimeoutCalled: Boolean,
        offensiveNumber: Int?,
        decryptedDefensiveNumber: String,
    ): Play {
        if (game.currentPlayType != PlayType.NORMAL) {
            throw InvalidPlayTypeException()
        }

        val difference =
            if (offensiveNumber != null) {
                gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
            } else {
                null
            }
        var possession = gamePlay.possession
        val (offensivePlaybook, defensivePlaybook) = getPlaybooks(game, possession)
        val (timeoutUsed, homeTimeoutCalled, awayTimeoutCalled) = getTimeoutUsage(game, gamePlay, offensiveTimeoutCalled)

        val resultInformation =
            if (difference != null) {
                rangesService.getNormalResult(playCall, offensivePlaybook, defensivePlaybook, difference)
            } else {
                when (playCall) {
                    PlayCall.SPIKE -> {
                        Ranges(
                            PlayType.NORMAL.description,
                            offensivePlaybook.description,
                            defensivePlaybook.description,
                            0,
                            0,
                            0,
                            Scenario.SPIKE,
                            0,
                            0,
                            0,
                        )
                    }

                    PlayCall.KNEEL -> {
                        Ranges(
                            PlayType.NORMAL.description,
                            offensivePlaybook.description,
                            defensivePlaybook.description,
                            0,
                            0,
                            0,
                            Scenario.KNEEL,
                            1,
                            0,
                            0,
                        )
                    }

                    else -> throw NumberNotFoundException()
                }
            }
        var result = resultInformation.result ?: throw ResultNotFoundException()
        var playTime = resultInformation.playTime

        // Determine runoff time between plays
        val clockStopped = game.clockStopped
        val runoffTime = getRunoffTime(game, clockStopped, gamePlay.clock, timeoutUsed, playCall, runoffType, offensivePlaybook)
        if (gamePlay.clock - runoffTime < 0 && (game.quarter == 2 || game.quarter == 4)) {
            result = Scenario.END_OF_HALF
        }

        var homeScore = game.homeScore
        var awayScore = game.awayScore
        var ballLocation = game.ballLocation
        var down = game.down
        var yardsToGo = game.yardsToGo
        var yards = 0
        var actualResult: ActualResult
        when (result) {
            Scenario.TURNOVER_TOUCHDOWN -> actualResult = ActualResult.TURNOVER_TOUCHDOWN
            Scenario.TURNOVER_PLUS_20_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation + 20
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TURNOVER_TOUCHDOWN
                }
            }
            Scenario.TURNOVER_PLUS_15_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation + 15
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TURNOVER_TOUCHDOWN
                }
            }
            Scenario.TURNOVER_PLUS_10_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation + 10
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TURNOVER_TOUCHDOWN
                }
            }
            Scenario.TURNOVER_PLUS_5_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation + 5
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TURNOVER_TOUCHDOWN
                }
            }
            Scenario.TURNOVER -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation
            }
            Scenario.TURNOVER_MINUS_5_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation - 5
                if (ballLocation <= 0) {
                    ballLocation = 20
                }
            }
            Scenario.TURNOVER_MINUS_10_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation - 10
                if (ballLocation <= 0) {
                    ballLocation = 20
                }
            }
            Scenario.TURNOVER_MINUS_15_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation - 15
                if (ballLocation <= 0) {
                    ballLocation = 20
                }
            }
            Scenario.TURNOVER_MINUS_20_YARDS -> {
                actualResult = ActualResult.TURNOVER
                ballLocation = 100 - ballLocation - 20
                if (ballLocation <= 0) {
                    ballLocation = 20
                }
            }
            Scenario.NO_GAIN, Scenario.INCOMPLETE -> {
                actualResult = ActualResult.NO_GAIN
                down += 1
                if (down > 4) {
                    actualResult = ActualResult.TURNOVER_ON_DOWNS
                    ballLocation = 100 - ballLocation
                }
            }
            Scenario.TOUCHDOWN -> {
                yards = 100 - ballLocation
                actualResult = ActualResult.TOUCHDOWN
                playTime = rangesService.getPlayTime(playCall, yards)
            }
            Scenario.SPIKE -> {
                actualResult = ActualResult.SPIKE
                down += 1
                if (down > 4) {
                    actualResult = ActualResult.TURNOVER_ON_DOWNS
                    ballLocation = 100 - ballLocation
                }
            }
            Scenario.KNEEL -> {
                yards = -2
                actualResult = ActualResult.KNEEL
                ballLocation -= 2
                if (ballLocation <= 0) {
                    ballLocation = 1
                }
                yardsToGo -= yards
                down += 1
                if (down > 4) {
                    actualResult = ActualResult.TURNOVER_ON_DOWNS
                    ballLocation = 100 - ballLocation
                }
            }
            Scenario.END_OF_HALF -> {
                actualResult = ActualResult.END_OF_HALF
            }
            else -> {
                yards = result.description.toInt()
                val originalBallLocation = ballLocation
                ballLocation += yards
                if (ballLocation >= 100) {
                    actualResult = ActualResult.TOUCHDOWN
                    yards = 100 - originalBallLocation
                    playTime = rangesService.getPlayTime(playCall, yards)
                } else if (ballLocation <= 0) {
                    actualResult = ActualResult.SAFETY
                    yards = 0 - originalBallLocation
                } else if (yards >= yardsToGo) {
                    down = 1
                    yardsToGo = 10
                    actualResult = ActualResult.FIRST_DOWN
                } else {
                    down += 1
                    if (down > 4) {
                        actualResult = ActualResult.TURNOVER_ON_DOWNS
                        ballLocation = 100 - ballLocation
                    } else {
                        yardsToGo -= yards
                        actualResult =
                            if (yards > 0) {
                                ActualResult.GAIN
                            } else if (yards == 0) {
                                ActualResult.NO_GAIN
                            } else {
                                ActualResult.LOSS
                            }
                    }
                }
            }
        }
        when (actualResult) {
            ActualResult.TURNOVER_ON_DOWNS, ActualResult.TURNOVER -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
                down = 1
                yardsToGo = 10
            }
            ActualResult.SAFETY -> {
                ballLocation = 20
                if (possession == TeamSide.HOME) {
                    awayScore += 2
                } else {
                    homeScore += 2
                }
            }
            ActualResult.TOUCHDOWN -> {
                ballLocation = 97
                if (possession == TeamSide.HOME) {
                    homeScore += 6
                } else {
                    awayScore += 6
                }
            }
            ActualResult.TURNOVER_TOUCHDOWN -> {
                ballLocation = 97
                if (possession == TeamSide.HOME) {
                    awayScore += 6
                    possession = TeamSide.AWAY
                } else {
                    homeScore += 6
                    possession = TeamSide.HOME
                }
            }
            ActualResult.END_OF_HALF -> {}
            ActualResult.FIRST_DOWN -> {}
            ActualResult.GAIN -> {}
            ActualResult.NO_GAIN -> {}
            ActualResult.SPIKE -> {}
            ActualResult.KNEEL -> {}
            ActualResult.LOSS -> {}
            else -> throw InvalidActualResultException()
        }

        return updatePlayValues(
            game,
            gamePlay,
            playCall,
            result,
            actualResult,
            possession,
            homeScore,
            awayScore,
            runoffTime,
            playTime,
            ballLocation,
            down,
            yardsToGo,
            decryptedDefensiveNumber,
            offensiveNumber,
            difference,
            yards,
            timeoutUsed,
            homeTimeoutCalled,
            awayTimeoutCalled,
        )
    }

    /**
     * Runs the field goal play, returns the updated gamePlay
     *
     */
    private fun runFieldGoalPlay(
        gamePlay: Play,
        game: Game,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensiveTimeoutCalled: Boolean,
        offensiveNumber: Int?,
        decryptedDefensiveNumber: String,
    ): Play {
        if (game.currentPlayType != PlayType.NORMAL) {
            throw InvalidPlayTypeException()
        }

        if (offensiveNumber == null) {
            throw NumberNotFoundException()
        }

        val difference = gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
        var possession = gamePlay.possession
        var ballLocation = 100 - game.ballLocation
        val (offensivePlaybook, _) = getPlaybooks(game, possession)
        val (timeoutUsed, homeTimeoutCalled, awayTimeoutCalled) = getTimeoutUsage(game, gamePlay, offensiveTimeoutCalled)

        val resultInformation = rangesService.getFieldGoalResult(playCall, ballLocation + 17, difference)
        var result = resultInformation.result ?: throw ResultNotFoundException()
        val playTime = resultInformation.playTime
        ballLocation = game.ballLocation

        // Determine runoff time between plays
        val clockStopped = game.clockStopped
        val runoffTime = getRunoffTime(game, clockStopped, gamePlay.clock, timeoutUsed, playCall, runoffType, offensivePlaybook)
        if (gamePlay.clock - runoffTime < 0 && (game.quarter == 2 || game.quarter == 4)) {
            result = Scenario.END_OF_HALF
        }

        var homeScore = game.homeScore
        var awayScore = game.awayScore
        val down = 1
        val yardsToGo = 10
        val actualResult: ActualResult
        when (result) {
            Scenario.KICK_SIX -> {
                actualResult = ActualResult.KICK_SIX
                ballLocation = 97
            }
            Scenario.BLOCKED_FIELD_GOAL -> {
                actualResult = ActualResult.BLOCKED
                ballLocation = 100 - ballLocation
            }
            Scenario.NO_GOOD -> {
                actualResult = ActualResult.NO_GOOD
                ballLocation = 100 - ballLocation
            }
            Scenario.GOOD -> {
                actualResult = ActualResult.GOOD
                ballLocation = 35
            }
            Scenario.END_OF_HALF -> {
                actualResult = ActualResult.END_OF_HALF
            }
            else -> throw InvalidScenarioException()
        }
        when (actualResult) {
            ActualResult.KICK_SIX -> {
                if (possession == TeamSide.HOME) {
                    possession = TeamSide.AWAY
                    awayScore += 6
                } else {
                    possession = TeamSide.HOME
                    homeScore += 6
                }
            }
            ActualResult.BLOCKED -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
            }
            ActualResult.NO_GOOD -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
            }
            ActualResult.GOOD -> {
                if (possession == TeamSide.HOME) {
                    homeScore += 3
                } else {
                    awayScore += 3
                }
            }
            ActualResult.END_OF_HALF -> {}
            else -> throw InvalidActualResultException()
        }

        return updatePlayValues(
            game,
            gamePlay,
            playCall,
            result,
            actualResult,
            possession,
            homeScore,
            awayScore,
            runoffTime,
            playTime,
            ballLocation,
            down,
            yardsToGo,
            decryptedDefensiveNumber,
            offensiveNumber,
            difference,
            0,
            timeoutUsed,
            homeTimeoutCalled,
            awayTimeoutCalled,
        )
    }

    /**
     * Runs the punt play, returns the updated gamePlay
     * @param gamePlay
     * @param game
     * @param playCall
     * @param offensiveNumber
     * @param decryptedDefensiveNumber
     */
    private fun runPuntPlay(
        gamePlay: Play,
        game: Game,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensiveTimeoutCalled: Boolean,
        offensiveNumber: Int?,
        decryptedDefensiveNumber: String,
    ): Play {
        if (game.currentPlayType != PlayType.NORMAL) {
            throw InvalidPlayTypeException()
        }

        if (offensiveNumber == null) {
            throw NumberNotFoundException()
        }

        val difference = gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
        var possession = gamePlay.possession
        var ballLocation = game.ballLocation
        val (offensivePlaybook, _) = getPlaybooks(game, possession)
        val (timeoutUsed, homeTimeoutCalled, awayTimeoutCalled) = getTimeoutUsage(game, gamePlay, offensiveTimeoutCalled)

        val resultInformation = rangesService.getPuntResult(playCall, ballLocation, difference)
        var result = resultInformation.result ?: throw ResultNotFoundException()
        val playTime = resultInformation.playTime

        // Determine runoff time between plays
        val clockStopped = game.clockStopped
        val runoffTime = getRunoffTime(game, clockStopped, gamePlay.clock, timeoutUsed, playCall, runoffType, offensivePlaybook)
        if (gamePlay.clock - runoffTime < 0 && (game.quarter == 2 || game.quarter == 4)) {
            result = Scenario.END_OF_HALF
        }

        var homeScore = game.homeScore
        var awayScore = game.awayScore
        var down = game.down
        var yardsToGo = game.yardsToGo
        val actualResult: ActualResult
        when (result) {
            Scenario.PUNT_RETURN_TOUCHDOWN -> {
                actualResult = ActualResult.PUNT_RETURN_TOUCHDOWN
                ballLocation = 97
            }
            Scenario.BLOCKED_PUNT -> {
                actualResult = ActualResult.BLOCKED
                ballLocation = 100 - ballLocation
            }
            Scenario.TOUCHBACK -> {
                actualResult = ActualResult.PUNT
                ballLocation = 20
            }
            Scenario.FIVE_YARD_PUNT, Scenario.TEN_YARD_PUNT, Scenario.FIFTEEN_YARD_PUNT, Scenario.TWENTY_YARD_PUNT,
            Scenario.TWENTY_FIVE_YARD_PUNT, Scenario.THIRTY_YARD_PUNT, Scenario.THIRTY_FIVE_YARD_PUNT,
            Scenario.FORTY_YARD_PUNT, Scenario.FORTY_FIVE_YARD_PUNT, Scenario.FIFTY_YARD_PUNT, Scenario.FIFTY_FIVE_YARD_PUNT,
            Scenario.SIXTY_YARD_PUNT, Scenario.SIXTY_FIVE_YARD_PUNT, Scenario.SEVENTY_YARD_PUNT,
            -> {
                actualResult = ActualResult.PUNT
                ballLocation = 100 - (ballLocation + result.description.substringBefore(" YARD PUNT").toInt())
            }
            Scenario.FUMBLE -> {
                actualResult = ActualResult.MUFFED_PUNT
                ballLocation += 40
                if (ballLocation >= 100) {
                    ballLocation = 99
                }
            }
            Scenario.TOUCHDOWN -> {
                actualResult = ActualResult.PUNT_TEAM_TOUCHDOWN
                ballLocation = 97
            }
            Scenario.END_OF_HALF -> {
                actualResult = ActualResult.END_OF_HALF
            }
            else -> throw InvalidScenarioException()
        }
        when (actualResult) {
            ActualResult.PUNT_RETURN_TOUCHDOWN -> {
                if (possession == TeamSide.HOME) {
                    possession = TeamSide.AWAY
                    awayScore += 6
                } else {
                    possession = TeamSide.HOME
                    homeScore += 6
                }
                down = 1
                yardsToGo = 10
            }
            ActualResult.BLOCKED -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
                down = 1
                yardsToGo = 10
            }
            ActualResult.PUNT -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
                down = 1
                yardsToGo = 10
            }
            ActualResult.MUFFED_PUNT -> {
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.HOME
                    } else {
                        TeamSide.AWAY
                    }
                down = 1
                yardsToGo = 10
            }
            ActualResult.PUNT_TEAM_TOUCHDOWN -> {
                if (possession == TeamSide.HOME) {
                    homeScore += 6
                    possession = TeamSide.HOME
                } else {
                    awayScore += 6
                    possession = TeamSide.AWAY
                }
            }
            ActualResult.END_OF_HALF -> {}
            else -> throw InvalidActualResultException()
        }

        return updatePlayValues(
            game,
            gamePlay,
            playCall,
            result,
            actualResult,
            possession,
            homeScore,
            awayScore,
            runoffTime,
            playTime,
            ballLocation,
            down,
            yardsToGo,
            decryptedDefensiveNumber,
            offensiveNumber,
            difference,
            0,
            timeoutUsed,
            homeTimeoutCalled,
            awayTimeoutCalled,
        )
    }

    /**
     * Runs the kickoff play, returns the updated gamePlay
     * @param gamePlay
     * @param game
     * @param playCall
     * @param offensiveNumber
     * @param decryptedDefensiveNumber
     */
    private fun runKickoffPlay(
        gamePlay: Play,
        game: Game,
        playCall: PlayCall,
        offensiveNumber: Int?,
        decryptedDefensiveNumber: String,
    ): Play {
        if (game.currentPlayType != PlayType.KICKOFF) {
            throw InvalidPlayTypeException()
        }

        if (offensiveNumber == null) {
            throw NumberNotFoundException()
        }

        val difference = gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
        var possession = gamePlay.possession
        val resultInformation = rangesService.getNonNormalResult(playCall, difference)
        val result = resultInformation.result ?: throw ResultNotFoundException()
        var homeScore = game.homeScore
        var awayScore = game.awayScore
        val ballLocation: Int
        val down = 1
        val yardsToGo = 10
        val playTime = resultInformation.playTime
        val actualResult: ActualResult?
        when (result) {
            Scenario.TOUCHDOWN -> {
                actualResult = ActualResult.KICKING_TEAM_TOUCHDOWN
                ballLocation = 97
            }
            Scenario.FUMBLE -> {
                actualResult = ActualResult.MUFFED_KICK
                ballLocation = 75
            }
            Scenario.FIVE_YARD_RETURN, Scenario.TEN_YARD_RETURN, Scenario.TWENTY_YARD_RETURN, Scenario.THIRTY_YARD_RETURN,
            Scenario.THIRTY_FIVE_YARD_RETURN, Scenario.FORTY_YARD_RETURN, Scenario.FORTY_FIVE_YARD_RETURN,
            Scenario.FIFTY_YARD_RETURN, Scenario.SIXTY_FIVE_YARD_RETURN,
            -> {
                actualResult = ActualResult.KICKOFF
                ballLocation = result.description.substringBefore(" YARD RETURN").toInt()
            }
            Scenario.TOUCHBACK -> {
                actualResult = ActualResult.KICKOFF
                ballLocation = 25
            }
            Scenario.RETURN_TOUCHDOWN -> {
                actualResult = ActualResult.RETURN_TOUCHDOWN
                ballLocation = 97
            }
            Scenario.RECOVERED -> {
                actualResult = ActualResult.SUCCESSFUL_ONSIDE
                ballLocation = 45
            }
            Scenario.FAILED_ONSIDE -> {
                actualResult = ActualResult.FAILED_ONSIDE
                ballLocation = 55
            }
            else -> throw InvalidScenarioException()
        }
        when (actualResult) {
            ActualResult.KICKING_TEAM_TOUCHDOWN ->
                if (possession == TeamSide.HOME) {
                    homeScore += 6
                } else {
                    awayScore += 6
                }
            ActualResult.KICKOFF, ActualResult.FAILED_ONSIDE ->
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.AWAY
                    } else {
                        TeamSide.HOME
                    }
            ActualResult.RETURN_TOUCHDOWN ->
                if (possession == TeamSide.HOME) {
                    possession = TeamSide.AWAY
                    awayScore += 6
                } else {
                    possession = TeamSide.HOME
                    homeScore += 6
                }
            ActualResult.SUCCESSFUL_ONSIDE, ActualResult.MUFFED_KICK ->
                possession =
                    if (possession == TeamSide.HOME) {
                        TeamSide.HOME
                    } else {
                        TeamSide.AWAY
                    }
            else -> throw InvalidActualResultException()
        }

        return updatePlayValues(
            game,
            gamePlay,
            playCall,
            result,
            actualResult,
            possession,
            homeScore,
            awayScore,
            0,
            playTime,
            ballLocation,
            down,
            yardsToGo,
            decryptedDefensiveNumber,
            offensiveNumber,
            difference,
            0,
            false,
            false,
            false,
        )
    }

    /**
     * Runs the point after play, returns the updated gamePlay
     * @param gamePlay
     * @param game
     * @param playCall
     * @param offensiveNumber
     * @param decryptedDefensiveNumber
     */
    private fun runPointAfterPlay(
        gamePlay: Play,
        game: Game,
        playCall: PlayCall,
        offensiveNumber: Int?,
        decryptedDefensiveNumber: String,
    ): Play {
        if (game.currentPlayType != PlayType.PAT) {
            throw InvalidPlayTypeException()
        }

        if (offensiveNumber == null) {
            throw NumberNotFoundException()
        }

        val difference = gameService.getDifference(offensiveNumber, decryptedDefensiveNumber.toInt())
        val possession = gamePlay.possession
        val resultInformation = rangesService.getNonNormalResult(playCall, difference)
        val result = resultInformation.result ?: throw ResultNotFoundException()
        var homeScore = game.homeScore
        var awayScore = game.awayScore
        val ballLocation = 35
        val down = 1
        val yardsToGo = 10
        val actualResult =
            when (result) {
                Scenario.GOOD -> ActualResult.GOOD
                Scenario.NO_GOOD -> ActualResult.NO_GOOD
                Scenario.SUCCESS -> ActualResult.SUCCESS
                Scenario.FAILED -> ActualResult.FAILED
                Scenario.DEFENSE_TWO_POINT -> ActualResult.DEFENSE_TWO_POINT
                else -> throw InvalidScenarioException()
            }
        when (actualResult) {
            ActualResult.GOOD -> {
                if (possession == TeamSide.HOME) {
                    homeScore += 1
                } else {
                    awayScore += 1
                }
            }
            ActualResult.NO_GOOD -> {}
            ActualResult.SUCCESS ->
                if (possession == TeamSide.HOME) {
                    homeScore += 2
                } else {
                    awayScore += 2
                }
            ActualResult.FAILED -> {}
            ActualResult.DEFENSE_TWO_POINT -> {
                if (possession == TeamSide.HOME) {
                    awayScore += 2
                } else {
                    homeScore += 2
                }
            }
            else -> {
                throw InvalidActualResultException()
            }
        }

        return updatePlayValues(
            game,
            gamePlay,
            playCall,
            result,
            actualResult,
            possession,
            homeScore,
            awayScore,
            0,
            0,
            ballLocation,
            down,
            yardsToGo,
            decryptedDefensiveNumber,
            offensiveNumber,
            difference,
            0,
            false,
            false,
            false,
        )
    }

    /**
     * Determine the runoff time between plays
     * @param clockStopped
     * @param clock
     * @param timeoutUsed
     * @param playCall
     * @param runoffType
     * @param offensivePlaybook
     */
    private fun getRunoffTime(
        game: Game,
        clockStopped: Boolean?,
        clock: Int,
        timeoutUsed: Boolean,
        playCall: PlayCall,
        runoffType: RunoffType,
        offensivePlaybook: OffensivePlaybook,
    ): Int {
        return if (!clockStopped!! && !timeoutUsed) {
            when {
                playCall == PlayCall.SPIKE -> 3
                playCall == PlayCall.KNEEL -> 40
                runoffType == RunoffType.HURRY -> 7
                runoffType == RunoffType.FINAL ->
                    if (clock <= 7) {
                        clock
                    } else if (clock > 30) {
                        30
                    } else {
                        clock - 1
                    }
                runoffType == RunoffType.NORMAL ->
                    when (offensivePlaybook) {
                        OffensivePlaybook.PRO -> 15
                        OffensivePlaybook.AIR_RAID -> 10
                        OffensivePlaybook.FLEXBONE -> 20
                        OffensivePlaybook.SPREAD -> 13
                        OffensivePlaybook.WEST_COAST -> 17
                    }
                runoffType == RunoffType.CHEW -> 30
                game.gameMode == GameMode.CHEW -> 30
                offensivePlaybook == OffensivePlaybook.PRO -> 15
                offensivePlaybook == OffensivePlaybook.AIR_RAID -> 10
                offensivePlaybook == OffensivePlaybook.FLEXBONE -> 20
                offensivePlaybook == OffensivePlaybook.SPREAD -> 13
                offensivePlaybook == OffensivePlaybook.WEST_COAST -> 17
                else -> 0
            }
        } else {
            when {
                playCall == PlayCall.SPIKE -> 1
                else -> 0
            }
        }
    }

    /**
     * Get the offensive and defensive playbooks
     * @param game
     * @param possession
     */
    private fun getPlaybooks(
        game: Game,
        possession: TeamSide,
    ): Pair<OffensivePlaybook, DefensivePlaybook> {
        return if (possession == TeamSide.HOME) {
            game.homeOffensivePlaybook to game.awayDefensivePlaybook
        } else {
            game.awayOffensivePlaybook to game.homeDefensivePlaybook
        }
    }

    /**
     * Handle the normal end of quarter scenarios
     * @param game
     * @param gamePlay
     * @param actualResult
     * @param initialPossession
     * @param initialClock
     * @param initialQuarter
     * @param homeScore
     * @param awayScore
     * @param playTime
     */
    private fun handleEndOfQuarterNormalScenarios(
        game: Game,
        gamePlay: Play,
        actualResult: ActualResult,
        initialPossession: TeamSide,
        initialClock: Int,
        initialQuarter: Int,
        homeScore: Int,
        awayScore: Int,
        playTime: Int,
    ): Triple<TeamSide, Int, Int> {
        var possession = initialPossession
        var clock = initialClock
        var quarter = initialQuarter

        // If quarter is over but game is not over
        if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && quarter < 4) {
            quarter += 1
            clock = 420 - playTime
            if (quarter == 3) {
                possession = gameService.handleHalfTimePossessionChange(game)
                clock = 420
            }
        } else if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && gamePlay.quarter == 4) {
            // Check if game is over or needs to go to OT
            quarter =
                if (homeScore > awayScore || awayScore > homeScore) {
                    0
                } else {
                    5
                }
            clock = 0
        } else if ((clock - playTime) <= 0 && gameService.isTouchdownPlay(actualResult)) {
            clock = 0
            if (quarter == 4 &&
                ((homeScore - awayScore) >= 2 || (awayScore - homeScore) >= 2)
            ) {
                quarter = 0
            }
        } else if (clock > 0) {
            clock = initialClock - playTime
            if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && quarter < 4) {
                quarter += 1
                clock = 420
                if (quarter == 3) {
                    possession = gameService.handleHalfTimePossessionChange(game)
                }
            } else if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && gamePlay.quarter == 4) {
                // Check if game is over or needs to go to OT
                quarter =
                    if (homeScore > awayScore || awayScore > homeScore) {
                        0
                    } else {
                        5
                    }
                clock = 0
            } else if (clock <= 0 && gameService.isTouchdownPlay(actualResult)) {
                clock = 0
                if (quarter == 4 &&
                    ((homeScore - awayScore) >= 2 || (awayScore - homeScore) >= 2)
                ) {
                    quarter = 0
                }
            }
        }
        return Triple(possession, clock, quarter)
    }

    /**
     * Handle the end of quarter PAT scenarios
     * @param game
     * @param gamePlay
     * @param initialPossession
     * @param initialClock
     * @param initialQuarter
     * @param homeScore
     * @param awayScore
     */
    private fun handleEndOfQuarterPATScenarios(
        game: Game,
        gamePlay: Play,
        initialPossession: TeamSide,
        initialClock: Int,
        initialQuarter: Int,
        homeScore: Int,
        awayScore: Int,
    ): Triple<TeamSide, Int, Int> {
        var possession = initialPossession
        var clock = initialClock
        var quarter = initialQuarter

        // If quarter is over but game is not over
        if (clock <= 0 &&
            quarter < 4
        ) {
            quarter += 1
            clock = 420
            if (quarter == 3) {
                possession = gameService.handleHalfTimePossessionChange(game)
            }
        } else if (
            clock <= 0 &&
            gamePlay.quarter == 4
        ) {
            // Check if game is over or needs to go to OT
            quarter =
                if (homeScore > awayScore || awayScore > homeScore) {
                    0
                } else {
                    5
                }
            clock = 0
        }
        return Triple(possession, clock, quarter)
    }

    /**
     * Get the timeout usage for the play
     * @param game
     * @param gamePlay
     * @param offensiveTimeoutCalled
     */
    private fun getTimeoutUsage(
        game: Game,
        gamePlay: Play,
        offensiveTimeoutCalled: Boolean,
    ): Triple<Boolean, Boolean, Boolean> {
        val clockStopped = game.clockStopped
        val defensiveTimeoutCalled = gamePlay.timeoutUsed
        var timeoutUsed = false
        var homeTimeoutCalled = false
        var awayTimeoutCalled = false
        if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME && game.awayTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY && game.homeTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME && game.homeTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY && game.awayTimeouts > 0 && !clockStopped) {
            timeoutUsed = true
            awayTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME) {
            homeTimeoutCalled = true
        } else if (offensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY) {
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.HOME) {
            awayTimeoutCalled = true
        } else if (defensiveTimeoutCalled && gamePlay.possession == TeamSide.AWAY) {
            homeTimeoutCalled = true
        }
        return Triple(timeoutUsed, homeTimeoutCalled, awayTimeoutCalled)
    }

    /**
     * Update the play values
     * @param game
     * @param gamePlay
     * @param playCall
     * @param result
     * @param actualResult
     * @param initialPossession
     * @param homeScore
     * @param awayScore
     * @param runoffTime
     * @param playTime
     * @param ballLocation
     * @param down
     * @param yardsToGo
     * @param decryptedDefensiveNumber
     * @param offensiveNumber
     * @param difference
     * @param yards
     * @param timeoutUsed
     * @param homeTimeoutCalled
     * @param awayTimeoutcalled
     * @return
     */
    private fun updatePlayValues(
        game: Game,
        gamePlay: Play,
        playCall: PlayCall,
        result: Scenario,
        actualResult: ActualResult,
        initialPossession: TeamSide,
        homeScore: Int,
        awayScore: Int,
        runoffTime: Int,
        playTime: Int,
        ballLocation: Int,
        down: Int,
        yardsToGo: Int,
        decryptedDefensiveNumber: String,
        offensiveNumber: Int?,
        difference: Int?,
        yards: Int,
        timeoutUsed: Boolean,
        homeTimeoutCalled: Boolean,
        awayTimeoutcalled: Boolean,
    ): Play {
        var clock = gamePlay.clock.minus(runoffTime)
        var quarter = gamePlay.quarter
        var possession = initialPossession
        var finalPlayTime = playTime

        if (playCall != PlayCall.PAT && playCall != PlayCall.TWO_POINT) {
            val (updatedPossession, updatedClock, updatedQuarter) =
                handleEndOfQuarterNormalScenarios(
                    game,
                    gamePlay,
                    actualResult,
                    possession,
                    clock,
                    quarter,
                    homeScore,
                    awayScore,
                    playTime,
                )
            if (updatedQuarter != quarter) {
                finalPlayTime = clock
            }
            possession = updatedPossession
            clock = updatedClock
            quarter = updatedQuarter
        } else {
            // Handle end of quarter PAT scenarios
            val (updatedPossession, updatedClock, updatedQuarter) =
                handleEndOfQuarterPATScenarios(
                    game,
                    gamePlay,
                    possession,
                    clock,
                    quarter,
                    homeScore,
                    awayScore,
                )
            possession = updatedPossession
            clock = updatedClock
            quarter = updatedQuarter
        }

        // Update gamePlay values
        if (playCall == PlayCall.SPIKE || playCall == PlayCall.KNEEL) {
            gamePlay.defensiveNumber = null
            gamePlay.offensiveNumber = null
        } else {
            gamePlay.defensiveNumber = decryptedDefensiveNumber
            gamePlay.offensiveNumber = offensiveNumber?.toString()
        }
        gamePlay.playCall = playCall
        gamePlay.result = result
        gamePlay.actualResult = actualResult
        gamePlay.yards = yards
        gamePlay.playTime = finalPlayTime
        gamePlay.runoffTime = runoffTime
        gamePlay.difference = difference
        gamePlay.timeoutUsed = timeoutUsed
        gamePlay.homeScore = homeScore
        gamePlay.awayScore = awayScore
        gamePlay.playFinished = true

        if (initialPossession == TeamSide.HOME) {
            gamePlay.offensiveTimeoutCalled = homeTimeoutCalled
            gamePlay.defensiveTimeoutCalled = awayTimeoutcalled
        } else {
            gamePlay.offensiveTimeoutCalled = awayTimeoutcalled
            gamePlay.defensiveTimeoutCalled = homeTimeoutCalled
        }

        // Save the play and then update the game and stats
        playRepository.save(gamePlay)

        val game =
            gameService.updateGameInformation(
                game,
                gamePlay,
                homeScore,
                awayScore,
                possession,
                quarter,
                clock,
                ballLocation,
                down,
                yardsToGo,
                homeTimeoutCalled,
                awayTimeoutcalled,
                timeoutUsed,
            )

        scorebugService.generateScorebug(game)

        val allPlays = playRepository.getAllPlaysByGameId(game.gameId)
        gameStatsService.updateGameStats(
            game,
            allPlays,
            gamePlay,
        )

        return gamePlay
    }
}
