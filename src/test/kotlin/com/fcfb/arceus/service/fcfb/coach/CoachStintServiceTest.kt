package com.fcfb.arceus.service.fcfb.coach

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.log.UsernameHistoryService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CoachStintServiceTest {
    private val coachTransactionLogRepository: CoachTransactionLogRepository = mockk()
    private val teamRepository: TeamRepository = mockk()
    private val usernameHistoryService: UsernameHistoryService = mockk()
    private lateinit var coachStintService: CoachStintService

    private val discordId = "100"

    private val coach =
        User().apply {
            id = 1L
            username = "buttersqauch"
            discordId = this@CoachStintServiceTest.discordId
        }

    @BeforeEach
    fun setup() {
        coachStintService = CoachStintService(coachTransactionLogRepository, teamRepository, usernameHistoryService)
        every { usernameHistoryService.getHistoricalUsernames(1L) } returns emptyList()
        every { teamRepository.findAll() } returns emptyList()
    }

    private fun entry(
        team: String,
        transaction: TransactionType,
        date: String,
    ) = CoachTransactionLog(
        team,
        CoachPosition.HEAD_COACH,
        mutableListOf("buttersqauch"),
        transaction,
        date,
        "admin",
        mutableListOf(discordId),
    )

    private fun at(timestamp: String) = coachStintService.parseGameTimestamp(timestamp)!!

    @Test
    fun `game at a coach's permanent team counts`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(entry("Marshall", TransactionType.HIRED, "01/09/2025 00:00:00"))

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)

        assertTrue(coachStintService.isPrimaryTeamAt(stints, "Marshall", at("2026-03-12 16:00:00")))
    }

    @Test
    fun `one-off sub for a team the coach has no stint on does not count`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(entry("Marshall", TransactionType.HIRED, "01/09/2025 00:00:00"))

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)

        assertFalse(coachStintService.isPrimaryTeamAt(stints, "Kent State", at("2025-04-03 13:00:00")))
    }

    @Test
    fun `interim stint taken while permanently employed elsewhere does not count`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(
                entry("Marshall", TransactionType.HIRED, "01/09/2025 00:00:00"),
                entry("Northern Arizona", TransactionType.HIRED_INTERIM, "01/23/2025 00:00:00"),
                entry("Northern Arizona", TransactionType.FIRED, "02/06/2025 00:00:00"),
            )

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)

        assertFalse(coachStintService.isPrimaryTeamAt(stints, "Northern Arizona", at("2025-01-24 12:00:00")))
    }

    @Test
    fun `interim stint with no permanent anywhere counts as primary`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(entry("Toledo", TransactionType.HIRED_INTERIM, "01/23/2025 00:00:00"))

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)

        assertTrue(coachStintService.isPrimaryTeamAt(stints, "Toledo", at("2025-01-24 12:00:00")))
    }

    @Test
    fun `two concurrent permanent stints both count`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(
                entry("Marshall", TransactionType.HIRED, "01/09/2025 00:00:00"),
                entry("Duke", TransactionType.HIRED, "01/10/2025 00:00:00"),
            )

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)

        assertTrue(coachStintService.isPrimaryTeamAt(stints, "Marshall", at("2025-06-01 12:00:00")))
        assertTrue(coachStintService.isPrimaryTeamAt(stints, "Duke", at("2025-06-01 12:00:00")))
    }

    @Test
    fun `a fired entry with no logged hire still counts as a tenure up to the fire`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(entry("USF", TransactionType.FIRED, "07/25/2026 00:00:00"))

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)

        assertTrue(coachStintService.isPrimaryTeamAt(stints, "USF", at("2025-10-23 13:00:00")))
        assertFalse(coachStintService.isPrimaryTeamAt(stints, "USF", at("2026-08-01 13:00:00")))
    }

    @Test
    fun `a game at the coach's current team counts even without a covering stint`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(entry("Marshall", TransactionType.FIRED, "01/01/2020 00:00:00"))

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)

        assertFalse(coachStintService.isPrimaryTeamAt(stints, "Montana State", at("2026-09-24 14:00:00")))
        assertTrue(coachStintService.countsForCoach(stints, "Montana State", "Montana State", at("2026-09-24 14:00:00")))
    }

    @Test
    fun `backdating pulls an earliest stint start back to cover a pre-hire game`() {
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(entry("LSU", TransactionType.HIRED, "09/25/2026 00:00:00"))

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)
        assertFalse(coachStintService.isPrimaryTeamAt(stints, "LSU", at("2026-09-24 14:00:00")))

        coachStintService.backdateStartsToGames(stints, mapOf("LSU" to at("2026-09-24 14:00:00")))

        assertTrue(coachStintService.isPrimaryTeamAt(stints, "LSU", at("2026-09-24 14:00:00")))
    }

    @Test
    fun `fake teams are excluded from stints`() {
        every { teamRepository.findAll() } returns
            listOf(
                mockk<Team> {
                    every { name } returns "Big Horn"
                    every { subdivision } returns Subdivision.FAKE
                },
            )
        every { coachTransactionLogRepository.getEntireCoachTransactionLog() } returns
            listOf(entry("Big Horn", TransactionType.HIRED, "05/02/2025 00:00:00"))

        val stints = coachStintService.getStintsForCoach("buttersqauch", coach)

        assertFalse(coachStintService.isPrimaryTeamAt(stints, "Big Horn", at("2025-06-01 12:00:00")))
    }
}
