package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.service.fcfb.play.FieldGoalPlayProcessor
import com.fcfb.arceus.service.fcfb.play.KickoffPlayProcessor
import com.fcfb.arceus.service.fcfb.play.NormalPlayProcessor
import com.fcfb.arceus.service.fcfb.play.PointAfterPlayProcessor
import com.fcfb.arceus.service.fcfb.play.PuntPlayProcessor
import com.fcfb.arceus.util.DefensiveNumberNotFound
import com.fcfb.arceus.util.EncryptionUtils
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.PlayNotFoundException
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class PlayService(
    private val playRepository: PlayRepository,
    private val encryptionUtils: EncryptionUtils,
    private val gameService: GameService,
    private val normalPlayProcessor: NormalPlayProcessor,
    private val fieldGoalPlayProcessor: FieldGoalPlayProcessor,
    private val puntPlayProcessor: PuntPlayProcessor,
    private val kickoffPlayProcessor: KickoffPlayProcessor,
    private val pointAfterPlayProcessor: PointAfterPlayProcessor,
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
                        normalPlayProcessor.runNormalPlay(
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
                        pointAfterPlayProcessor.runPointAfterPlay(
                            gamePlay,
                            game,
                            playCall,
                            offensiveNumber,
                            decryptedDefensiveNumber,
                        )

                PlayCall.KICKOFF_NORMAL, PlayCall.KICKOFF_ONSIDE, PlayCall.KICKOFF_SQUIB ->
                    gamePlay =
                        kickoffPlayProcessor.runKickoffPlay(
                            gamePlay,
                            game,
                            playCall,
                            offensiveNumber,
                            decryptedDefensiveNumber,
                        )

                PlayCall.FIELD_GOAL ->
                    gamePlay =
                        fieldGoalPlayProcessor.runFieldGoalPlay(
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
                        puntPlayProcessor.runPuntPlay(
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

    private fun getResponseSpeed(game: Game): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val parsedDateTime = game.lastMessageTimestamp?.let { LocalDateTime.parse(it, formatter) }
        val currentDateTime = LocalDateTime.now()
        return Duration.between(parsedDateTime, currentDateTime).seconds
    }

    fun getPlayById(playId: Int) =
        playRepository.getPlayById(playId)
            ?: throw PlayNotFoundException("Play with id $playId not found")

    fun getPreviousPlay(gameId: Int) =
        playRepository.getPreviousPlay(gameId)
            ?: throw PlayNotFoundException("No previous play found for game $gameId")

    fun getCurrentPlay(gameId: Int) =
        playRepository.getCurrentPlay(gameId)
            ?: throw PlayNotFoundException("No current play found for game $gameId")

    fun getCurrentPlayOrNull(gameId: Int) = playRepository.getCurrentPlay(gameId)

    fun getAllPlaysByGameId(gameId: Int) =
        playRepository.getAllPlaysByGameId(gameId).ifEmpty {
            throw PlayNotFoundException("No plays found for game $gameId")
        }

    fun getAllPlaysByDiscordTag(discordTag: String) =
        playRepository.getAllPlaysByDiscordTag(discordTag).ifEmpty {
            throw PlayNotFoundException("No plays found for user $discordTag")
        }

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

    fun getHomeDelayOfGameInstances(gameId: Int) =
        playRepository.getHomeDelayOfGameInstances(gameId)
            ?: throw PlayNotFoundException("No delay of game instances found for game $gameId")

    fun getAwayDelayOfGameInstances(gameId: Int) =
        playRepository.getAwayDelayOfGameInstances(gameId)
            ?: throw PlayNotFoundException("No delay of game instances found for game $gameId")

    fun getDelayOfGameCountsByWeek(
        season: Int,
        week: Int,
    ): Map<String, Int> {
        val results = playRepository.getDelayOfGameCountsByWeek(season, week)
        return results.associate { row ->
            val team = row[0] as String
            val count = (row[1] as Number).toInt()
            team to count
        }
    }

    fun getUserAverageResponseTime(
        discordTag: String,
        season: Int,
    ) = playRepository.getUserAverageResponseTime(discordTag, season)
        ?: throw Exception("Could not get average response time for user $discordTag")
}
