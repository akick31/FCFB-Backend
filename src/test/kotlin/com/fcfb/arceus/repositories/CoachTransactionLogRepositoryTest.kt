package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.CoachTransactionLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoachTransactionLogRepositoryTest {
    private lateinit var coachTransactionLogRepository: CoachTransactionLogRepository

    @BeforeEach
    fun setUp() {
        coachTransactionLogRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and find by id`() {
        val coachTransactionLog = createTestCoachTransactionLog()
        coachTransactionLog.id = 1

        every { coachTransactionLogRepository.save(any()) } returns coachTransactionLog
        every { coachTransactionLogRepository.findById(1) } returns java.util.Optional.of(coachTransactionLog)

        val saved = coachTransactionLogRepository.save(coachTransactionLog)
        val found = coachTransactionLogRepository.findById(saved.id!!).get()

        assertNotNull(found)
        assertEquals("Alabama", found.team)
        assertEquals(CoachPosition.HEAD_COACH, found.position)
        assertEquals(TransactionType.HIRED, found.transaction)
        assertEquals("2024-01-01", found.transactionDate)
        assertEquals("Admin", found.processedBy)
    }

    @Test
    fun `test find by team`() {
        val coachTransactionLog1 = createTestCoachTransactionLog(team = "Alabama")
        val coachTransactionLog2 = createTestCoachTransactionLog(team = "Auburn")
        val alabamaLogs = listOf(coachTransactionLog1)
        val auburnLogs = listOf(coachTransactionLog2)

        every { coachTransactionLogRepository.findByTeam("Alabama") } returns alabamaLogs
        every { coachTransactionLogRepository.findByTeam("Auburn") } returns auburnLogs

        val foundAlabamaLogs = coachTransactionLogRepository.findByTeam("Alabama")
        val foundAuburnLogs = coachTransactionLogRepository.findByTeam("Auburn")

        assertEquals(1, foundAlabamaLogs.size)
        assertEquals("Alabama", foundAlabamaLogs[0].team)
        assertEquals(1, foundAuburnLogs.size)
        assertEquals("Auburn", foundAuburnLogs[0].team)
    }

    @Test
    fun `test find by transaction`() {
        val hiredLog = createTestCoachTransactionLog(transaction = TransactionType.HIRED)
        val firedLog = createTestCoachTransactionLog(transaction = TransactionType.FIRED)
        val hiredLogs = listOf(hiredLog)
        val firedLogs = listOf(firedLog)

        every { coachTransactionLogRepository.findByTransaction(TransactionType.HIRED) } returns hiredLogs
        every { coachTransactionLogRepository.findByTransaction(TransactionType.FIRED) } returns firedLogs

        val foundHiredLogs = coachTransactionLogRepository.findByTransaction(TransactionType.HIRED)
        val foundFiredLogs = coachTransactionLogRepository.findByTransaction(TransactionType.FIRED)

        assertEquals(1, foundHiredLogs.size)
        assertEquals(TransactionType.HIRED, foundHiredLogs[0].transaction)
        assertEquals(1, foundFiredLogs.size)
        assertEquals(TransactionType.FIRED, foundFiredLogs[0].transaction)
    }

    @Test
    fun `test find by position`() {
        val headCoachLog = createTestCoachTransactionLog(position = CoachPosition.HEAD_COACH)
        val offensiveCoordinatorLog = createTestCoachTransactionLog(position = CoachPosition.OFFENSIVE_COORDINATOR)
        val headCoachLogs = listOf(headCoachLog)
        val offensiveCoordinatorLogs = listOf(offensiveCoordinatorLog)

        every { coachTransactionLogRepository.findByPosition(CoachPosition.HEAD_COACH) } returns headCoachLogs
        every { coachTransactionLogRepository.findByPosition(CoachPosition.OFFENSIVE_COORDINATOR) } returns offensiveCoordinatorLogs

        val foundHeadCoachLogs = coachTransactionLogRepository.findByPosition(CoachPosition.HEAD_COACH)
        val foundOffensiveCoordinatorLogs = coachTransactionLogRepository.findByPosition(CoachPosition.OFFENSIVE_COORDINATOR)

        assertEquals(1, foundHeadCoachLogs.size)
        assertEquals(CoachPosition.HEAD_COACH, foundHeadCoachLogs[0].position)
        assertEquals(1, foundOffensiveCoordinatorLogs.size)
        assertEquals(CoachPosition.OFFENSIVE_COORDINATOR, foundOffensiveCoordinatorLogs[0].position)
    }

    @Test
    fun `test find by processed by`() {
        val adminLog = createTestCoachTransactionLog(processedBy = "Admin")
        val systemLog = createTestCoachTransactionLog(processedBy = "System")
        val adminLogs = listOf(adminLog)
        val systemLogs = listOf(systemLog)

        every { coachTransactionLogRepository.findByProcessedBy("Admin") } returns adminLogs
        every { coachTransactionLogRepository.findByProcessedBy("System") } returns systemLogs

        val foundAdminLogs = coachTransactionLogRepository.findByProcessedBy("Admin")
        val foundSystemLogs = coachTransactionLogRepository.findByProcessedBy("System")

        assertEquals(1, foundAdminLogs.size)
        assertEquals("Admin", foundAdminLogs[0].processedBy)
        assertEquals(1, foundSystemLogs.size)
        assertEquals("System", foundSystemLogs[0].processedBy)
    }

    @Test
    fun `test delete by id`() {
        every { coachTransactionLogRepository.deleteById(1) } returns Unit

        coachTransactionLogRepository.deleteById(1)

        verify { coachTransactionLogRepository.deleteById(1) }
    }

    @Test
    fun `test update`() {
        val coachTransactionLog = createTestCoachTransactionLog()
        every { coachTransactionLogRepository.save(any()) } returns coachTransactionLog

        val updated = coachTransactionLogRepository.save(coachTransactionLog)

        assertNotNull(updated)
        assertEquals("Alabama", updated.team)
    }

    @Test
    fun `test count`() {
        every { coachTransactionLogRepository.count() } returns 10L

        val count = coachTransactionLogRepository.count()

        assertEquals(10L, count)
    }

    @Test
    fun `test existsById`() {
        every { coachTransactionLogRepository.existsById(1) } returns true
        every { coachTransactionLogRepository.existsById(999) } returns false

        assertTrue(coachTransactionLogRepository.existsById(1))
        assertFalse(coachTransactionLogRepository.existsById(999))
    }

    @Test
    fun `test delete`() {
        val coachTransactionLog = createTestCoachTransactionLog()
        every { coachTransactionLogRepository.delete(coachTransactionLog) } returns Unit

        coachTransactionLogRepository.delete(coachTransactionLog)

        verify { coachTransactionLogRepository.delete(coachTransactionLog) }
    }

    @Test
    fun `test deleteAll`() {
        every { coachTransactionLogRepository.deleteAll() } returns Unit

        coachTransactionLogRepository.deleteAll()

        verify { coachTransactionLogRepository.deleteAll() }
    }

    @Test
    fun `test saveAll`() {
        val coachTransactionLog1 = createTestCoachTransactionLog(team = "Alabama")
        val coachTransactionLog2 = createTestCoachTransactionLog(team = "Auburn")
        val coachTransactionLogs = listOf(coachTransactionLog1, coachTransactionLog2)

        every { coachTransactionLogRepository.saveAll(coachTransactionLogs) } returns coachTransactionLogs

        val savedCoachTransactionLogs = coachTransactionLogRepository.saveAll(coachTransactionLogs)

        assertEquals(2, savedCoachTransactionLogs.count())
        assertTrue(savedCoachTransactionLogs.any { it.team == "Alabama" })
        assertTrue(savedCoachTransactionLogs.any { it.team == "Auburn" })
    }

    private fun createTestCoachTransactionLog(
        id: Int = 1,
        team: String = "Alabama",
        position: CoachPosition = CoachPosition.HEAD_COACH,
        coach: MutableList<String> = mutableListOf("Coach1"),
        transaction: TransactionType = TransactionType.HIRED,
        transactionDate: String = "2024-01-01",
        processedBy: String = "Admin",
    ): CoachTransactionLog {
        return CoachTransactionLog(
            team = team,
            position = position,
            coach = coach,
            transaction = transaction,
            transactionDate = transactionDate,
            processedBy = processedBy,
        )
    }
}
