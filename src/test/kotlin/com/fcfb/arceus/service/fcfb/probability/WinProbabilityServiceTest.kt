package com.fcfb.arceus.service.fcfb.probability

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.service.fcfb.play.PlayService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.ml.XGBoostPredictor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WinProbabilityServiceTest {
    private val xgboostPredictor = mockk<XGBoostPredictor>(relaxed = true)
    private val gameStatsService = mockk<GameStatsService>(relaxed = true)
    private val winProbabilityService = WinProbabilityService(xgboostPredictor)

    private fun gameAfterPlay(possessionAfter: TeamSide) =
        Game().apply {
            gameId = 1
            homeTeam = "Home"
            awayTeam = "Away"
            possession = possessionAfter
            quarter = 2
            clock = "5:00"
            ballLocation = 25
            homeScore = 7
            awayScore = 3
        }

    @Test
    fun `a play stores the win chance after it for its offense and what it added`() {
        val game = gameAfterPlay(TeamSide.AWAY)
        val play = Play().apply { possession = TeamSide.HOME }
        every { xgboostPredictor.predict(any()) } returns 0.7

        winProbabilityService.recordPlayResult(game, play, 0.6, 1500.0, 1500.0)

        assertEquals(0.3, play.winProbability!!, 0.000001)
        assertEquals(-0.3, play.winProbabilityAdded!!, 0.000001)
        assertEquals(0.7, game.winProbability!!, 0.000001)
    }

    @Test
    fun `a kickoff that leaves the game unchanged adds nothing`() {
        val game = gameAfterPlay(TeamSide.HOME)
        val play = Play().apply { possession = TeamSide.AWAY }
        every { xgboostPredictor.predict(any()) } returns 0.003

        winProbabilityService.recordPlayResult(game, play, 0.003, 1500.0, 1500.0)

        assertEquals(0.997, play.winProbability!!, 0.000001)
        assertEquals(0.0, play.winProbabilityAdded!!, 0.000001)
    }

    @Test
    fun `the final play settles the game`() {
        val game = gameAfterPlay(TeamSide.HOME).apply { gameStatus = GameStatus.FINAL }
        val play = Play().apply { possession = TeamSide.HOME }

        winProbabilityService.recordPlayResult(game, play, 0.8, 1500.0, 1500.0)

        assertEquals(1.0, play.winProbability)
        assertEquals(0.2, play.winProbabilityAdded!!, 0.000001)
    }

    @Test
    fun `prediction failure falls back to an even game`() {
        val game = gameAfterPlay(TeamSide.HOME)
        val play = Play().apply { possession = TeamSide.HOME }
        every { xgboostPredictor.predict(any()) } throws RuntimeException("Model error")

        winProbabilityService.recordPlayResult(game, play, 0.5, 1500.0, 1500.0)

        assertEquals(0.5, play.winProbability)
        assertEquals(0.0, play.winProbabilityAdded!!, 0.000001)
    }

    @Test
    fun `rebuild scores each play from the next snap with its own score and settles a final game`() {
        val game = gameAfterPlay(TeamSide.AWAY).apply { gameStatus = GameStatus.FINAL }
        val first =
            Play().apply {
                playNumber = 1
                possession = TeamSide.HOME
                playFinished = true
                homeScore = 7
            }
        val last =
            Play().apply {
                playNumber = 2
                possession = TeamSide.AWAY
                playFinished = true
                homeScore = 7
                awayScore = 3
            }
        val plays = mockk<PlayService>(relaxed = true)
        every { gameStatsService.getGameStatsById(1) } returns emptyList()
        every { xgboostPredictor.predict(any()) } returnsMany listOf(0.5, 0.4)

        WinProbabilityOrchestrationService(winProbabilityService, mockk(), plays, mockk(), gameStatsService)
            .calculateWinProbabilitiesForSingleGame(1, game, listOf(last, first), Team(), Team())

        assertEquals(0.6, first.winProbability!!, 0.000001)
        assertEquals(0.1, first.winProbabilityAdded!!, 0.000001)
        assertEquals(0.0, last.winProbability!!, 0.000001)
        assertEquals(-0.4, last.winProbabilityAdded!!, 0.000001)
    }

    @Test
    fun `single game rebuild propagates the original persistence failure`() {
        val failure = IllegalStateException("Statistics unavailable")
        val plays = mockk<PlayService>()
        every { gameStatsService.getGameStatsById(7) } throws failure

        val thrown =
            assertThrows(IllegalStateException::class.java) {
                WinProbabilityOrchestrationService(winProbabilityService, mockk(), plays, mockk(), gameStatsService)
                    .calculateWinProbabilitiesForSingleGame(7, Game(), emptyList(), Team(), Team())
            }

        assertSame(failure, thrown)
        verify(exactly = 0) { plays.updatePlay(any()) }
    }

    @Test
    fun `batch rebuild continues after a failed game and excludes empty games`() {
        val failedGame = Game().apply { gameId = 1 }
        val emptyGame = Game().apply { gameId = 2 }
        val completedGame =
            Game().apply {
                gameId = 3
                homeTeam = "Home"
                awayTeam = "Away"
            }
        val play =
            Play().apply {
                playNumber = 1
                playFinished = true
            }
        val plays = mockk<PlayService>()
        val teams = mockk<TeamService>()
        every { plays.getAllPlaysByGameId(1) } throws IllegalStateException("Plays unavailable")
        every { plays.getAllPlaysByGameId(2) } returns emptyList()
        every { plays.getAllPlaysByGameId(3) } returns listOf(play)
        every { plays.updatePlay(play) } returns play
        every { teams.getTeamByName("Home") } returns Team()
        every { teams.getTeamByName("Away") } returns Team()
        every { gameStatsService.getGameStatsById(3) } returns emptyList()
        every { xgboostPredictor.predict(any()) } returns 0.7

        val response =
            WinProbabilityOrchestrationService(winProbabilityService, mockk(), plays, teams, gameStatsService)
                .calculateWinProbabilitiesForAllGames(listOf(failedGame, emptyGame, completedGame))

        assertEquals(3, response.totalGames)
        assertEquals(1, response.gamesProcessed)
        assertEquals(1, response.totalPlaysProcessed)
        assertEquals(listOf(3), response.processedGames.map { it.gameId })
        assertEquals(1, response.processedGames.single().playsProcessed)
        verify(exactly = 1) { plays.updatePlay(play) }
        verify(exactly = 2) { teams.getTeamByName(any()) }
    }

    @Test
    fun `stored probability response sorts plays and retains possession perspective and null handling`() {
        val awayPlay =
            Play().apply {
                playNumber = 3
                possession = TeamSide.AWAY
                winProbability = 0.3
            }
        val homePlay =
            Play().apply {
                playNumber = 1
                possession = TeamSide.HOME
                winProbability = 0.6
            }
        val missingProbability =
            Play().apply {
                playNumber = 2
                possession = TeamSide.AWAY
                winProbability = null
            }

        val response =
            WinProbabilityOrchestrationService(winProbabilityService, mockk(), mockk(), mockk(), gameStatsService)
                .getWinProbabilitiesForGame(7, listOf(awayPlay, homePlay, missingProbability))

        assertEquals(7, response.gameId)
        assertEquals(3, response.totalPlays)
        assertEquals(listOf(1, 2, 3), response.plays.map { it.playNumber })
        assertEquals(listOf(0.6, 1.0, 0.7), response.plays.map { it.homeTeamWinProbability })
        assertEquals(listOf(0.4, 0.0, 0.3), response.plays.map { it.awayTeamWinProbability })
        verify(exactly = 0) { xgboostPredictor.predict(any()) }
    }
}
