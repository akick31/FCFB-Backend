package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.service.fcfb.ScheduleService
import com.fcfb.arceus.util.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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
