package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.RecordRepository
import com.fcfb.arceus.repositories.SeasonRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.fcfb.record.AgainstRecordService
import com.fcfb.arceus.service.fcfb.record.GameRecordService
import com.fcfb.arceus.service.fcfb.record.GeneralRecordService
import com.fcfb.arceus.service.fcfb.record.RecordQualifier
import com.fcfb.arceus.service.fcfb.record.RecordStatUtils
import com.fcfb.arceus.service.fcfb.record.SeasonRecordService
import com.fcfb.arceus.service.specification.RecordSpecificationService
import com.fcfb.arceus.util.RecordGenerationInProgressException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecordServiceTest {
    private val recordRepository: RecordRepository = mockk()
    private val gameStatsRepository: GameStatsRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private val recordSpecificationService: RecordSpecificationService = mockk()
    private val gameRecordService: GameRecordService = mockk()
    private val seasonRecordService: SeasonRecordService = mockk()
    private val generalRecordService: GeneralRecordService = mockk()
    private val againstRecordService: AgainstRecordService = mockk()
    private val recordStatUtils: RecordStatUtils = mockk()
    private val recordQualifier: RecordQualifier = mockk()
    private val teamRepository: TeamRepository = mockk()
    private val seasonRepository: SeasonRepository = mockk()
    private lateinit var recordService: RecordService

    @BeforeEach
    fun setup() {
        recordService =
            RecordService(
                recordRepository,
                gameStatsRepository,
                gameRepository,
                recordSpecificationService,
                gameRecordService,
                seasonRecordService,
                generalRecordService,
                againstRecordService,
                recordStatUtils,
                recordQualifier,
                teamRepository,
                seasonRepository,
            )
    }

    @Test
    fun `a call that overlaps a running generateAllRecords is rejected`() {
        var nestedCallWasRejected = false
        every { recordRepository.deleteAll() } answers {
            assertThrows(RecordGenerationInProgressException::class.java) {
                recordService.generateAllRecords()
            }
            nestedCallWasRejected = true
            throw RuntimeException("stop the outer call once the reentrancy check has run")
        }

        assertThrows(RuntimeException::class.java) { recordService.generateAllRecords() }

        assertTrue(nestedCallWasRejected)
    }

    @Test
    fun `the guard is released after a failed run so a later call can proceed`() {
        every { recordRepository.deleteAll() } throws RuntimeException("boom")

        assertThrows(RuntimeException::class.java) { recordService.generateAllRecords() }
        assertThrows(RuntimeException::class.java) { recordService.generateAllRecords() }

        verify(exactly = 2) { recordRepository.deleteAll() }
    }
}
