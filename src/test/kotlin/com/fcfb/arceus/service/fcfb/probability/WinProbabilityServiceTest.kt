package com.fcfb.arceus.service.fcfb.probability

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.PlayRepository
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class WinProbabilityServiceTest {
    private val xgboostPredictor = mockk<XGBoostPredictor>(relaxed = true)
    private val playRepository = mockk<PlayRepository>(relaxed = true)
    private val gameStatsService = mockk<GameStatsService>(relaxed = true)
    private val winProbabilityService = WinProbabilityService(xgboostPredictor, playRepository)

    @ParameterizedTest
    @CsvSource(
        "HOME, HOME, 0.6, 0.7, 0.1",
        "AWAY, AWAY, 0.3, 0.4, 0.1",
        "HOME, AWAY, 0.6, 0.3, 0.1",
        "AWAY, HOME, 0.3, 0.6, -0.1",
    )
    fun `win probability added uses the new prediction`(
        previousPossession: TeamSide,
        possession: TeamSide,
        previousProbability: Double,
        prediction: Double,
        expectedAdded: Double,
    ) {
        val game =
            Game().apply {
                gameId = 1
                winProbability = previousProbability
            }
        val previousPlay =
            Play().apply {
                playNumber = 1
                this.possession = previousPossession
                winProbability = previousProbability
            }
        val play =
            Play().apply {
                playNumber = 2
                this.possession = possession
                playCall = PlayCall.RUN
                winProbability = previousProbability
            }
        every { playRepository.getAllPlaysByGameId(1) } returns listOf(previousPlay, play)
        every { xgboostPredictor.predict(any()) } returns prediction

        val probability = winProbabilityService.calculateWinProbability(game, play, 1500.0, 1500.0)

        assertEquals(prediction, probability, 0.000001)
        assertEquals(prediction, play.winProbability)
        assertEquals(expectedAdded, play.winProbabilityAdded!!, 0.000001)
    }

    @Test
    fun `first play retains the game probability baseline`() {
        val game =
            Game().apply {
                gameId = 1
                winProbability = 0.5
            }
        val play =
            Play().apply {
                playNumber = 1
                playCall = PlayCall.RUN
                winProbability = 0.5
            }
        every { playRepository.getAllPlaysByGameId(1) } returns listOf(play)
        every { xgboostPredictor.predict(any()) } returns 0.7

        winProbabilityService.calculateWinProbability(game, play, 1500.0, 1500.0)

        assertEquals(0.7, play.winProbability)
        assertEquals(0.2, play.winProbabilityAdded!!, 0.000001)
    }

    @Test
    fun `prediction failure retains the existing fallback`() {
        val game = Game().apply { gameId = 1 }
        val play = Play().apply { playCall = PlayCall.RUN }
        every { xgboostPredictor.predict(any()) } throws RuntimeException("Model error")

        val probability = winProbabilityService.calculateWinProbability(game, play, 1500.0, 1500.0)

        assertEquals(0.5, probability)
        assertEquals(0.5, play.winProbability)
        assertEquals(0.0, play.winProbabilityAdded)
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
        val play = Play().apply { playNumber = 1 }
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
