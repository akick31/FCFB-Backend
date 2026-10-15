package com.fcfb.arceus.controllers.schedule

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.exception.GlobalExceptionHandler
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ScheduleControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var scheduleService: ScheduleService
    private lateinit var scheduleController: ScheduleController

    @BeforeEach
    fun setup() {
        scheduleService = mockk()
        scheduleController = ScheduleController(scheduleService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(scheduleController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `bulk creation retains created status and list response`() {
        every { scheduleService.createBulkScheduleEntries(any()) } returns emptyList()

        mockMvc.perform(
            post("${ApiConstants.FULL_PATH}/schedule/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"entries":[]}"""),
        ).andExpect(status().isCreated)
            .andExpect(content().json("[]"))
    }

    @Test
    fun `delete routes retain empty no-content responses`() {
        every { scheduleService.deleteScheduleEntry(7) } just Runs
        every { scheduleService.deleteScheduleBySeason(10) } just Runs

        mockMvc.perform(delete("${ApiConstants.FULL_PATH}/schedule").param("id", "7"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
        mockMvc.perform(delete("${ApiConstants.FULL_PATH}/schedule/season").param("season", "10"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        verify { scheduleService.deleteScheduleEntry(7) }
        verify { scheduleService.deleteScheduleBySeason(10) }
    }

    @Test
    fun `getTeamOpponent should return opponent for valid team`() {
        val team = "Texas"
        val mockOpponent = "Oklahoma"
        every { scheduleService.getTeamOpponent(team) } returns mockOpponent

        mockMvc.perform(
            get("/api/v1/arceus/schedule/opponent")
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockOpponent))

        verify { scheduleService.getTeamOpponent(team) }
    }

    @Test
    fun `getTeamOpponent should return 500 when team parameter is missing`() {
        mockMvc.perform(
            get("/api/v1/arceus/schedule/opponent")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getTeamOpponent should return 500 when team parameter is empty`() {
        mockMvc.perform(
            get("/api/v1/arceus/schedule/opponent")
                .param("team", "")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getTeamOpponent should handle service exception`() {
        val team = "Texas"
        every { scheduleService.getTeamOpponent(team) } throws RuntimeException("Service error")

        mockMvc.perform(
            get("/api/v1/arceus/schedule/opponent")
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getScheduleBySeasonAndTeam should return schedule for valid parameters`() {
        val season = 2024
        val team = "Texas"
        val mockSchedule = listOf(mockk<Schedule>(relaxed = true))
        every { scheduleService.getScheduleBySeasonAndTeam(season, team) } returns mockSchedule

        mockMvc.perform(
            get("/api/v1/arceus/schedule/season")
                .param("season", season.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { scheduleService.getScheduleBySeasonAndTeam(season, team) }
    }

    @Test
    fun `getScheduleBySeasonAndTeam should return 500 when season parameter is missing`() {
        val team = "Texas"
        mockMvc.perform(
            get("/api/v1/arceus/schedule/season")
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getScheduleBySeasonAndTeam should return 500 when team parameter is missing`() {
        val season = 2024
        mockMvc.perform(
            get("/api/v1/arceus/schedule/season")
                .param("season", season.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getScheduleBySeasonAndTeam should return 500 when both parameters are missing`() {
        mockMvc.perform(
            get("/api/v1/arceus/schedule/season")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getScheduleBySeasonAndTeam should return 500 when season parameter is not a number`() {
        val team = "Texas"
        mockMvc.perform(
            get("/api/v1/arceus/schedule/season")
                .param("season", "invalid")
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getScheduleBySeasonAndTeam should handle service exception`() {
        val season = 2024
        val team = "Texas"
        every { scheduleService.getScheduleBySeasonAndTeam(season, team) } throws RuntimeException("Service error")

        mockMvc.perform(
            get("/api/v1/arceus/schedule/season")
                .param("season", season.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getScheduleBySeasonAndTeam should handle negative season`() {
        val season = -1
        val team = "Texas"
        val mockSchedule = listOf(mockk<Schedule>(relaxed = true))
        every { scheduleService.getScheduleBySeasonAndTeam(season, team) } returns mockSchedule

        mockMvc.perform(
            get("/api/v1/arceus/schedule/season")
                .param("season", season.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { scheduleService.getScheduleBySeasonAndTeam(season, team) }
    }

    @Test
    fun `getScheduleBySeasonAndTeam should handle large season number`() {
        val season = 9999
        val team = "Texas"
        val mockSchedule = listOf(mockk<Schedule>(relaxed = true))
        every { scheduleService.getScheduleBySeasonAndTeam(season, team) } returns mockSchedule

        mockMvc.perform(
            get("/api/v1/arceus/schedule/season")
                .param("season", season.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { scheduleService.getScheduleBySeasonAndTeam(season, team) }
    }

    @Test
    fun `getTeamOpponent should handle special characters in team name`() {
        val team = "Texas A&M"
        val mockOpponent = "LSU"
        every { scheduleService.getTeamOpponent(team) } returns mockOpponent

        mockMvc.perform(
            get("/api/v1/arceus/schedule/opponent")
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockOpponent))

        verify { scheduleService.getTeamOpponent(team) }
    }
}
