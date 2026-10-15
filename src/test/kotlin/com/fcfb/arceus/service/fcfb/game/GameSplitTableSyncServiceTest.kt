package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.system.Platform
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.game.GameCoinToss
import com.fcfb.arceus.model.game.GamePresentation
import com.fcfb.arceus.model.game.GameRecordSnapshot
import com.fcfb.arceus.model.game.GameVegasOdds
import com.fcfb.arceus.model.game.PlatformTransport
import com.fcfb.arceus.repositories.game.GameCoinTossRepository
import com.fcfb.arceus.repositories.game.GamePresentationRepository
import com.fcfb.arceus.repositories.game.GameRecordSnapshotRepository
import com.fcfb.arceus.repositories.game.GameVegasOddsRepository
import com.fcfb.arceus.repositories.game.PlatformTransportRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameSplitTableSyncServiceTest {
    private val platformTransportRepository = mockk<PlatformTransportRepository>(relaxed = true)
    private val gamePresentationRepository = mockk<GamePresentationRepository>(relaxed = true)
    private val gameRecordSnapshotRepository = mockk<GameRecordSnapshotRepository>(relaxed = true)
    private val gameCoinTossRepository = mockk<GameCoinTossRepository>(relaxed = true)
    private val gameVegasOddsRepository = mockk<GameVegasOddsRepository>(relaxed = true)
    private val service =
        GameSplitTableSyncService(
            platformTransportRepository,
            gamePresentationRepository,
            gameRecordSnapshotRepository,
            gameCoinTossRepository,
            gameVegasOddsRepository,
        )

    init {
        every { platformTransportRepository.save(any()) } answers { firstArg() }
        every { gamePresentationRepository.save(any()) } answers { firstArg() }
        every { gameRecordSnapshotRepository.save(any()) } answers { firstArg() }
        every { gameCoinTossRepository.save(any()) } answers { firstArg() }
        every { gameVegasOddsRepository.save(any()) } answers { firstArg() }
    }

    private val game =
        Game().apply {
            gameId = 42
            homePlatform = Platform.DISCORD
            homePlatformId = "111"
            awayPlatform = Platform.DISCORD
            awayPlatformId = "222"
            requestMessageId = listOf("333")
            lastMessageTimestamp = "2026-01-01 00:00:00"
            tvChannel = null
            postseasonGameLogo = "logo.png"
            postseasonGameName = "Bowl"
            neutralSite = true
            venue = "Stadium"
            homeTeamRank = 1
            awayTeamRank = 2
            homeWins = 3
            homeLosses = 4
            awayWins = 5
            awayLosses = 6
            coinTossWinner = TeamSide.HOME
            coinTossChoice = CoinTossChoice.RECEIVE
            overtimeCoinTossWinner = TeamSide.AWAY
            overtimeCoinTossChoice = OvertimeCoinTossChoice.DEFENSE
            homeVegasSpread = -3.5
            awayVegasSpread = 3.5
        }

    @Test
    fun `sync writes platform transport fields from the game`() {
        val captured = slot<PlatformTransport>()
        service.sync(game)
        verify(exactly = 1) { platformTransportRepository.save(capture(captured)) }
        assertEquals(42, captured.captured.gameId)
        assertEquals(Platform.DISCORD, captured.captured.homePlatform)
        assertEquals("111", captured.captured.homePlatformId)
        assertEquals(Platform.DISCORD, captured.captured.awayPlatform)
        assertEquals("222", captured.captured.awayPlatformId)
        assertEquals(listOf("333"), captured.captured.requestMessageId)
        assertEquals("2026-01-01 00:00:00", captured.captured.lastMessageTimestamp)
    }

    @Test
    fun `sync writes presentation fields from the game`() {
        val captured = slot<GamePresentation>()
        service.sync(game)
        verify(exactly = 1) { gamePresentationRepository.save(capture(captured)) }
        assertEquals(42, captured.captured.gameId)
        assertEquals("logo.png", captured.captured.postseasonGameLogo)
        assertEquals("Bowl", captured.captured.postseasonGameName)
        assertEquals(true, captured.captured.neutralSite)
        assertEquals("Stadium", captured.captured.venue)
    }

    @Test
    fun `sync writes record snapshot fields from the game`() {
        val captured = slot<GameRecordSnapshot>()
        service.sync(game)
        verify(exactly = 1) { gameRecordSnapshotRepository.save(capture(captured)) }
        assertEquals(42, captured.captured.gameId)
        assertEquals(1, captured.captured.homeTeamRank)
        assertEquals(2, captured.captured.awayTeamRank)
        assertEquals(3, captured.captured.homeWins)
        assertEquals(4, captured.captured.homeLosses)
        assertEquals(5, captured.captured.awayWins)
        assertEquals(6, captured.captured.awayLosses)
    }

    @Test
    fun `sync writes coin toss fields from the game`() {
        val captured = slot<GameCoinToss>()
        service.sync(game)
        verify(exactly = 1) { gameCoinTossRepository.save(capture(captured)) }
        assertEquals(42, captured.captured.gameId)
        assertEquals(TeamSide.HOME, captured.captured.coinTossWinner)
        assertEquals(CoinTossChoice.RECEIVE, captured.captured.coinTossChoice)
        assertEquals(TeamSide.AWAY, captured.captured.overtimeCoinTossWinner)
        assertEquals(OvertimeCoinTossChoice.DEFENSE, captured.captured.overtimeCoinTossChoice)
    }

    @Test
    fun `sync writes vegas odds fields from the game`() {
        val captured = slot<GameVegasOdds>()
        service.sync(game)
        verify(exactly = 1) { gameVegasOddsRepository.save(capture(captured)) }
        assertEquals(42, captured.captured.gameId)
        assertEquals(-3.5, captured.captured.homeVegasSpread)
        assertEquals(3.5, captured.captured.awayVegasSpread)
    }
}
