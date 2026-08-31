package com.fcfb.arceus.service.log

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.repositories.UsernameHistoryRepository
import com.fcfb.arceus.util.UserForbiddenException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class CoachTransactionLogServiceTest {
    private val coachTransactionLogRepository: CoachTransactionLogRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val usernameHistoryRepository: UsernameHistoryRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private lateinit var service: CoachTransactionLogService

    @BeforeEach
    fun setup() {
        service = CoachTransactionLogService(coachTransactionLogRepository, userRepository, usernameHistoryRepository, gameRepository)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("1", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        every { coachTransactionLogRepository.save(any()) } answers { firstArg() }
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun game(
        homeTeam: String,
        awayTeam: String,
        homeCoaches: List<String>,
        homeCoachDiscordIds: List<String>,
        timestamp: String,
    ) = Game().apply {
        this.homeTeam = homeTeam
        this.awayTeam = awayTeam
        this.homeCoaches = homeCoaches
        this.homeCoachDiscordIds = homeCoachDiscordIds
        this.awayCoaches = emptyList()
        this.awayCoachDiscordIds = emptyList()
        this.gameType = GameType.CONFERENCE_GAME
        this.timestamp = timestamp
    }

    @Test
    fun `backfillPreLogHires requires admin`() {
        SecurityContextHolder.clearContext()
        assertThrows(UserForbiddenException::class.java) { service.backfillPreLogHires(10) }
    }

    @Test
    fun `backfillPreLogHires creates a HIRED entry from the team's earliest tracked game when no log entry exists`() {
        val earliest = game("Wyoming", "Air Force", listOf("cyclone_puffin"), listOf("111222333"), "2023-09-01 12:00:00")
        val later = game("Wyoming", "Boise State", listOf("cyclone_puffin"), listOf("111222333"), "2023-09-08 12:00:00")
        every { gameRepository.findBySeason(10) } returns listOf(later, earliest)
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns emptyList()

        val logged = slot<CoachTransactionLog>()
        every { coachTransactionLogRepository.save(capture(logged)) } answers { firstArg() }

        val result = service.backfillPreLogHires(10)

        assertEquals(1, result)
        assertEquals("Wyoming", logged.captured.team)
        assertEquals(TransactionType.HIRED, logged.captured.transaction)
        assertEquals(CoachPosition.HEAD_COACH, logged.captured.position)
        assertEquals(mutableListOf("cyclone_puffin"), logged.captured.coach)
        assertEquals(mutableListOf("111222333"), logged.captured.coachDiscordIds)
        assertEquals("09/01/2023 12:00:00", logged.captured.transactionDate)
    }

    @Test
    fun `backfillPreLogHires skips a team whose log already has an entry at or before the earliest game`() {
        val earliest = game("Wyoming", "Air Force", listOf("cyclone_puffin"), listOf("111222333"), "2023-09-01 12:00:00")
        every { gameRepository.findBySeason(10) } returns listOf(earliest)
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(
                CoachTransactionLog(
                    "Wyoming",
                    CoachPosition.HEAD_COACH,
                    mutableListOf("cyclone_puffin"),
                    TransactionType.HIRED,
                    "08/15/2023 09:00:00",
                    "admin",
                    mutableListOf("111222333"),
                ),
            )

        val result = service.backfillPreLogHires(10)

        assertEquals(0, result)
    }

    @Test
    fun `backfillPreLogHires still backfills a team whose only log entry postdates the earliest game`() {
        // A later coaching change was properly logged, but the original pre-log coach never was.
        val earliest = game("Wyoming", "Air Force", listOf("cyclone_puffin"), listOf("111222333"), "2023-09-01 12:00:00")
        every { gameRepository.findBySeason(10) } returns listOf(earliest)
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(
                CoachTransactionLog(
                    "Wyoming",
                    CoachPosition.HEAD_COACH,
                    mutableListOf("someone_else"),
                    TransactionType.FIRED,
                    "10/01/2024 09:00:00",
                    "admin",
                    mutableListOf("999888777"),
                ),
            )

        val logged = slot<CoachTransactionLog>()
        every { coachTransactionLogRepository.save(capture(logged)) } answers { firstArg() }

        val result = service.backfillPreLogHires(10)

        assertEquals(1, result)
        assertEquals(mutableListOf("cyclone_puffin"), logged.captured.coach)
    }

    @Test
    fun `backfillPreLogHires skips teams with no coach on the earliest game`() {
        val earliest = game("Wyoming", "Air Force", emptyList(), emptyList(), "2023-09-01 12:00:00")
        every { gameRepository.findBySeason(10) } returns listOf(earliest)
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns emptyList()

        val result = service.backfillPreLogHires(10)

        assertEquals(0, result)
    }
}
