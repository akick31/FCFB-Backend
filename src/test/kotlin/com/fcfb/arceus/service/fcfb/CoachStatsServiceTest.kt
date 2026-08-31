package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.service.log.UsernameHistoryService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CoachStatsServiceTest {
    private val gameStatsRepository: GameStatsRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private val teamRepository: TeamRepository = mockk()
    private val coachTransactionLogRepository: CoachTransactionLogRepository = mockk()
    private val seasonStatsService: SeasonStatsService = mockk()
    private val userRepository: UserRepository = mockk()
    private val usernameHistoryService: UsernameHistoryService = mockk()
    private lateinit var coachStatsService: CoachStatsService

    private val discordId = "111222333"

    private val currentUser =
        User().apply {
            id = 1L
            username = "cyclone_puffin"
            discordId = this@CoachStatsServiceTest.discordId
        }

    @BeforeEach
    fun setup() {
        coachStatsService =
            CoachStatsService(
                gameStatsRepository,
                gameRepository,
                teamRepository,
                coachTransactionLogRepository,
                seasonStatsService,
                userRepository,
                usernameHistoryService,
            )
        every { userRepository.findByUsername("cyclone_puffin") } returns currentUser
        every { usernameHistoryService.getHistoricalUsernames(1L) } returns emptyList()
        every { teamRepository.findAll() } returns emptyList()
    }

    @Test
    fun `getCoachStats finds a stint logged under a since-renamed username via discord id`() {
        // The transaction log entry was written back when this coach's username was
        // "flying_porygon" - the current lookup is by "cyclone_puffin", which never
        // appears in the log's `coach` field, only in `coachDiscordIds`.
        val hiredEntry =
            CoachTransactionLog(
                "Arkansas",
                CoachPosition.HEAD_COACH,
                mutableListOf("flying_porygon"),
                TransactionType.HIRED,
                "01/01/2024 00:00:00",
                "admin",
                mutableListOf(discordId),
            )
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns listOf(hiredEntry)

        val game =
            Game().apply {
                gameId = 42
                homeTeam = "Arkansas"
                awayTeam = "LSU"
                gameType = GameType.CONFERENCE_GAME
                timestamp = "2024-06-01 12:00:00"
            }
        every { gameRepository.findByHomeTeam("Arkansas") } returns listOf(game)
        every { gameRepository.findByAwayTeam("Arkansas") } returns emptyList()

        val gameStats = GameStats(gameId = 42, team = "Arkansas", season = 13, week = 1)
        every { gameStatsRepository.findByTeam("Arkansas") } returns listOf(gameStats)
        every { gameStatsRepository.findByGameIdIn(setOf(42)) } returns listOf(gameStats)

        val seasonStats = mockk<SeasonStats>()
        every {
            seasonStatsService.aggregateGameStatsToSeasonStats(any(), "Arkansas", 13, any(), null)
        } returns seasonStats

        val result = coachStatsService.getCoachStats("cyclone_puffin")

        assertEquals(1, result.size)
        assertTrue(result.contains(seasonStats))
    }

    @Test
    fun `getCoachStats attributes a team's entire history when the team has no transaction log entries at all`() {
        // Original team assignment predates the transaction log system entirely - there
        // are no HIRED/FIRED rows for this team, but the coach's current team is still
        // known from the User row itself.
        currentUser.team = "Wyoming"
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns emptyList()

        val game =
            Game().apply {
                gameId = 7
                homeTeam = "Wyoming"
                awayTeam = "Air Force"
                gameType = GameType.CONFERENCE_GAME
                timestamp = "2023-09-01 12:00:00"
            }
        every { gameRepository.findByHomeTeam("Wyoming") } returns listOf(game)
        every { gameRepository.findByAwayTeam("Wyoming") } returns emptyList()

        val gameStats = GameStats(gameId = 7, team = "Wyoming", season = 10, week = 1)
        every { gameStatsRepository.findByTeam("Wyoming") } returns listOf(gameStats)
        every { gameStatsRepository.findByGameIdIn(setOf(7)) } returns listOf(gameStats)

        val seasonStats = mockk<SeasonStats>()
        every {
            seasonStatsService.aggregateGameStatsToSeasonStats(any(), "Wyoming", 10, any(), null)
        } returns seasonStats

        val result = coachStatsService.getCoachStats("cyclone_puffin")

        assertEquals(1, result.size)
        assertTrue(result.contains(seasonStats))
    }

    @Test
    fun `getCoachStats returns empty when neither discord id nor username history match`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(
                CoachTransactionLog(
                    "Arkansas",
                    CoachPosition.HEAD_COACH,
                    mutableListOf("someone_else"),
                    TransactionType.HIRED,
                    "01/01/2024 00:00:00",
                    "admin",
                    mutableListOf("999888777"),
                ),
            )

        val result = coachStatsService.getCoachStats("cyclone_puffin")

        assertTrue(result.isEmpty())
    }
}
