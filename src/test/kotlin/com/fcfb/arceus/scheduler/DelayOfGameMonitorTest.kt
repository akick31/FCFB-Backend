package com.fcfb.arceus.scheduler

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.notification.NotificationType
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.account.User
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.game.NotificationOutbox
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.graphics.ScorebugService
import com.fcfb.arceus.service.fcfb.play.PlayService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DelayOfGameMonitorTest {
    private val gameService = mockk<GameService>(relaxed = true)
    private val gameStatsService = mockk<GameStatsService>(relaxed = true)
    private val userService = mockk<UserService>(relaxed = true)
    private val playService = mockk<PlayService>(relaxed = true)
    private val discordService = mockk<DiscordService>(relaxed = true)
    private val scorebugService = mockk<ScorebugService>(relaxed = true)
    private val playRepository = mockk<PlayRepository>(relaxed = true)
    private val monitor =
        DelayOfGameMonitor(
            gameService,
            mockk(relaxed = true),
            gameStatsService,
            userService,
            playService,
            discordService,
            scorebugService,
            playRepository,
        )

    private val homeCoach = User().apply { delayOfGameInstances = 0 }
    private val awayCoach = User().apply { delayOfGameInstances = 0 }
    private val delayOfGamePlay = Play().apply { playNumber = 1 }

    init {
        every { gameService.findGamesToWarnFirstInstance() } returns emptyList()
        every { gameService.findGamesToWarnSecondInstance() } returns emptyList()
        every { gameService.calculateDelayOfGameTimer() } returns "01/01/2030 00:00:00"
        every { userService.getUserByDiscordId("home-coach") } returns homeCoach
        every { userService.getUserByDiscordId("away-coach") } returns awayCoach
        every { playService.recordPregameDelayOfGame(any(), any()) } returns delayOfGamePlay
        every { playService.getHomeDelayOfGameInstances(any()) } returns 1
        every { playService.getAwayDelayOfGameInstances(any()) } returns 1
        every { gameStatsService.updateGameStats(any(), any()) } returns listOf(GameStats(), GameStats())
    }

    private fun pregameGame(
        waitingOn: TeamSide,
        coinTossWinner: TeamSide?,
    ) = Game().apply {
        gameId = 7
        homeTeam = "UCLA"
        awayTeam = "Western Michigan"
        gameStatus = GameStatus.PREGAME
        gameType = GameType.OUT_OF_CONFERENCE
        gameWarning = GameWarning.SECOND_WARNING
        currentPlayType = PlayType.KICKOFF
        possession = TeamSide.HOME
        this.waitingOn = waitingOn
        this.coinTossWinner = coinTossWinner
        homeCoachDiscordIds = listOf("home-coach")
        awayCoachDiscordIds = listOf("away-coach")
        numPlays = 0
        ballLocation = 35
    }

    @Test
    fun `pregame delay of game before the toss gives the opponent eight points and changes nothing else`() {
        val game = pregameGame(waitingOn = TeamSide.AWAY, coinTossWinner = null)
        every { gameService.findExpiredTimers() } returns listOf(game)

        monitor.checkForDelayOfGame()

        assertEquals(8, game.homeScore)
        assertEquals(0, game.awayScore)
        assertEquals(GameStatus.PREGAME, game.gameStatus)
        assertEquals(TeamSide.AWAY, game.waitingOn)
        assertEquals(TeamSide.HOME, game.possession)
        assertEquals(PlayType.KICKOFF, game.currentPlayType)
        assertNull(game.coinTossWinner)
        assertEquals(35, game.ballLocation)
        assertEquals(GameWarning.NONE, game.gameWarning)
        assertEquals(1, awayCoach.delayOfGameInstances)
        assertEquals(0, homeCoach.delayOfGameInstances)
        verify(exactly = 1) { playService.recordPregameDelayOfGame(game, TeamSide.AWAY) }
    }

    @Test
    fun `pregame delay of game after the toss penalizes the winner and leaves the choice pending`() {
        val game = pregameGame(waitingOn = TeamSide.HOME, coinTossWinner = TeamSide.HOME)
        every { gameService.findExpiredTimers() } returns listOf(game)

        monitor.checkForDelayOfGame()

        assertEquals(0, game.homeScore)
        assertEquals(8, game.awayScore)
        assertEquals(GameStatus.PREGAME, game.gameStatus)
        assertEquals(TeamSide.HOME, game.coinTossWinner)
        assertEquals(TeamSide.HOME, game.waitingOn)
        assertEquals(1, homeCoach.delayOfGameInstances)
        verify(exactly = 1) { playService.recordPregameDelayOfGame(game, TeamSide.HOME) }
    }

    @Test
    fun `pregame delay of game records the play number and never routes through defensive number submission`() {
        val game = pregameGame(waitingOn = TeamSide.AWAY, coinTossWinner = null)
        every { gameService.findExpiredTimers() } returns listOf(game)
        val savedGame = slot<Game>()
        every { gameService.saveGame(capture(savedGame)) } answers { savedGame.captured }

        monitor.checkForDelayOfGame()

        assertEquals(1, savedGame.captured.numPlays)
        verify(exactly = 0) { playService.defensiveNumberSubmitted(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `a failed delay of game notification is persisted for retry instead of being dropped`() {
        val game = pregameGame(waitingOn = TeamSide.AWAY, coinTossWinner = null)
        every { gameService.findExpiredTimers() } returns listOf(game)
        every { discordService.notifyDelayOfGame(any(), any()) } returns false

        monitor.checkForDelayOfGame()

        verify(exactly = 1) { gameService.markPendingDogNotification(game.gameId, false) }
    }

    @Test
    fun `a successful delay of game notification is not persisted for retry`() {
        val game = pregameGame(waitingOn = TeamSide.AWAY, coinTossWinner = null)
        every { gameService.findExpiredTimers() } returns listOf(game)
        every { discordService.notifyDelayOfGame(any(), any()) } returns true

        monitor.checkForDelayOfGame()

        verify(exactly = 0) { gameService.markPendingDogNotification(any(), any()) }
    }

    @Test
    fun `pending delay of game notifications are retried and cleared on success`() {
        val pendingGame = Game().apply { gameId = 42 }
        val pendingNotification = NotificationOutbox(42, NotificationType.DOG_OUT, "true")
        every { gameService.findPendingDogNotifications() } returns listOf(pendingNotification)
        every { gameService.getGameById(42) } returns pendingGame
        every { gameService.findExpiredTimers() } returns emptyList()
        every { discordService.notifyDelayOfGame(pendingGame, true) } returns true

        monitor.checkForDelayOfGame()

        verify(exactly = 1) { discordService.notifyDelayOfGame(pendingGame, true) }
        verify(exactly = 1) { gameService.clearPendingDogNotification(42) }
    }

    @Test
    fun `pending delay of game notifications stay pending when the retry also fails`() {
        val pendingGame = Game().apply { gameId = 43 }
        val pendingNotification = NotificationOutbox(43, NotificationType.DOG_OUT, "false")
        every { gameService.findPendingDogNotifications() } returns listOf(pendingNotification)
        every { gameService.getGameById(43) } returns pendingGame
        every { gameService.findExpiredTimers() } returns emptyList()
        every { discordService.notifyDelayOfGame(pendingGame, false) } returns false

        monitor.checkForDelayOfGame()

        verify(exactly = 0) { gameService.clearPendingDogNotification(any()) }
    }
}
