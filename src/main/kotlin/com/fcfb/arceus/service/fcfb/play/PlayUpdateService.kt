package com.fcfb.arceus.service.fcfb.play

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.graphics.ScorebugService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import org.springframework.stereotype.Component

@Component
class PlayUpdateService(
    private val playRepository: PlayRepository,
    private val gameService: GameService,
    private val gameStatsService: GameStatsService,
    private val scorebugService: ScorebugService,
) {
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

        if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && quarter < 4) {
            quarter += 1
            clock = 420 - playTime
            if (quarter == 3) {
                possession = gameService.handleHalfTimePossessionChange(game)
                clock = 420
            }
        } else if (clock <= 0 && !gameService.isTouchdownPlay(actualResult) && gamePlay.quarter == 4) {
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

    fun updatePlayValues(
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
        )

        return gamePlay
    }
}
