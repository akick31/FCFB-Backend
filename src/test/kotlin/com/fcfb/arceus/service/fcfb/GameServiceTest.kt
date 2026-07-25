package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.dto.response.VegasOddsResponse
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.system.Platform
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.PlayRepository
import com.fcfb.arceus.repositories.RankingRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.specification.GameSpecificationService
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
    private val winProbabilityService: WinProbabilityService = mockk(relaxed = true)
    private val vegasOddsService: VegasOddsService = mockk(relaxed = true)
    private val gameStatsRepository: GameStatsRepository = mockk(relaxed = true)
    private val rankingRepository: RankingRepository = mockk(relaxed = true)

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
                winProbabilityService,
                vegasOddsService,
                gameStatsRepository,
                rankingRepository,
            )

        every { teamService.getTeamByName("Ohio State") } returns team(1, "Ohio State", "coachA", "111")
        every { teamService.getTeamByName("Michigan") } returns team(2, "Michigan", "coachB", "222")
        every { teamService.getTeamRanks(1, 2) } returns (null to null)
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
