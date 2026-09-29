package com.fcfb.arceus.service.fcfb.elo

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EloServiceTest {
    private val gameRepository = mockk<GameRepository>(relaxed = true)
    private val gameStatsRepository = mockk<GameStatsRepository>(relaxed = true)
    private val teamRepository = mockk<TeamRepository>(relaxed = true)
    private val service = EloService(teamRepository, gameStatsRepository, gameRepository)

    private val airForce = team("Air Force", 1464.29)
    private val wyoming = team("Wyoming", 1759.49)
    private val gameOneAirForceRow = statsRow(1, "Air Force", 1473.28)
    private val gameOneWyomingRow = statsRow(1, "Wyoming", 1754.63)
    private val gameOne =
        Game().apply {
            gameId = 1
            season = 1
            week = 1
            homeTeam = "Air Force"
            awayTeam = "Wyoming"
            homeScore = 28
            awayScore = 38
            gameStatus = GameStatus.FINAL
            gameType = GameType.OUT_OF_CONFERENCE
            homeVegasSpread = -9.5
            awayVegasSpread = 9.5
        }

    init {
        every { gameRepository.getAllGames() } returns listOf(gameOne)
        every { teamRepository.findAll() } returns listOf(airForce, wyoming)
        every { gameStatsRepository.findAll() } returns listOf(gameOneAirForceRow, gameOneWyomingRow)
    }

    @Test
    fun `dry run reports changes without saving`() {
        val result = service.rebuild(apply = false)

        assertEquals(2, result.teamsChanged.size)
        assertEquals(1, result.gameSpreadsChanged)
        assertEquals(2, result.gameStatsRowsChanged)
        assertEquals(-9.5, gameOne.homeVegasSpread!!, 1e-9)
        verify(exactly = 0) { teamRepository.saveAll(any<Iterable<Team>>()) }
        verify(exactly = 0) { gameStatsRepository.saveAll(any<Iterable<GameStats>>()) }
        assertEquals(1464.29, airForce.currentElo, 1e-9)
    }

    @Test
    fun `apply writes replayed ratings and pre game snapshots`() {
        service.rebuild(apply = true)

        assertEquals(1500.0 - 16.0, airForce.currentElo, 1e-9)
        assertEquals(1500.0 + 16.0, wyoming.currentElo, 1e-9)
        assertEquals(1500.0, gameOneAirForceRow.teamElo, 1e-9)
        assertEquals(1500.0, gameOneWyomingRow.teamElo, 1e-9)
        assertEquals(-2.5, gameOne.homeVegasSpread!!, 1e-9)
        assertEquals(2.5, gameOne.awayVegasSpread!!, 1e-9)
        assertEquals(2.5, gameOneAirForceRow.favoredMargin!!, 1e-9)
        assertEquals(0.0, gameOneAirForceRow.upsetMargin!!, 1e-9)
        assertEquals(-2.5, gameOneWyomingRow.favoredMargin!!, 1e-9)
        assertEquals(2.5, gameOneWyomingRow.upsetMargin!!, 1e-9)
        verify(exactly = 1) { teamRepository.saveAll(any<Iterable<Team>>()) }
        verify(exactly = 1) { gameStatsRepository.saveAll(any<Iterable<GameStats>>()) }
    }

    @Test
    fun `rows already matching the replay are left alone`() {
        gameOneAirForceRow.teamElo = 1500.0
        gameOneWyomingRow.teamElo = 1500.0
        gameOneAirForceRow.favoredMargin = 2.5
        gameOneAirForceRow.upsetMargin = 0.0
        gameOneWyomingRow.favoredMargin = -2.5
        gameOneWyomingRow.upsetMargin = 2.5
        gameOne.homeVegasSpread = -2.5
        gameOne.awayVegasSpread = 2.5
        airForce.currentElo = 1484.0
        wyoming.currentElo = 1516.0

        val result = service.rebuild(apply = false)

        assertEquals(0, result.teamsChanged.size)
        assertEquals(0, result.gameSpreadsChanged)
        assertEquals(0, result.gameStatsRowsChanged)
    }

    private fun team(
        teamName: String,
        elo: Double,
    ) = Team().apply {
        name = teamName
        currentElo = elo
    }

    private fun statsRow(
        id: Int,
        teamName: String,
        elo: Double,
    ) = GameStats(gameId = id, team = teamName, teamElo = elo)
}
