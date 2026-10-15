package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.system.Platform
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.account.User
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.game.PlatformTransport
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.game.NotificationOutboxRepository
import com.fcfb.arceus.repositories.game.PlatformTransportRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.appearance.GameFieldService
import com.fcfb.arceus.service.fcfb.probability.WinProbabilityService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import com.fcfb.arceus.service.fcfb.season.SeasonService
import com.fcfb.arceus.service.fcfb.statistics.CoachStatsService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.statistics.record.RecordService
import com.fcfb.arceus.service.fcfb.statistics.season.PostseasonSeasonStatsService
import com.fcfb.arceus.service.fcfb.statistics.season.SeasonStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.service.specification.GameSpecificationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.util.UUID
import kotlin.test.assertEquals

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
    private val coachStatsService: CoachStatsService = mockk(relaxed = true)
    private val winProbabilityService: WinProbabilityService = mockk(relaxed = true)
    private val gameStatsRepository: GameStatsRepository = mockk(relaxed = true)
    private val gameFieldService: GameFieldService = mockk(relaxed = true)
    private val notificationOutboxRepository: NotificationOutboxRepository = mockk(relaxed = true)
    private val gameSplitTableSyncService: GameSplitTableSyncService = mockk(relaxed = true)
    private val platformTransportRepository: PlatformTransportRepository = mockk(relaxed = true)

    private lateinit var gameService: GameService
    private lateinit var finalization: GameFinalizationService

    @BeforeEach
    fun setup() {
        finalization =
            GameFinalizationService(
                gameRepository,
                playRepository,
                teamService,
                discordService,
                userService,
                gameStatsService,
                seasonService,
                scheduleService,
                recordService,
                seasonStatsService,
                postseasonSeasonStatsService,
                coachStatsService,
                winProbabilityService,
                gameFieldService,
                DataSourceTransactionManager(DriverManagerDataSource("jdbc:h2:mem:${UUID.randomUUID()}", "sa", "")),
            )
        gameService =
            GameService(
                gameRepository,
                playRepository,
                teamService,
                discordService,
                userService,
                gameStatsService,
                gameSpecificationService,
                winProbabilityService,
                gameStatsRepository,
                finalization,
                notificationOutboxRepository,
                gameSplitTableSyncService,
                platformTransportRepository,
            )

        every { teamService.getTeamByName("Ohio State") } returns team(1, "Ohio State", "coachA", "111")
        every { teamService.getTeamByName("Michigan") } returns team(2, "Michigan", "coachB", "222")
        every { gameRepository.save(any()) } answers { firstArg<Game>().apply { gameId = SAVED_GAME_ID } }
    }

    @ParameterizedTest
    @EnumSource(value = GameStatus::class, names = ["IN_PROGRESS", "OVERTIME"])
    fun `first play completion updates records Elo and schedule`(status: GameStatus) {
        val game =
            finalizationGame(status).apply {
                homeCoachDiscordIds = listOf("111")
                awayCoachDiscordIds = listOf("222")
            }
        every { userService.getUserByDiscordId("111") } returns
            User().apply {
                id = 1L
                username = "coachA"
                discordId = "111"
            }
        every { userService.getUserByDiscordId("222") } returns
            User().apply {
                id = 2L
                username = "coachB"
                discordId = "222"
            }
        every { playRepository.getUserAverageResponseTime("111", any()) } returns 70.0
        every { playRepository.getUserAverageResponseTime("222", any()) } returns 80.0
        every { playRepository.getPreviousPlay(SAVED_GAME_ID) } returns null
        val play = Play().apply { actualResult = ActualResult.TURNOVER_TOUCHDOWN }

        finishPlay(game, play, if (status == GameStatus.OVERTIME) 5 else 0)

        assertEquals(GameStatus.FINAL, game.gameStatus)
        verify(exactly = 1) { teamService.updateTeamWinsAndLosses(any()) }
        verify(exactly = 1) { userService.updateUserWinsAndLosses(any()) }
        verify(exactly = 1) { winProbabilityService.updateEloRatings(any(), any(), any()) }
        verify(exactly = 1) { scheduleService.markGameAsFinished(any()) }
        verify(exactly = 2) { seasonStatsService.updateSeasonStatsForGame(any()) }
        verify(exactly = 1) { userService.updateUserAverageResponseTime(1L, 70.0) }
        verify(exactly = 1) { userService.updateUserAverageResponseTime(2L, 80.0) }
    }

    @Test
    fun `play completion of an already final game does not repeat records`() {
        every { playRepository.getPreviousPlay(SAVED_GAME_ID) } returns null
        finishPlay(finalizationGame(GameStatus.FINAL))

        verify(exactly = 0) { teamService.updateTeamWinsAndLosses(any()) }
        verify(exactly = 0) { userService.updateUserWinsAndLosses(any()) }
        verify(exactly = 0) { winProbabilityService.updateEloRatings(any(), any(), any()) }
        verify(exactly = 0) { scheduleService.markGameAsFinished(any()) }
    }

    @Test
    fun `ongoing play does not trigger finalization`() {
        val game = finalizationGame(GameStatus.IN_PROGRESS)
        finishPlay(game, quarter = 4)

        assertEquals(GameStatus.IN_PROGRESS, game.gameStatus)
        verify(exactly = 0) { teamService.updateTeamWinsAndLosses(any()) }
        verify(exactly = 0) { scheduleService.markGameAsFinished(any()) }
        verify(exactly = 0) { seasonStatsService.updateSeasonStatsForGame(any()) }
    }

    @Test
    fun `scrimmage play completion does not update records or schedule`() {
        every { playRepository.getPreviousPlay(SAVED_GAME_ID) } returns null
        val game = finalizationGame(GameStatus.IN_PROGRESS).apply { gameType = GameType.SCRIMMAGE }
        finishPlay(game)

        assertEquals(GameStatus.FINAL, game.gameStatus)
        verify(exactly = 0) { teamService.updateTeamWinsAndLosses(any()) }
        verify(exactly = 0) { userService.updateUserWinsAndLosses(any()) }
        verify(exactly = 0) { scheduleService.markGameAsFinished(any()) }
        verify(exactly = 0) { seasonStatsService.updateSeasonStatsForGame(any()) }
    }

    @Test
    fun `repeating manual completion does not repeat record updates`() {
        var persistedStatus: GameStatus? = GameStatus.IN_PROGRESS
        every { gameRepository.getGameById(SAVED_GAME_ID) } answers { finalizationGame(persistedStatus) }
        every { gameRepository.save(any()) } answers { firstArg<Game>().also { persistedStatus = it.gameStatus } }
        every { playRepository.getPreviousPlay(SAVED_GAME_ID) } returns null

        finalization.endSingleGameByGameId(SAVED_GAME_ID)
        finalization.endSingleGameByGameId(SAVED_GAME_ID)

        assertEquals(GameStatus.FINAL, persistedStatus)
        verify(exactly = 1) { teamService.updateTeamWinsAndLosses(any()) }
        verify(exactly = 1) { userService.updateUserWinsAndLosses(any()) }
        verify(exactly = 1) { winProbabilityService.updateEloRatings(any(), any(), any()) }
        verify(exactly = 1) { scheduleService.markGameAsFinished(any()) }
    }

    @Test
    fun `updating the request message id syncs platform transport`() {
        val game = platformGame()
        every { gameRepository.getGameById(SAVED_GAME_ID) } returns game
        every { gameRepository.save(any()) } answers { firstArg() }
        val captured = slot<PlatformTransport>()
        every { platformTransportRepository.save(capture(captured)) } answers { firstArg() }

        gameService.updateRequestMessageId(SAVED_GAME_ID, "999")

        assertEquals(SAVED_GAME_ID, captured.captured.gameId)
        assertEquals(listOf("999"), captured.captured.requestMessageId)
    }

    @Test
    fun `updating the last message timestamp syncs platform transport`() {
        val game = platformGame()
        every { gameRepository.getGameById(SAVED_GAME_ID) } returns game
        every { gameRepository.save(any()) } answers { firstArg() }
        val captured = slot<PlatformTransport>()
        every { platformTransportRepository.save(capture(captured)) } answers { firstArg() }

        gameService.updateLastMessageTimestamp(SAVED_GAME_ID)

        assertEquals(SAVED_GAME_ID, captured.captured.gameId)
        assertEquals(game.lastMessageTimestamp, captured.captured.lastMessageTimestamp)
    }

    private fun platformGame() =
        Game().apply {
            gameId = SAVED_GAME_ID
            homePlatform = Platform.DISCORD
            homePlatformId = "111"
            awayPlatform = Platform.DISCORD
            awayPlatformId = "222"
        }

    private fun finishPlay(
        game: Game,
        play: Play = Play(),
        quarter: Int = 0,
    ) = gameService.updateGameInformation(
        game = game,
        play = play,
        homeScore = 21,
        awayScore = 14,
        possession = TeamSide.HOME,
        quarter = quarter,
        clock = 0,
        ballLocation = 50,
        down = 1,
        yardsToGo = 10,
        homeTimeoutCalled = false,
        awayTimeoutCalled = false,
        timeoutUsed = false,
    )

    private fun finalizationGame(status: GameStatus?) =
        Game().apply {
            gameId = SAVED_GAME_ID
            homeTeam = "Ohio State"
            awayTeam = "Michigan"
            homeScore = 21
            awayScore = 14
            gameStatus = status
            gameType = GameType.CONFERENCE_GAME
            week = 1
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
    }
}
