package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.exception.statistics.GameStatsNotFoundException
import com.fcfb.arceus.exception.statistics.InvalidGameStatsRequestException
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GameStatsReadTest {
    private val repository = mockk<GameStatsRepository>()
    private val service = GameStatsService(repository, mockk(), mockk(), mockk(), mockk())

    @Test
    fun `game read returns the existing repository row unchanged`() {
        val stats = GameStats(gameId = 123, team = "Texas", season = 12)
        every { repository.getGameStatsByIdAndTeam(123, "Texas") } returns stats

        assertSame(stats, service.getGameStatsForGame(123, "Texas"))
        verify(exactly = 1) { repository.getGameStatsByIdAndTeam(123, "Texas") }
    }

    @Test
    fun `season read preserves repository order and empty results`() {
        val stats = listOf(GameStats(gameId = 456, team = "Texas"), GameStats(gameId = 123, team = "Texas"))
        val empty = emptyList<GameStats>()
        every { repository.findByTeamAndSeason("Texas", 12) } returns stats
        every { repository.findByTeamAndSeason("Texas", 13) } returns empty

        assertSame(stats, service.getGameStatsForTeamSeason("Texas", 12))
        assertSame(empty, service.getGameStatsForTeamSeason("Texas", 13))
    }

    @Test
    fun `missing selectors retain boundary validation without querying`() {
        assertThrows(InvalidGameStatsRequestException::class.java) { service.getGameStatsForGame(null, "Texas") }
        assertThrows(InvalidGameStatsRequestException::class.java) { service.getGameStatsForTeamSeason("Texas", null) }
        verify { repository wasNot Called }
    }

    @Test
    fun `missing game retains the existing domain exception`() {
        every { repository.getGameStatsByIdAndTeam(123, "Texas") } returns null
        assertThrows(GameStatsNotFoundException::class.java) { service.getGameStatsForGame(123, "Texas") }
    }

    @Test
    fun `read failures propagate unchanged`() {
        val failure = IllegalStateException("Database unavailable")
        every { repository.getGameStatsByIdAndTeam(123, "Texas") } throws failure
        every { repository.findByTeamAndSeason("Texas", 12) } throws failure

        assertSame(failure, assertThrows(IllegalStateException::class.java) { service.getGameStatsForGame(123, "Texas") })
        assertSame(failure, assertThrows(IllegalStateException::class.java) { service.getGameStatsForTeamSeason("Texas", 12) })
    }
}
