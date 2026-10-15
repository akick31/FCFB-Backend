package com.fcfb.arceus.service.fcfb.statistics

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.account.User
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.log.CoachTransactionLog
import com.fcfb.arceus.model.statistics.CoachSeasonStats
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.repositories.account.UserRepository
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.log.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.statistics.CoachSeasonStatsRepository
import com.fcfb.arceus.repositories.statistics.GameStatsRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.coach.CoachStintService
import com.fcfb.arceus.service.log.UsernameHistoryService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CoachStatsServiceTest {
    private val gameStatsRepository: GameStatsRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private val teamRepository: TeamRepository = mockk()
    private val coachTransactionLogRepository: CoachTransactionLogRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val usernameHistoryService: UsernameHistoryService = mockk()
    private val coachSeasonStatsRepository: CoachSeasonStatsRepository = mockk()
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
        val coachStintService =
            CoachStintService(
                coachTransactionLogRepository,
                teamRepository,
                usernameHistoryService,
            )
        coachStatsService =
            CoachStatsService(
                gameStatsRepository,
                gameRepository,
                teamRepository,
                userRepository,
                coachStintService,
                coachSeasonStatsRepository,
            )
        every { userRepository.findByUsername("cyclone_puffin") } returns currentUser
        every { usernameHistoryService.getHistoricalUsernames(1L) } returns emptyList()
        every { teamRepository.findAll() } returns emptyList()
        every { coachSeasonStatsRepository.findByUserId(1L) } returns emptyList()
        every { coachSeasonStatsRepository.findByUserIdAndTeamAndSeasonNumber(any(), any(), any()) } returns null
        every { coachSeasonStatsRepository.save(any()) } answers { firstArg() }
        every { coachSeasonStatsRepository.deleteAll(any<List<CoachSeasonStats>>()) } returns Unit
    }

    @Test
    fun `regenerateForCoach persists a stint logged under a since-renamed username via discord id`() {
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

        coachStatsService.regenerateForCoach("cyclone_puffin")

        verify {
            coachSeasonStatsRepository.save(
                match { it.userId == 1L && it.team == "Arkansas" && it.seasonNumber == 13 },
            )
        }
    }

    @Test
    fun `regenerateForCoach attributes a team's entire history when the team has no transaction log entries at all`() {
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

        coachStatsService.regenerateForCoach("cyclone_puffin")

        verify {
            coachSeasonStatsRepository.save(
                match { it.userId == 1L && it.team == "Wyoming" && it.seasonNumber == 10 },
            )
        }
    }

    @Test
    fun `regenerateForCoach persists nothing when neither discord id nor username history match`() {
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

        coachStatsService.regenerateForCoach("cyclone_puffin")

        verify(exactly = 0) { coachSeasonStatsRepository.save(any()) }
    }

    @Test
    fun `regenerateForCoach deletes a stale row for a team the coach no longer qualifies for`() {
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

        val staleRow = CoachSeasonStats(userId = 1L, team = "Air Force", seasonNumber = 9)
        every { coachSeasonStatsRepository.findByUserId(1L) } returns listOf(staleRow)

        coachStatsService.regenerateForCoach("cyclone_puffin")

        verify { coachSeasonStatsRepository.deleteAll(listOf(staleRow)) }
    }

    @Test
    fun `getCoachStats reads persisted rows for the resolved user`() {
        val persisted = listOf(CoachSeasonStats(userId = 1L, team = "Arkansas", seasonNumber = 13))
        every { coachSeasonStatsRepository.findByUserIdOrderBySeasonNumberDescTeamAsc(1L) } returns persisted

        val result = coachStatsService.getCoachStats("cyclone_puffin")

        assertEquals(1, result.size)
        assertTrue(result.contains(persisted.first()))
    }

    @Test
    fun `getCoachStats returns empty when the coach does not resolve to a user`() {
        every { userRepository.findByUsername("unknown_coach") } returns null

        val result = coachStatsService.getCoachStats("unknown_coach")

        assertTrue(result.isEmpty())
    }
}
