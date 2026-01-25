package com.fcfb.arceus.scheduler

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning.NONE
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.fcfb.PlayService
import com.fcfb.arceus.service.fcfb.ScorebugService
import com.fcfb.arceus.service.fcfb.UserService
import com.fcfb.arceus.util.Logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class DelayOfGameMonitor(
    private val gameService: GameService,
    private val userService: UserService,
    private val playService: PlayService,
    private val discordService: DiscordService,
    private val scorebugService: ScorebugService,
    private val playRepository: PlayRepository,
) {
    /**
     * Checks for delay of game every minute
     */
    @Scheduled(fixedRate = 60000)
    fun checkForDelayOfGame() {
        val warnedGamesFirstInstance = gameService.findGamesToWarnFirstInstance()
        warnedGamesFirstInstance.forEach { game ->
            discordService.notifyWarning(game, 1)
            gameService.updateGameAsWarned(game.gameId, 1)
            Logger.info(
                "Delay of game warning.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Home Team: ${game.homeTeam}\n" +
                    "Away Team: ${game.awayTeam}\n" +
                    "Instance: 1\n",
            )
        }
        val warnedGamesSecondInstance = gameService.findGamesToWarnSecondInstance()
        warnedGamesSecondInstance.forEach { game ->
            discordService.notifyWarning(game, 2)
            gameService.updateGameAsWarned(game.gameId, 2)
            Logger.info(
                "Delay of game warning.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Home Team: ${game.homeTeam}\n" +
                    "Away Team: ${game.awayTeam}\n" +
                    "Instance: 2\n",
            )
        }
//        val expiredGames = gameService.findExpiredTimers()
//        expiredGames.forEach { game ->
//            val updatedGame =
//                if (game.gameStatus == GameStatus.PREGAME) {
//                    applyPregameDelayOfGame(game)
//                } else {
//                    applyDelayOfGame(game)
//                }
//            val delayOfGameInstances = getDelayOfGameInstances(updatedGame)
//            val isDelayOfGameOut = delayOfGameInstances.first >= 3 || delayOfGameInstances.second >= 3
//            if (isDelayOfGameOut) {
//                gameService.endDOGOutGame(updatedGame, delayOfGameInstances)
//            }
//            discordService.notifyDelayOfGame(updatedGame, isDelayOfGameOut)
//            Logger.info("A delay of game for game ${game.gameId} has been processed")
//        }
    }

    /**
     * Get the delay of game instances for a given game
     * @return Pair of home and away delay of game instances
     */
    private fun getDelayOfGameInstances(game: Game): Pair<Int, Int> {
        if (game.gameType == GameType.SCRIMMAGE) {
            return Pair(0, 0)
        }
        if (game.waitingOn == TeamSide.HOME) {
            val instances = playService.getHomeDelayOfGameInstances(game.gameId)
            return Pair(instances, 0)
        } else {
            val instances = playService.getAwayDelayOfGameInstances(game.gameId)
            return Pair(0, instances)
        }
    }

    /**
     * Apply a delay of game to a game in pregame status
     * @param game
     */
    private fun applyPregameDelayOfGame(game: Game): Game {
        game.gameTimer = gameService.calculateDelayOfGameTimer()

        val teamToPenalize =
            if (game.coinTossWinner != null) {
                game.coinTossWinner!!
            } else {
                game.waitingOn
            }

        if (teamToPenalize == TeamSide.HOME) {
            game.awayScore += 8
            if (game.gameType != GameType.SCRIMMAGE) {
                for (coach in game.homeCoachDiscordIds!!) {
                    val user = userService.getUserByDiscordId(coach)
                    user.delayOfGameInstances += 1
                    userService.saveUser(user)
                }
            }
        } else {
            game.homeScore += 8
            if (game.gameType != GameType.SCRIMMAGE) {
                for (coach in game.awayCoachDiscordIds!!) {
                    val user = userService.getUserByDiscordId(coach)
                    user.delayOfGameInstances += 1
                    userService.saveUser(user)
                }
            }
        }

        val savedPlay = saveDelayOfGameOnOffensePlay(game.gameId, teamToPenalize)
        game.currentPlayId = savedPlay.playId
        game.gameWarning = NONE
        gameService.saveGame(game)
        scorebugService.generateScorebug(game)
        return game
    }

    /**
     * Apply a delay of game to a game
     * @param game
     */
    private fun applyDelayOfGame(game: Game): Game {
        game.gameTimer = gameService.calculateDelayOfGameTimer()

        val teamToPenalize = game.waitingOn

        if (teamToPenalize == TeamSide.HOME) {
            game.currentPlayType = PlayType.KICKOFF
            game.possession = TeamSide.AWAY
            game.awayScore += 8

            if (game.gameType != GameType.SCRIMMAGE) {
                for (coach in game.homeCoachDiscordIds!!) {
                    val user = userService.getUserByDiscordId(coach)
                    user.delayOfGameInstances += 1
                    userService.saveUser(user)
                }
            }
        } else {
            game.currentPlayType = PlayType.KICKOFF
            game.possession = TeamSide.HOME
            game.homeScore += 8

            if (game.gameType != GameType.SCRIMMAGE) {
                for (coach in game.awayCoachDiscordIds!!) {
                    val user = userService.getUserByDiscordId(coach)
                    user.delayOfGameInstances += 1
                    userService.saveUser(user)
                }
            }
        }

        val currentPlay =
            try {
                playService.getCurrentPlay(game.gameId)
            } catch (e: Exception) {
                null
            }

        val savedPlay =
            if (currentPlay != null) {
                // Use the existing play as delay of game (keep its playNumber)
                saveDelayOfGameOnDefensePlay(teamToPenalize, currentPlay)
            } else {
                saveDelayOfGameOnOffensePlay(game.gameId, teamToPenalize)
            }

        game.currentPlayId = savedPlay.playId
        game.gameWarning = NONE
        game.clockStopped = true
        // Set game.numPlays to match the play's playNumber to keep them in sync
        // This ensures the next play will have the correct playNumber
        game.numPlays = savedPlay.playNumber
        game.ballLocation = 35
        gameService.saveGame(game)
        scorebugService.generateScorebug(game)
        return game
    }

    /**
     * Save a delay of game on defense play, as defense has called a number
     * Uses the existing play and marks it as delay of game (keeps its playNumber)
     */
    private fun saveDelayOfGameOnDefensePlay(
        teamToPenalize: TeamSide,
        play: Play,
    ): Play {
        play.playFinished = true
        play.offensiveNumber = null
        play.defensiveNumber = null
        play.difference = null
        play.ballLocation = 35

        if (teamToPenalize == TeamSide.HOME) {
            play.result = Scenario.DELAY_OF_GAME_HOME
            play.actualResult = ActualResult.DELAY_OF_GAME
        } else {
            play.result = Scenario.DELAY_OF_GAME_AWAY
            play.actualResult = ActualResult.DELAY_OF_GAME
        }
        return playRepository.save(play)
    }

    /**
     * Save a delay of game on offense play, as defense hasn't called a number
     * @param gameId
     * @param teamToPenalize
     */
    private fun saveDelayOfGameOnOffensePlay(
        gameId: Int,
        teamToPenalize: TeamSide,
    ): Play {
        // defensiveNumberSubmitted creates a play with playNumber = game.numPlays + 1
        // This is already the next play number, so we don't need to increment it
        val play = playService.defensiveNumberSubmitted(gameId, "NONE", "NONE", 0, false)
        play.playFinished = true
        play.offensiveNumber = null
        play.defensiveNumber = null
        play.difference = null
        play.ballLocation = 35

        if (teamToPenalize == TeamSide.HOME) {
            play.result = Scenario.DELAY_OF_GAME_HOME
            play.actualResult = ActualResult.DELAY_OF_GAME
        } else {
            play.result = Scenario.DELAY_OF_GAME_AWAY
            play.actualResult = ActualResult.DELAY_OF_GAME
        }
        return playRepository.save(play)
    }
}
