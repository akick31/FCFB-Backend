package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.dto.response.VegasOddsResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.system.Platform
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.repositories.RankingRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.elo.EloService
import com.fcfb.arceus.service.specification.GameSpecificationService
import com.fcfb.arceus.util.InvalidGameReopenException
import com.fcfb.arceus.util.UnableToCreateGameThreadException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameServiceTest {
    private val gameRepository: GameRepository = mockk(relaxed = true)
    private val playRepository: PlayRepository = mockk(relaxed = true)
    private val teamService: TeamService = mockk(relaxed = true)
    private val discordService: DiscordService = mockk(relaxed = true)
    private val userService: UserService = mockk(relaxed = true)
    private val gameStatsService: GameStatsService = mockk(relaxed = true)
    private val seasonService: SeasonService = mockk(relaxed = true)
    private val scheduleService: ScheduleService = mockk(relaxed = true)
    private val gameSpecificationService: GameSpecificationService = mockk(relaxed = true)
    private val recordService: RecordService = mockk(relaxed = true)
    private val seasonStatsService: SeasonStatsService = mockk(relaxed = true)
    private val postseasonSeasonStatsService: PostseasonSeasonStatsService = mockk(relaxed = true)
    private val winProbabilityService: WinProbabilityService = mockk(relaxed = true)
    private val vegasOddsService: VegasOddsService = mockk(relaxed = true)
    private val gameStatsRepository: GameStatsRepository = mockk(relaxed = true)
    private val rankingRepository: RankingRepository = mockk(relaxed = true)
    private val rankingService: RankingService = mockk()
    private val eloService: EloService = mockk(relaxed = true)

    private lateinit var gameService: GameService

    private val startRequest =
        StartRequest(
            homePlatform = Platform.DISCORD,
            awayPlatform = Platform.DISCORD,
            subdivision = Subdivision.FBS,
            homeTeam = "Ohio State",
            awayTeam = "Michigan",
            tvChannel = null,
            gameType = GameType.SCRIMMAGE,
        )

    @BeforeEach
    fun setup() {
        gameService =
            GameService(
                gameRepository,
                playRepository,
                teamService,
                discordService,
                userService,
                gameStatsService,
                seasonService,
                scheduleService,
                gameSpecificationService,
                recordService,
                seasonStatsService,
                postseasonSeasonStatsService,
                winProbabilityService,
                vegasOddsService,
                gameStatsRepository,
                rankingRepository,
                rankingService,
                eloService,
            )

        every { teamService.getTeamByName("Ohio State") } returns team(1, "Ohio State", "coachA", "111")
        every { teamService.getTeamByName("Michigan") } returns team(2, "Michigan", "coachB", "222")
        every { rankingService.getTeamRanks(any(), any(), 1, 2) } returns (null to null)
        every { vegasOddsService.calculateVegasOdds(any(), any()) } returns
            VegasOddsResponse("Ohio State", "Michigan", -3.5, 3.5, 1600.0, 1500.0)
        every { gameRepository.save(any()) } answers { firstArg<Game>().apply { gameId = SAVED_GAME_ID } }
    }

    @Test
    fun `a game whose Discord thread cannot be created is deleted rather than left orphaned`() {
        coEvery { discordService.createGameThread(any()) } returns null

        assertFailsWith<UnableToCreateGameThreadException> {
            runBlocking { gameService.startSingleGame(startRequest, null) }
        }

        verify(exactly = 1) { gameRepository.deleteById(SAVED_GAME_ID) }
        verify(exactly = 1) { gameStatsService.deleteByGameId(SAVED_GAME_ID) }
        verify(exactly = 1) { playRepository.deleteAllPlaysByGameId(SAVED_GAME_ID) }
    }

    @Test
    fun `a bot that answers with the literal string null deletes the game`() {
        coEvery { discordService.createGameThread(any()) } returns listOf("null")

        assertFailsWith<UnableToCreateGameThreadException> {
            runBlocking { gameService.startSingleGame(startRequest, null) }
        }

        verify(exactly = 1) { gameRepository.deleteById(SAVED_GAME_ID) }
    }

    @Test
    fun `a thread id with no request message id deletes the game`() {
        coEvery { discordService.createGameThread(any()) } returns listOf("123456789")

        assertFailsWith<UnableToCreateGameThreadException> {
            runBlocking { gameService.startSingleGame(startRequest, null) }
        }

        verify(exactly = 1) { gameRepository.deleteById(SAVED_GAME_ID) }
    }

    @Test
    fun `a started game keeps its platform ids and is never deleted`() {
        coEvery { discordService.createGameThread(any()) } returns listOf("123456789", "987654321")

        val game = runBlocking { gameService.startSingleGame(startRequest, null) }

        assertEquals("123456789", game.homePlatformId)
        assertEquals("123456789", game.awayPlatformId)
        assertEquals(listOf("987654321"), game.requestMessageId)
        verify(exactly = 0) { gameRepository.deleteById(any()) }
    }

    @Test
    fun `a game that reaches FINAL through live play still credits team wins and losses`() {
        every { gameStatsRepository.getGameStatsByIdAndTeam(any(), any()) } returns GameStats()
        every { playRepository.getPreviousPlay(any()) } returns null

        val game =
            Game().apply {
                gameId = SAVED_GAME_ID
                homeTeam = "Ohio State"
                awayTeam = "Michigan"
                gameType = GameType.OUT_OF_CONFERENCE
                gameStatus = GameStatus.IN_PROGRESS
                homeCoachDiscordIds = listOf()
                awayCoachDiscordIds = listOf()
            }
        val play =
            Play().apply {
                homeTeam = "Ohio State"
                awayTeam = "Michigan"
            }

        gameService.updateGameInformation(
            game = game,
            play = play,
            homeScore = 24,
            awayScore = 17,
            possession = TeamSide.HOME,
            quarter = 0,
            clock = 0,
            ballLocation = 0,
            down = 1,
            yardsToGo = 10,
            homeTimeoutCalled = false,
            awayTimeoutCalled = false,
            timeoutUsed = false,
        )

        verify(exactly = 1) { teamService.updateTeamWinsAndLosses(game) }
    }

    @Test
    fun `reopening a game that ended on a final touchdown restores the scorer's pending conversion`() {
        val game = finalGame(homeScore = 24, awayScore = 22)
        val touchdown =
            Play().apply {
                homeTeam = "Ohio State"
                awayTeam = "Michigan"
                possession = TeamSide.AWAY
                actualResult = ActualResult.TOUCHDOWN
                playId = TOUCHDOWN_PLAY_ID
                playFinished = true
                homeScore = 22
                awayScore = 24
            }
        val endOfGame =
            Play().apply {
                playId = END_OF_GAME_PLAY_ID
                playFinished = true
                homeTeam = "Ohio State"
                awayTeam = "Michigan"
                actualResult = ActualResult.END_OF_GAME
            }
        every { gameRepository.getGameById(SAVED_GAME_ID) } returns game
        every { playRepository.getAllPlaysByGameId(SAVED_GAME_ID) } returns listOf(touchdown, endOfGame)
        every { playRepository.getPreviousPlay(SAVED_GAME_ID) } returns endOfGame

        val reopened = gameService.reopenGame(SAVED_GAME_ID)

        assertEquals(GameStatus.IN_PROGRESS, reopened.gameStatus)
        assertEquals(TeamSide.AWAY, reopened.possession)
        assertEquals(TeamSide.HOME, reopened.waitingOn)
        assertEquals(PlayType.PAT, reopened.currentPlayType)
        assertEquals(22, reopened.homeScore)
        assertEquals(24, reopened.awayScore)
        verify(exactly = 1) { playRepository.deleteById(END_OF_GAME_PLAY_ID) }
        verify(exactly = 1) { teamService.revertTeamWinsAndLosses(game) }
        verify(exactly = 1) { userService.revertUserWinsAndLosses(game) }
        verify(exactly = 1) { scheduleService.markGameAsUnfinished(game) }
        verify(exactly = 1) { eloService.rebuild(true) }
    }

    @Test
    fun `a game whose last real play was not a touchdown cannot be reopened and nothing is reverted`() {
        val game = finalGame(homeScore = 24, awayScore = 22)
        val punt =
            Play().apply {
                homeTeam = "Ohio State"
                awayTeam = "Michigan"
                playId = TOUCHDOWN_PLAY_ID
                playFinished = true
                actualResult = ActualResult.PUNT
            }
        val endOfGame =
            Play().apply {
                playId = END_OF_GAME_PLAY_ID
                homeTeam = "Ohio State"
                awayTeam = "Michigan"
                playFinished = true
                actualResult = ActualResult.END_OF_GAME
            }
        every { gameRepository.getGameById(SAVED_GAME_ID) } returns game
        every { playRepository.getAllPlaysByGameId(SAVED_GAME_ID) } returns listOf(punt, endOfGame)

        assertFailsWith<InvalidGameReopenException> { gameService.reopenGame(SAVED_GAME_ID) }

        verify(exactly = 0) { teamService.revertTeamWinsAndLosses(any()) }
        verify(exactly = 0) { playRepository.deleteById(any()) }
        assertEquals(GameStatus.FINAL, game.gameStatus)
    }

    @Test
    fun `a game that is not final cannot be reopened`() {
        val game = finalGame(homeScore = 24, awayScore = 22).apply { gameStatus = GameStatus.IN_PROGRESS }
        every { gameRepository.getGameById(SAVED_GAME_ID) } returns game

        assertFailsWith<InvalidGameReopenException> { gameService.reopenGame(SAVED_GAME_ID) }
    }

    @Test
    fun `a national championship cannot be reopened`() {
        val game = finalGame(homeScore = 24, awayScore = 22).apply { gameType = GameType.NATIONAL_CHAMPIONSHIP }
        every { gameRepository.getGameById(SAVED_GAME_ID) } returns game

        assertFailsWith<InvalidGameReopenException> { gameService.reopenGame(SAVED_GAME_ID) }
    }

    private fun finalGame(
        homeScore: Int,
        awayScore: Int,
    ) = Game().apply {
        gameId = SAVED_GAME_ID
        homeTeam = "Ohio State"
        awayTeam = "Michigan"
        gameType = GameType.OUT_OF_CONFERENCE
        gameStatus = GameStatus.FINAL
        quarter = 4
        this.homeScore = homeScore
        this.awayScore = awayScore
        possession = TeamSide.AWAY
    }

    private fun team(
        teamId: Int,
        teamName: String,
        coach: String,
        discordId: String,
    ) = Team().apply {
        id = teamId
        name = teamName
        coachUsernames = mutableListOf(coach)
        coachNames = mutableListOf(coach)
        coachDiscordTags = mutableListOf(coach)
        coachDiscordIds = mutableListOf(discordId)
        offensivePlaybook = OffensivePlaybook.AIR_RAID
        defensivePlaybook = DefensivePlaybook.FOUR_THREE
    }

    companion object {
        private const val SAVED_GAME_ID = 2211
        private const val END_OF_GAME_PLAY_ID = 9002
        private const val TOUCHDOWN_PLAY_ID = 9001
    }
}
