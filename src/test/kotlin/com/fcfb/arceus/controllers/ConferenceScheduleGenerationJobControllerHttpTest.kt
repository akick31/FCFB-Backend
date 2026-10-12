package com.fcfb.arceus.controllers

import com.fcfb.arceus.config.PRIVILEGED_POST_PATHS
import com.fcfb.arceus.dto.response.ScheduleGenJob
import com.fcfb.arceus.dto.response.ScheduleGenJobResponse
import com.fcfb.arceus.dto.response.ScheduleGenJobStatus
import com.fcfb.arceus.exception.GlobalExceptionHandler
import com.fcfb.arceus.exception.schedule.ScheduleNotFoundException
import com.fcfb.arceus.service.fcfb.schedule.ConferenceScheduleGenerationJobService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ConferenceScheduleGenerationJobControllerHttpTest {
    private val service = mockk<ConferenceScheduleGenerationJobService>()
    private val path = "${ApiConstants.FULL_PATH}/conference-schedule-generation-job"
    private val mvc =
        MockMvcBuilders.standaloneSetup(ConferenceScheduleGenerationJobController(service))
            .setControllerAdvice(GlobalExceptionHandler()).build()

    @Test
    fun `start accepts the existing season parameter and returns the job response`() {
        every { service.startAllConferenceGenerationAsync(12) } returns ScheduleGenJobResponse("job", "Started")

        mvc.perform(post(path).param("season", "12"))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.jobId").value("job"))
            .andExpect(jsonPath("$.message").value("Started"))

        verify(exactly = 1) { service.startAllConferenceGenerationAsync(12) }
        assertTrue(path in PRIVILEGED_POST_PATHS)
    }

    @Test
    fun `status retains interrupted progress counts and the failed enum`() {
        every { service.getScheduleGenJobStatus("job") } returns
            ScheduleGenJob("job", 12, ScheduleGenJobStatus.FAILED, totalConferences = 2, completedConferences = 1)

        mvc.perform(get("$path/status").param("jobId", "job"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.totalConferences").value(2))
            .andExpect(jsonPath("$.completedConferences").value(1))
            .andExpect(jsonPath("$.failedConferences").value(0))
            .andExpect(jsonPath("$.logs").isEmpty)
    }

    @Test
    fun `missing jobs retain the not found contract`() {
        every { service.getScheduleGenJobStatus("missing") } throws
            ScheduleNotFoundException("No schedule generation job found with id missing")

        mvc.perform(get("$path/status").param("jobId", "missing"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("No schedule generation job found with id missing"))
    }
}
