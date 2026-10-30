package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.schedule.ScheduleRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StadiumGamesServiceTest {
    private val gameRepository = mockk<GameRepository>()
    private val scheduleRepository = mockk<ScheduleRepository>()
    private val service = StadiumGamesService(gameRepository, scheduleRepository)

    private fun game(
        id: Int,
        type: GameType,
    ) = mockk<Game>(relaxed = true).also {
        every { it.gameId } returns id
        every { it.gameType } returns type
        every { it.gameStatus } returns GameStatus.FINAL
        every { it.homeTeam } returns "Wyoming"
        every { it.awayTeam } returns "Idaho"
        every { it.homeScore } returns 21
        every { it.awayScore } returns 14
    }

    @Test
    fun `scrimmages are left out of a stadium's games and records`() {
        every { gameRepository.findAllAtVenue("War Memorial Stadium") } returns
            listOf(game(1, GameType.CONFERENCE_GAME), game(2, GameType.SCRIMMAGE))

        val result = service.games(" War Memorial Stadium ")

        assertEquals(listOf(1), result.games.map { it.gameId })
        assertEquals(1, result.records.first { it.team == "Wyoming" }.wins)
    }

    @Test
    fun `scheduled scrimmages are left out of a stadium's upcoming games`() {
        val conference = mockk<Schedule>(relaxed = true).also { every { it.gameType } returns GameType.CONFERENCE_GAME }
        val scrimmage = mockk<Schedule>(relaxed = true).also { every { it.gameType } returns GameType.SCRIMMAGE }
        every { scheduleRepository.findUpcomingAtVenue("War Memorial Stadium") } returns listOf(conference, scrimmage)

        assertEquals(listOf(GameType.CONFERENCE_GAME), service.upcoming("War Memorial Stadium").map { it.gameType })
    }
}
