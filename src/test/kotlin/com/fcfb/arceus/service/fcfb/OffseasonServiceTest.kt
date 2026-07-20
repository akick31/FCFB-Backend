package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Offseason
import com.fcfb.arceus.repositories.OffseasonRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OffseasonServiceTest {
    private val offseasonRepository: OffseasonRepository = mockk()
    private lateinit var offseasonService: OffseasonService

    @BeforeEach
    fun setup() {
        offseasonService = OffseasonService(offseasonRepository)
    }

    @Test
    fun `should return the current offseason`() {
        val offseason = Offseason(startDate = "01/01/2024 00:00:00", endDate = null)
        every { offseasonRepository.getCurrentOffseason() } returns offseason

        val result = offseasonService.getCurrentOffseason()

        assertEquals(offseason, result)
    }

    @Test
    fun `should return null when there is no current offseason`() {
        every { offseasonRepository.getCurrentOffseason() } returns null

        val result = offseasonService.getCurrentOffseason()

        assertNull(result)
    }

    @Test
    fun `should start a new offseason`() {
        every { offseasonRepository.save(any()) } returns Offseason(startDate = "01/01/2024 00:00:00", endDate = null)

        offseasonService.startOffseason("01/01/2024 00:00:00")

        verify {
            offseasonRepository.save(
                match { it.startDate == "01/01/2024 00:00:00" && it.endDate == null },
            )
        }
    }

    @Test
    fun `should end the current offseason`() {
        val openOffseason = Offseason(startDate = "01/01/2024 00:00:00", endDate = null)
        every { offseasonRepository.getCurrentOffseason() } returns openOffseason
        every { offseasonRepository.save(any()) } returns openOffseason

        offseasonService.endOffseason("06/01/2024 00:00:00")

        verify { offseasonRepository.save(openOffseason) }
        assertEquals("06/01/2024 00:00:00", openOffseason.endDate)
    }

    @Test
    fun `should do nothing when ending offseason and none is open`() {
        every { offseasonRepository.getCurrentOffseason() } returns null

        offseasonService.endOffseason("06/01/2024 00:00:00")

        verify(exactly = 0) { offseasonRepository.save(any()) }
    }
}
