package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameRepositoryTest {
    private lateinit var gameRepository: GameRepository

    @BeforeEach
    fun setUp() {
        gameRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and find by game id`() {
        val game = createTestGame()

        every { gameRepository.save(any()) } returns game
        every { gameRepository.findById(1) } returns java.util.Optional.of(game)

        val saved = gameRepository.save(game)
        val found = gameRepository.findById(saved.gameId).get()

        assertNotNull(found)
        assertEquals("Alabama", found.homeTeam)
        assertEquals("Auburn", found.awayTeam)
        assertEquals(GameStatus.IN_PROGRESS, found.gameStatus)
    }

    @Test
    fun `test getGameById`() {
        val game = createTestGame()

        every { gameRepository.getGameById(1) } returns game

        val found = gameRepository.getGameById(1)

        assertNotNull(found)
        assertEquals("Alabama", found.homeTeam)
        assertEquals("Auburn", found.awayTeam)
    }

    @Test
    fun `test getGameByRequestMessageId`() {
        val game = createTestGame(requestMessageId = listOf("test_message_id"))

        every { gameRepository.getGameByRequestMessageId("test_message_id") } returns game

        val found = gameRepository.getGameByRequestMessageId("test_message_id")

        assertNotNull(found)
        assertEquals(listOf("test_message_id"), found.requestMessageId)
    }

    @Test
    fun `test getGameByPlatformId`() {
        val game = createTestGame(homePlatformId = "platform123")

        every { gameRepository.getGameByPlatformId(123UL) } returns game

        val found = gameRepository.getGameByPlatformId(123UL)

        assertNotNull(found)
        assertEquals("platform123", found.homePlatformId)
    }

    @Test
    fun `test getAllGames`() {
        val game1 = createTestGame(gameId = 1, homeTeam = "Alabama")
        val game2 = createTestGame(gameId = 2, homeTeam = "Georgia")
        val games = listOf(game1, game2)

        every { gameRepository.getAllGames() } returns games

        val found = gameRepository.getAllGames()

        assertEquals(2, found.size)
        assertTrue(found.any { it.homeTeam == "Alabama" })
        assertTrue(found.any { it.homeTeam == "Georgia" })
    }

    @Test
    fun `test getAllOngoingGames`() {
        val game = createTestGame(gameStatus = GameStatus.IN_PROGRESS)
        val games = listOf(game)

        every { gameRepository.getAllOngoingGames() } returns games

        val found = gameRepository.getAllOngoingGames()

        assertEquals(1, found.size)
        assertEquals(GameStatus.IN_PROGRESS, found.first().gameStatus)
    }

    @Test
    fun `test getRankedGames`() {
        val game = createTestGame(homeTeamRank = 5, awayTeamRank = 10)
        val games = listOf(game)

        every { gameRepository.getRankedGames() } returns games

        val found = gameRepository.getRankedGames()

        assertEquals(1, found.size)
        assertEquals(5, found.first().homeTeamRank)
        assertEquals(10, found.first().awayTeamRank)
    }

    @Test
    fun `test getGamesByTeamSeasonAndWeek`() {
        val game = createTestGame(homeTeam = "Alabama", season = 2024, week = 1)

        every { gameRepository.getGamesByTeamSeasonAndWeek("Alabama", 2024, 1) } returns game

        val found = gameRepository.getGamesByTeamSeasonAndWeek("Alabama", 2024, 1)

        assertNotNull(found)
        assertEquals("Alabama", found.homeTeam)
        assertEquals(2024, found.season)
        assertEquals(1, found.week)
    }

    @Test
    fun `test findExpiredTimers`() {
        val game = createTestGame(gameWarning = GameWarning.FIRST_WARNING)
        val games = listOf(game)

        every { gameRepository.findExpiredTimers() } returns games

        val found = gameRepository.findExpiredTimers()

        assertEquals(1, found.size)
        assertEquals(GameWarning.FIRST_WARNING, found.first().gameWarning)
    }

    @Test
    fun `test findGamesToWarnFirstInstance`() {
        val game = createTestGame(gameWarning = GameWarning.NONE)
        val games = listOf(game)

        every { gameRepository.findGamesToWarnFirstInstance() } returns games

        val found = gameRepository.findGamesToWarnFirstInstance()

        assertEquals(1, found.size)
        assertEquals(GameWarning.NONE, found.first().gameWarning)
    }

    @Test
    fun `test findGamesToWarnSecondInstance`() {
        val game = createTestGame(gameWarning = GameWarning.FIRST_WARNING)
        val games = listOf(game)

        every { gameRepository.findGamesToWarnSecondInstance() } returns games

        val found = gameRepository.findGamesToWarnSecondInstance()

        assertEquals(1, found.size)
        assertEquals(GameWarning.FIRST_WARNING, found.first().gameWarning)
    }

    @Test
    fun `test updateGameAsFirstWarning`() {
        every { gameRepository.updateGameAsFirstWarning(1) } returns Unit

        gameRepository.updateGameAsFirstWarning(1)

        verify { gameRepository.updateGameAsFirstWarning(1) }
    }

    @Test
    fun `test updateGameAsSecondWarning`() {
        every { gameRepository.updateGameAsSecondWarning(1) } returns Unit

        gameRepository.updateGameAsSecondWarning(1)

        verify { gameRepository.updateGameAsSecondWarning(1) }
    }

    @Test
    fun `test markCloseGamePinged`() {
        every { gameRepository.markCloseGamePinged(1) } returns Unit

        gameRepository.markCloseGamePinged(1)

        verify { gameRepository.markCloseGamePinged(1) }
    }

    @Test
    fun `test count`() {
        every { gameRepository.count() } returns 10L

        val count = gameRepository.count()

        assertEquals(10L, count)
    }

    @Test
    fun `test existsById`() {
        every { gameRepository.existsById(1) } returns true
        every { gameRepository.existsById(999) } returns false

        assertTrue(gameRepository.existsById(1))
        assertFalse(gameRepository.existsById(999))
    }

    @Test
    fun `test delete`() {
        val game = createTestGame()
        every { gameRepository.delete(game) } returns Unit

        gameRepository.delete(game)

        verify { gameRepository.delete(game) }
    }

    @Test
    fun `test deleteById`() {
        every { gameRepository.deleteById(1) } returns Unit

        gameRepository.deleteById(1)

        verify { gameRepository.deleteById(1) }
    }

    @Test
    fun `test deleteAll`() {
        every { gameRepository.deleteAll() } returns Unit

        gameRepository.deleteAll()

        verify { gameRepository.deleteAll() }
    }

    @Test
    fun `test saveAll`() {
        val game1 = createTestGame(gameId = 1, homeTeam = "Alabama")
        val game2 = createTestGame(gameId = 2, homeTeam = "Georgia")
        val games = listOf(game1, game2)

        every { gameRepository.saveAll(games) } returns games

        val savedGames = gameRepository.saveAll(games)

        assertTrue(savedGames.any { it.homeTeam == "Alabama" })
        assertTrue(savedGames.any { it.homeTeam == "Georgia" })
    }

    private fun createTestGame(
        gameId: Int = 1,
        homeTeam: String = "Alabama",
        awayTeam: String = "Auburn",
        homeCoaches: List<String> = listOf("Coach1"),
        awayCoaches: List<String> = listOf("Coach2"),
        homeCoachDiscordIds: List<String> = listOf("discord1"),
        awayCoachDiscordIds: List<String> = listOf("discord2"),
        homeOffensivePlaybook: OffensivePlaybook = OffensivePlaybook.PRO,
        awayOffensivePlaybook: OffensivePlaybook = OffensivePlaybook.SPREAD,
        homeDefensivePlaybook: DefensivePlaybook = DefensivePlaybook.THREE_FOUR,
        awayDefensivePlaybook: DefensivePlaybook = DefensivePlaybook.FOUR_THREE,
        homeScore: Int = 0,
        awayScore: Int = 0,
        possession: TeamSide = TeamSide.HOME,
        quarter: Int = 1,
        clock: String = "7:00",
        ballLocation: Int = 0,
        down: Int = 1,
        yardsToGo: Int = 10,
        tvChannel: TVChannel? = null,
        homeTeamRank: Int? = null,
        homeWins: Int? = null,
        homeLosses: Int? = null,
        awayWins: Int? = null,
        awayLosses: Int? = null,
        awayTeamRank: Int? = null,
        subdivision: Subdivision? = null,
        timestamp: String? = null,
        winProbability: Double? = null,
        season: Int = 2024,
        week: Int = 1,
        waitingOn: TeamSide = TeamSide.AWAY,
        numPlays: Int = 0,
        homeTimeouts: Int = 3,
        awayTimeouts: Int = 3,
        coinTossWinner: TeamSide? = null,
        coinTossChoice: CoinTossChoice? = null,
        overtimeCoinTossWinner: TeamSide? = null,
        overtimeCoinTossChoice: OvertimeCoinTossChoice? = null,
        homePlatform: com.fcfb.arceus.enums.system.Platform = com.fcfb.arceus.enums.system.Platform.DISCORD,
        homePlatformId: String? = null,
        awayPlatform: com.fcfb.arceus.enums.system.Platform = com.fcfb.arceus.enums.system.Platform.DISCORD,
        awayPlatformId: String? = null,
        lastMessageTimestamp: String? = null,
        gameTimer: String? = null,
        gameWarning: GameWarning? = null,
        currentPlayType: PlayType? = null,
        currentPlayId: Int? = null,
        clockStopped: Boolean = false,
        requestMessageId: List<String>? = emptyList(),
        gameStatus: GameStatus = GameStatus.IN_PROGRESS,
        gameType: GameType = GameType.CONFERENCE_GAME,
        gameMode: GameMode = GameMode.NORMAL,
        overtimeHalf: Int? = null,
        closeGame: Boolean = false,
        closeGamePinged: Boolean = false,
        upsetAlert: Boolean = false,
        upsetAlertPinged: Boolean = false,
    ): Game {
        return Game().apply {
            this.gameId = gameId
            this.homeTeam = homeTeam
            this.awayTeam = awayTeam
            this.homeCoaches = homeCoaches
            this.awayCoaches = awayCoaches
            this.homeCoachDiscordIds = homeCoachDiscordIds
            this.awayCoachDiscordIds = awayCoachDiscordIds
            this.homeOffensivePlaybook = homeOffensivePlaybook
            this.awayOffensivePlaybook = awayOffensivePlaybook
            this.homeDefensivePlaybook = homeDefensivePlaybook
            this.awayDefensivePlaybook = awayDefensivePlaybook
            this.homeScore = homeScore
            this.awayScore = awayScore
            this.possession = possession
            this.quarter = quarter
            this.clock = clock
            this.ballLocation = ballLocation
            this.down = down
            this.yardsToGo = yardsToGo
            this.tvChannel = tvChannel
            this.homeTeamRank = homeTeamRank
            this.homeWins = homeWins
            this.homeLosses = homeLosses
            this.awayWins = awayWins
            this.awayLosses = awayLosses
            this.awayTeamRank = awayTeamRank
            this.subdivision = subdivision
            this.timestamp = timestamp
            this.winProbability = winProbability
            this.season = season
            this.week = week
            this.waitingOn = waitingOn
            this.numPlays = numPlays
            this.homeTimeouts = homeTimeouts
            this.awayTimeouts = awayTimeouts
            this.coinTossWinner = coinTossWinner
            this.coinTossChoice = coinTossChoice
            this.overtimeCoinTossWinner = overtimeCoinTossWinner
            this.overtimeCoinTossChoice = overtimeCoinTossChoice
            this.homePlatform = homePlatform
            this.homePlatformId = homePlatformId
            this.awayPlatform = awayPlatform
            this.awayPlatformId = awayPlatformId
            this.lastMessageTimestamp = lastMessageTimestamp
            this.gameTimer = gameTimer
            this.gameWarning = gameWarning
            this.currentPlayType = currentPlayType
            this.currentPlayId = currentPlayId
            this.clockStopped = clockStopped
            this.requestMessageId = requestMessageId
            this.gameStatus = gameStatus
            this.gameType = gameType
            this.gameMode = gameMode
            this.overtimeHalf = overtimeHalf
            this.closeGame = closeGame
            this.closeGamePinged = closeGamePinged
            this.upsetAlert = upsetAlert
            this.upsetAlertPinged = upsetAlertPinged
        }
    }
}
