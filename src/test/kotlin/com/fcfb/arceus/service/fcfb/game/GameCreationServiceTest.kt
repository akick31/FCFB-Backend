package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.dto.response.VegasOddsResponse
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.system.Platform
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.exception.discord.UnableToCreateGameThreadException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.season.Season
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.appearance.TeamUniformService
import com.fcfb.arceus.service.fcfb.probability.VegasOddsService
import com.fcfb.arceus.service.fcfb.ranking.RankingService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import com.fcfb.arceus.service.fcfb.season.SeasonService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameCreationServiceTest {
    private val gameRepository = mockk<GameRepository>(relaxed = true)
    private val playRepository = mockk<PlayRepository>(relaxed = true)
    private val teamService = mockk<TeamService>(relaxed = true)
    private val discordService = mockk<DiscordService>(relaxed = true)
    private val gameStatsService = mockk<GameStatsService>(relaxed = true)
    private val teamUniformService = mockk<TeamUniformService>(relaxed = true)
    private val seasonService = mockk<SeasonService>(relaxed = true)
    private val scheduleService = mockk<ScheduleService>(relaxed = true)
    private val vegasOddsService = mockk<VegasOddsService>(relaxed = true)
    private val rankingService = mockk<RankingService>()
    private val games =
        GameService(
            gameRepository, playRepository, teamService, discordService, mockk(), gameStatsService,
            mockk(), mockk(), mockk(), mockk(), mockk(), mockk(relaxed = true), mockk(relaxed = true),
        )
    private val creation =
        GameCreationService(
            games, teamService, discordService, gameStatsService, teamUniformService,
            seasonService, scheduleService, vegasOddsService, rankingService, mockk(relaxed = true),
        )
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
            runBlocking { creation.startSingleGame(startRequest, null) }
        }

        verify(exactly = 1) { gameRepository.deleteById(SAVED_GAME_ID) }
        verify(exactly = 1) { gameStatsService.deleteByGameId(SAVED_GAME_ID) }
        verify(exactly = 1) { playRepository.deleteAllPlaysByGameId(SAVED_GAME_ID) }
    }

    @Test
    fun `a bot that answers with the literal string null deletes the game`() {
        coEvery { discordService.createGameThread(any()) } returns listOf("null")

        assertFailsWith<UnableToCreateGameThreadException> {
            runBlocking { creation.startSingleGame(startRequest, null) }
        }

        verify(exactly = 1) { gameRepository.deleteById(SAVED_GAME_ID) }
    }

    @Test
    fun `a thread id with no request message id deletes the game`() {
        coEvery { discordService.createGameThread(any()) } returns listOf("123456789")

        assertFailsWith<UnableToCreateGameThreadException> {
            runBlocking { creation.startSingleGame(startRequest, null) }
        }

        verify(exactly = 1) { gameRepository.deleteById(SAVED_GAME_ID) }
    }

    @Test
    fun `a started game keeps its platform ids and is never deleted`() {
        coEvery { discordService.createGameThread(any()) } returns listOf("123456789", "987654321")

        val game = runBlocking { creation.startSingleGame(startRequest, null) }

        assertEquals("123456789", game.homePlatformId)
        assertEquals("123456789", game.awayPlatformId)
        assertEquals(listOf("987654321"), game.requestMessageId)
        verify(exactly = 0) { gameRepository.deleteById(any()) }
    }

    @ParameterizedTest
    @EnumSource(value = GameType::class, names = ["SCRIMMAGE", "CONFERENCE_GAME"])
    fun `normal creation preserves metadata setup order and manual schedule marking`(type: GameType) =
        runBlocking {
            val request =
                startRequest.copy(
                    gameType = type,
                    postseasonGameLogo = "logo.png",
                    postseasonGameName = "Bowl",
                    neutralSite = true,
                    venue = "Stadium",
                )
            every { seasonService.getCurrentSeason() } returns
                Season().apply {
                    seasonNumber = 12
                    currentWeek = 4
                }
            coEvery { discordService.createGameThread(any()) } returns listOf("thread", "message")
            val game = creation.startSingleGame(request, 8)
            assertEquals(if (type == GameType.SCRIMMAGE) null else 12, game.season)
            assertEquals(8, game.week)
            assertEquals("logo.png", game.postseasonGameLogo)
            assertEquals("Bowl", game.postseasonGameName)
            assertEquals(true, game.neutralSite)
            assertEquals("Stadium", game.venue)
            assertEquals(1, game.quarter)
            assertEquals(3, game.homeTimeouts)
            assertEquals(GameStatus.PREGAME, game.gameStatus)
            coVerifyOrder {
                gameRepository.save(any())
                discordService.createGameThread(any())
                gameRepository.save(any())
                gameStatsService.createGameStats(any())
                teamUniformService.snapshot(any())
            }
            verify(exactly = if (type == GameType.SCRIMMAGE) 0 else 1) { scheduleService.markManuallyStartedGameAsStarted(game) }
        }

    @Test
    fun `creation falls back to the home team's stadium when no venue is given`() =
        runBlocking {
            every { teamService.getTeamByName("Ohio State") } returns
                team(1, "Ohio State", "coachA", "111", stadium = "Ohio Stadium")
            coEvery { discordService.createGameThread(any()) } returns listOf("thread", "message")

            val game = creation.startSingleGame(startRequest, null)

            assertEquals("Ohio Stadium", game.venue)
        }

    @Test
    fun `creation keeps an explicit neutral site venue over the home team's stadium`() =
        runBlocking {
            every { teamService.getTeamByName("Ohio State") } returns
                team(1, "Ohio State", "coachA", "111", stadium = "Ohio Stadium")
            coEvery { discordService.createGameThread(any()) } returns listOf("thread", "message")
            val request = startRequest.copy(neutralSite = true, venue = "Neutral Arena")

            val game = creation.startSingleGame(request, null)

            assertEquals("Neutral Arena", game.venue)
        }

    @Test
    fun `overtime creation retains scrimmage defaults and shared setup order`() =
        runBlocking {
            coEvery { discordService.createGameThread(any()) } returns listOf("thread", "message")
            val game = creation.startOvertimeGame(startRequest.copy(gameType = GameType.CONFERENCE_GAME))
            assertEquals(GameType.SCRIMMAGE, game.gameType)
            assertEquals(GameStatus.END_OF_REGULATION, game.gameStatus)
            assertEquals(null, game.season)
            assertEquals(null, game.week)
            assertEquals(5, game.quarter)
            assertEquals("0:00", game.clock)
            assertEquals(75, game.ballLocation)
            assertEquals(1, game.homeTimeouts)
            assertEquals(1, game.overtimeHalf)
            assertEquals(PlayType.NORMAL, game.currentPlayType)
            coVerifyOrder {
                gameRepository.save(any())
                discordService.createGameThread(any())
                gameRepository.save(any())
                gameStatsService.createGameStats(any())
                teamUniformService.snapshot(any())
            }
            verify(exactly = 0) { scheduleService.markManuallyStartedGameAsStarted(any()) }
            verify(exactly = 0) { vegasOddsService.calculateVegasOdds(any(), any()) }
        }

    @ParameterizedTest
    @ValueSource(strings = ["123", "18446744073709551615"])
    fun `restart deletes previous game before creation and leaves manual schedule marking alone`(channelId: String) =
        runBlocking {
            val previous =
                Game().apply {
                    gameId = SAVED_GAME_ID
                    homeTeam = "Ohio State"
                    awayTeam = "Michigan"
                    gameType = GameType.CONFERENCE_GAME
                    gameStatus = GameStatus.IN_PROGRESS
                    week = 8
                }
            every { gameRepository.getGameByPlatformId(channelId.toULong()) } returns previous
            every { seasonService.getCurrentSeason() } returns
                Season().apply {
                    seasonNumber = 12
                    currentWeek = 4
                }
            coEvery { discordService.createGameThread(any()) } returns listOf("thread", "message")
            val game = creation.restartGame(channelId)
            assertEquals(8, game.week)
            coVerifyOrder {
                gameRepository.deleteById(previous.gameId)
                gameStatsService.deleteByGameId(previous.gameId)
                playRepository.deleteAllPlaysByGameId(previous.gameId)
                gameRepository.save(any())
                discordService.createGameThread(any())
            }
            verify(exactly = 0) { scheduleService.markManuallyStartedGameAsStarted(any()) }
        }

    private fun team(
        teamId: Int,
        teamName: String,
        coach: String,
        discordId: String,
        stadium: String? = null,
    ) = Team().apply {
        id = teamId
        name = teamName
        coachUsernames = mutableListOf(coach)
        coachNames = mutableListOf(coach)
        coachDiscordTags = mutableListOf(coach)
        coachDiscordIds = mutableListOf(discordId)
        offensivePlaybook = OffensivePlaybook.AIR_RAID
        defensivePlaybook = DefensivePlaybook.FOUR_THREE
        this.stadium = stadium
    }

    companion object {
        private const val SAVED_GAME_ID = 2211
    }
}
