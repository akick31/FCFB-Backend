package com.fcfb.arceus.controllers.log

import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.log.CoachTransactionLog
import com.fcfb.arceus.service.log.CoachTransactionLogService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CoachTransactionLogControllerTest {
    private val coachTransactionLogService: CoachTransactionLogService = mockk()
    private val controller = CoachTransactionLogController(coachTransactionLogService)

    @Test
    fun `should return entire coach transaction log`() {
        val mockLogs =
            listOf(
                CoachTransactionLog(
                    team = "Team A",
                    position = CoachPosition.HEAD_COACH,
                    coach = mutableListOf("Coach X"),
                    transaction = TransactionType.HIRED,
                    transactionDate = "2023-10-01T12:00:00",
                    processedBy = "Admin",
                ),
                CoachTransactionLog(
                    team = "Team B",
                    position = CoachPosition.OFFENSIVE_COORDINATOR,
                    coach = mutableListOf("Coach Y"),
                    transaction = TransactionType.FIRED,
                    transactionDate = "2023-10-02T14:00:00",
                    processedBy = "Admin",
                ),
            )

        every { coachTransactionLogService.getEntireCoachTransactionLog() } returns mockLogs

        val response: List<CoachTransactionLog> = controller.getEntireCoachTransactionLog()

        assertEquals(mockLogs, response)
    }

    @Test
    fun `should return empty list when no coach transaction logs exist`() {
        every { coachTransactionLogService.getEntireCoachTransactionLog() } returns emptyList()

        val response: List<CoachTransactionLog> = controller.getEntireCoachTransactionLog()

        assertEquals(emptyList<CoachTransactionLog>(), response)
    }

    @Test
    fun `should handle exception from service`() {
        every { coachTransactionLogService.getEntireCoachTransactionLog() } throws RuntimeException("Service error")

        val exception =
            assertThrows<RuntimeException> {
                controller.getEntireCoachTransactionLog()
            }

        assertEquals("Service error", exception.message)
    }

    @Test
    fun `should return large list of coach transaction logs`() {
        val mockLogs =
            (1..1000).map {
                CoachTransactionLog(
                    team = "Team $it",
                    position = CoachPosition.HEAD_COACH,
                    coach = mutableListOf("Coach $it"),
                    transaction = TransactionType.HIRED,
                    transactionDate = "2023-10-01T12:00:00",
                    processedBy = "Admin",
                )
            }

        every { coachTransactionLogService.getEntireCoachTransactionLog() } returns mockLogs

        val response: List<CoachTransactionLog> = controller.getEntireCoachTransactionLog()

        assertEquals(mockLogs, response)
    }
}
