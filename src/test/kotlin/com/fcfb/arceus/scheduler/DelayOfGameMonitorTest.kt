package com.fcfb.arceus.scheduler

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.fcfb.GameStatsService
import com.fcfb.arceus.service.fcfb.PlayService
import com.fcfb.arceus.service.fcfb.ScorebugService
import com.fcfb.arceus.service.fcfb.UserService
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
        DelayOfGameMonitor(gameService, gameStatsService, userService, playService, discordService, scorebugService, playRepository)

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
}
