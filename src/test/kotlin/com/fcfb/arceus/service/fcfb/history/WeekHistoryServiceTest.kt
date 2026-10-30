package com.fcfb.arceus.service.fcfb.history

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.service.fcfb.play.WinProbabilitySwingService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WeekHistoryServiceTest {
    private val gameRepository = mockk<GameRepository>()
    private val swingService = mockk<WinProbabilitySwingService>()
    private val service = WeekHistoryService(gameRepository, swingService)

    private fun game(
        id: Int,
        status: GameStatus,
    ) = Game().apply {
        gameId = id
        homeTeam = "Home"
        awayTeam = "Away"
        homeScore = 14
        awayScore = 7
        homeTeamRank = 30
        awayTeamRank = 8
        gameStatus = status
    }

    @Test
    fun `a week lists only finished games and drops ranks outside the top 25`() {
        every { gameRepository.getGamesBySeasonAndWeek(11, 4) } returns
            listOf(game(1, GameStatus.FINAL), game(2, GameStatus.IN_PROGRESS))
        every { swingService.biggestSwing(11, 4) } returns null

        val week = service.week(11, 4)

        assertEquals(listOf(1), week.games.map { it.gameId })
        assertNull(week.games.single().homeRank)
        assertEquals(8, week.games.single().awayRank)
        assertEquals(listOf(1), week.highlights.upsets.map { it.gameId })
        assertEquals(listOf(8), week.highlights.rankedLosses.map { it.rank })
    }

    @Test
    fun `weeks come from finished games`() {
        every { gameRepository.getFinishedWeeksBySeason(11) } returns listOf(1, 2, 3)

        assertEquals(listOf(1, 2, 3), service.finishedWeeks(11))
    }
}
