package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.exception.game.InvalidCoinTossChoiceException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.game.GameCoinToss
import com.fcfb.arceus.repositories.game.GameCoinTossRepository
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service
import java.util.Random

@Service
class GameCoinTossService(
    private val gameService: GameService,
    private val gameCoinTossRepository: GameCoinTossRepository,
) {
    private fun syncCoinToss(game: Game) {
        gameCoinTossRepository.save(
            GameCoinToss(
                gameId = game.gameId,
                coinTossWinner = game.coinTossWinner,
                coinTossChoice = game.coinTossChoice,
                overtimeCoinTossWinner = game.overtimeCoinTossWinner,
                overtimeCoinTossChoice = game.overtimeCoinTossChoice,
            ),
        )
    }

    fun runCoinToss(
        gameId: Int,
        coinTossCall: CoinTossCall,
    ): Game {
        val game = gameService.getGameById(gameId)

        try {
            val coinFlip = Random().nextInt(2)
            val coinTossWinner =
                if (
                    (coinFlip == 1 && coinTossCall == CoinTossCall.HEADS) ||
                    (coinFlip == 0 && coinTossCall == CoinTossCall.TAILS)
                ) {
                    TeamSide.AWAY
                } else {
                    TeamSide.HOME
                }
            if (game.gameStatus == GameStatus.PREGAME) {
                game.coinTossWinner = coinTossWinner
                game.waitingOn = coinTossWinner
            } else if (game.gameStatus == GameStatus.END_OF_REGULATION) {
                game.overtimeCoinTossWinner = coinTossWinner
                game.waitingOn = coinTossWinner
            }
            game.gameTimer = gameService.calculateDelayOfGameTimer()
            Logger().info(
                "Coin toss finished.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Coin Toss Winner: $coinTossWinner",
            )
            gameService.saveGame(game)
            syncCoinToss(game)
            return game
        } catch (e: Exception) {
            Logger().error(
                "Coin toss error.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Error Message: ${e.message ?: "Unknown error"}",
            )
            throw e
        }
    }

    fun makeCoinTossChoice(
        gameId: Int,
        coinTossChoice: CoinTossChoice,
    ): Game {
        val game = gameService.getGameById(gameId)

        try {
            game.coinTossChoice = coinTossChoice
            if (game.coinTossWinner == TeamSide.HOME && coinTossChoice == CoinTossChoice.RECEIVE) {
                game.possession = TeamSide.AWAY
                game.waitingOn = TeamSide.HOME
            } else if (game.coinTossWinner == TeamSide.HOME && coinTossChoice == CoinTossChoice.DEFER) {
                game.possession = TeamSide.HOME
                game.waitingOn = TeamSide.AWAY
            } else if (game.coinTossWinner == TeamSide.AWAY && coinTossChoice == CoinTossChoice.RECEIVE) {
                game.possession = TeamSide.HOME
                game.waitingOn = TeamSide.AWAY
            } else if (game.coinTossWinner == TeamSide.AWAY && coinTossChoice == CoinTossChoice.DEFER) {
                game.possession = TeamSide.AWAY
                game.waitingOn = TeamSide.HOME
            }
            game.gameStatus = GameStatus.OPENING_KICKOFF
            game.gameTimer = gameService.calculateDelayOfGameTimer()
            Logger().info(
                "Coin toss choice made.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Coin Toss Winner: ${game.coinTossWinner}\n" +
                    "Coin Toss Choice: $coinTossChoice",
            )
            return gameService.saveGame(game).also { syncCoinToss(it) }
        } catch (e: Exception) {
            Logger().error("Error in ${game.gameId}: ${e.message ?: "Unknown error"}")
            if (e is IllegalArgumentException) {
                throw InvalidCoinTossChoiceException("Invalid coin toss choice: $coinTossChoice")
            }
            throw e
        }
    }

    fun makeOvertimeCoinTossChoice(
        gameId: Int,
        coinTossChoice: OvertimeCoinTossChoice,
    ): Game {
        val game = gameService.getGameById(gameId)

        try {
            game.overtimeCoinTossChoice = coinTossChoice
            if (game.overtimeCoinTossWinner == TeamSide.HOME && coinTossChoice == OvertimeCoinTossChoice.DEFENSE) {
                game.possession = TeamSide.AWAY
                game.waitingOn = TeamSide.HOME
            } else if (game.overtimeCoinTossWinner == TeamSide.HOME && coinTossChoice == OvertimeCoinTossChoice.OFFENSE) {
                game.possession = TeamSide.HOME
                game.waitingOn = TeamSide.AWAY
            } else if (game.overtimeCoinTossWinner == TeamSide.AWAY && coinTossChoice == OvertimeCoinTossChoice.DEFENSE) {
                game.possession = TeamSide.HOME
                game.waitingOn = TeamSide.AWAY
            } else if (game.overtimeCoinTossWinner == TeamSide.AWAY && coinTossChoice == OvertimeCoinTossChoice.OFFENSE) {
                game.possession = TeamSide.AWAY
                game.waitingOn = TeamSide.HOME
            }
            game.gameStatus = GameStatus.OVERTIME
            Logger().info(
                "Overtime coin toss choice made.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Coin Toss Winner: ${game.overtimeCoinTossWinner}\n" +
                    "Overtime Coin Toss Choice: $coinTossChoice",
            )
            return gameService.saveGame(game).also { syncCoinToss(it) }
        } catch (e: Exception) {
            Logger().error(
                "Overtime coin toss choice error.\n" +
                    "Game ID: ${game.gameId}\n" +
                    "Error Message: ${e.message ?: "Unknown error"}",
            )
            if (e is IllegalArgumentException) {
                throw InvalidCoinTossChoiceException("Invalid coin toss choice $coinTossChoice")
            }
            throw e
        }
    }
}
