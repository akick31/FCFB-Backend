package com.fcfb.arceus.controllers.game

import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameSort
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.service.fcfb.game.GameCreationService
import com.fcfb.arceus.service.fcfb.game.GameFinalizationService
import com.fcfb.arceus.service.fcfb.game.GameService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class GameControllerTest {
    private lateinit var gameService: GameService
    private lateinit var gameController: GameController
    private val creation = mockk<GameCreationService>()
    private val finalization = mockk<GameFinalizationService>()
    private val creationController = GameCreationController(creation)
    private val finalizationController = GameFinalizationController(finalization)

    @BeforeEach
    fun setUp() {
        gameService = mockk()
        gameController = GameController(gameService)
    }

    @Test
    fun `getOngoingGameById should return game`() {
        val gameId = 1
        val mockGame = mockk<Game>()
        every { gameService.getGameById(gameId) } returns mockGame

        val response = gameController.getOngoingGameById(gameId)

        assertEquals(mockGame, response)
        verify { gameService.getGameById(gameId) }
    }

    @Test
    fun `getFilteredGames should return paginated games`() {
        val pageable = mockk<Pageable>()
        val mockPage = PageImpl(listOf(mockk<Game>()))
        every { gameService.getFilteredGames(any(), any(), any(), any(), any(), any(), any(), pageable) } returns mockPage

        val response =
            gameController.getFilteredGames(
                null,
                null,
                GameSort.CLOSEST_TO_END,
                "Big 12",
                1,
                1,
                null,
                pageable,
            )

        assertEquals(mockPage, response)
        verify { gameService.getFilteredGames(any(), any(), any(), any(), any(), any(), any(), pageable) }
    }

    @Test
    fun `startGame should return created game`() =
        runBlocking {
            val startRequest = mockk<StartRequest>()
            val mockGame = mockk<Game>()
            coEvery { creation.startSingleGame(startRequest, null) } returns mockGame

            val response = creationController.startGame(startRequest)

            assertEquals(mockGame, response)
            coVerify { creation.startSingleGame(startRequest, null) }
        }

    @Test
    fun `startOvertimeGame should return created game`() =
        runBlocking {
            val startRequest = mockk<StartRequest>()
            val mockGame = mockk<Game>()
            coEvery { creation.startOvertimeGame(startRequest) } returns mockGame

            val response = creationController.startOvertimeGame(startRequest)

            assertEquals(mockGame, response)
            coVerify { creation.startOvertimeGame(startRequest) }
        }

    @Test
    fun `endGame should return ended game`() {
        val channelId = 1234UL
        val mockGame = mockk<Game>()
        every { finalization.endSingleGameByChannelId(channelId) } returns mockGame

        val response = finalizationController.endGameByChannelId(channelId)

        assertEquals(mockGame, response)
        verify { finalization.endSingleGameByChannelId(channelId) }
    }

    @Test
    fun `endAllGames should return list of ended games`() {
        val mockGames = listOf(mockk<Game>())
        every { finalization.endAllGames() } returns mockGames

        val response = finalizationController.endAllGames()

        assertEquals(mockGames, response)
        verify { finalization.endAllGames() }
    }

    @Test
    fun `chewGame should return updated game`() {
        val channelId = 1234UL
        val mockGame = mockk<Game>()
        every { gameService.chewGameByPlatformId(channelId) } returns mockGame

        val response = gameController.chewGameByPlatformId(channelId)

        assertEquals(mockGame, response)
        verify { gameService.chewGameByPlatformId(channelId) }
    }

    @Test
    fun `chewAllGames should return list of chewed games`() {
        val mockGames = listOf(mockk<Game>())
        every { gameService.chewAllGames() } returns mockGames

        val response = gameController.chewAllGames()

        assertEquals(mockGames, response)
        verify { gameService.chewAllGames() }
    }

    @Test
    fun `unchewGame should return updated game`() {
        val channelId = 1234UL
        val mockGame = mockk<Game>()
        every { gameService.unchewGameByPlatformId(channelId) } returns mockGame

        val response = gameController.unchewGameByPlatformId(channelId)

        assertEquals(mockGame, response)
        verify { gameService.unchewGameByPlatformId(channelId) }
    }

    @Test
    fun `unchewAllGames should return list of unchewed games`() {
        val mockGames = listOf(mockk<Game>())
        every { gameService.unchewAllGames() } returns mockGames

        val response = gameController.unchewAllGames()

        assertEquals(mockGames, response)
        verify { gameService.unchewAllGames() }
    }

    @Test
    fun `updateRequestMessageId should return updated game`() {
        val gameId = 1
        val requestMessageId = "12345"
        val mockGame = mockk<Game>()
        every { gameService.updateRequestMessageId(gameId, requestMessageId) } returns mockGame

        val response = gameController.updateRequestMessageId(gameId, requestMessageId)

        assertEquals(mockGame, response)
        verify { gameService.updateRequestMessageId(gameId, requestMessageId) }
    }

    @Test
    fun `updateLastMessageTimestamp should return updated game`() {
        val gameId = 1
        val mockGame = mockk<Game>()
        every { gameService.updateLastMessageTimestamp(gameId) } returns mockGame

        val response = gameController.updateLastMessageTimestamp(gameId)

        assertEquals(mockGame, response)
        verify { gameService.updateLastMessageTimestamp(gameId) }
    }

    @Test
    fun `getGameByRequestMessageId should return game`() {
        val gameService = mockk<GameService>()

        val mockGame =
            Game(
                homeTeam = "Team A",
                awayTeam = "Team B",
                homeCoaches = listOf("Coach A1", "Coach A2"),
                awayCoaches = listOf("Coach B1", "Coach B2"),
                homeCoachDiscordIds = listOf("123456789", "987654321"),
                awayCoachDiscordIds = listOf("112233445", "556677889"),
                homeOffensivePlaybook = OffensivePlaybook.AIR_RAID,
                awayOffensivePlaybook = OffensivePlaybook.AIR_RAID,
                homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE,
                awayDefensivePlaybook = DefensivePlaybook.FOUR_THREE,
                homeScore = 21,
                awayScore = 14,
                possession = TeamSide.HOME,
                quarter = 2,
                clock = "5:30",
                ballLocation = 50,
                down = 2,
                yardsToGo = 8,
                tvChannel = TVChannel.ESPN,
                homeTeamRank = 5,
                homeWins = 10,
                homeLosses = 2,
                awayTeamRank = 8,
                awayWins = 8,
                awayLosses = 4,
                subdivision = Subdivision.FCFB,
                timestamp = "2023-10-01T12:00:00",
                winProbability = 0.75,
                season = 2023,
                week = 5,
                waitingOn = TeamSide.AWAY,
                numPlays = 45,
                homeTimeouts = 2,
                awayTimeouts = 3,
                coinTossWinner = TeamSide.HOME,
                coinTossChoice = CoinTossChoice.RECEIVE,
                overtimeCoinTossWinner = null,
                overtimeCoinTossChoice = null,
                homePlatform = com.fcfb.arceus.enums.system.Platform.DISCORD,
                homePlatformId = "homePlatform123",
                awayPlatform = com.fcfb.arceus.enums.system.Platform.DISCORD,
                awayPlatformId = "awayPlatform456",
                lastMessageTimestamp = "2023-10-01T12:30:00",
                gameTimer = "10/02/2023 06:00:00",
                gameWarning = GameWarning.NONE,
                currentPlayType = PlayType.NORMAL,
                currentPlayId = 101,
                clockStopped = false,
                requestMessageId = listOf("request123", "request456"),
                gameStatus = GameStatus.PREGAME,
                gameType = GameType.SCRIMMAGE,
                gameMode = GameMode.NORMAL,
                overtimeHalf = null,
                closeGame = false,
                closeGamePinged = false,
                upsetAlert = false,
                upsetAlertPinged = false,
            )
        every { gameService.getGameByRequestMessageId(eq("12345")) } returns mockGame

        val response = gameService.getGameByRequestMessageId("12345")

        assertEquals(mockGame, response)
    }

    @Test
    fun `getGameByPlatformId should return game`() {
        val platformId = 1234UL
        val mockGame = mockk<Game>()
        every { gameService.getGameByPlatformId(platformId) } returns mockGame

        val response = gameController.getGameByPlatformId(platformId)

        assertEquals(mockGame, response)
        verify { gameService.getGameByPlatformId(platformId) }
    }

    @Test
    fun `subCoachIntoGame should return updated game`() {
        val gameId = 1
        val team = "homeTeam"
        val discordId = "12345"
        val mockGame = mockk<Game>()
        every { gameService.subCoachIntoGame(gameId, team, discordId) } returns mockGame

        val response = gameController.subCoachIntoGame(gameId, team, discordId)

        assertEquals(mockGame, response)
        verify { gameService.subCoachIntoGame(gameId, team, discordId) }
    }

    @Test
    fun `restartGame should return restarted game`() =
        runBlocking {
            val channelId = 1234UL
            val mockGame = mockk<Game>()
            coEvery { creation.restartGame(channelId.toString()) } returns mockGame

            val response = creationController.restartGame(channelId.toString())

            assertEquals(mockGame, response)
            coVerify { creation.restartGame(channelId.toString()) }
        }

    @Test
    fun `markCloseGamePinged should return no content`() {
        val gameId = 1
        every { gameService.markCloseGamePinged(gameId) } just Runs

        val response = gameController.markCloseGamePinged(gameId)

        assertEquals(Unit, response)
        verify { gameService.markCloseGamePinged(gameId) }
    }

    @Test
    fun `markUpsetAlertPinged should return no content`() {
        val gameId = 1
        every { gameService.markUpsetAlertPinged(gameId) } just Runs

        val response = gameController.markUpsetAlertPinged(gameId)

        assertEquals(Unit, response)
        verify { gameService.markUpsetAlertPinged(gameId) }
    }

    @Test
    fun `deleteOngoingGame should return true`() {
        val channelId = 1234UL
        every { gameService.deleteOngoingGame(channelId) } returns true

        val response = gameController.deleteOngoingGame(channelId)

        assertEquals(true, response)
        verify { gameService.deleteOngoingGame(channelId) }
    }

    @Test
    fun `updateGame should return updated game`() {
        val mockGame = mockk<Game>()
        every { gameService.updateGame(mockGame) } returns mockGame

        val response = gameController.updateGame(mockGame)

        assertEquals(mockGame, response)
        verify { gameService.updateGame(mockGame) }
    }
}
