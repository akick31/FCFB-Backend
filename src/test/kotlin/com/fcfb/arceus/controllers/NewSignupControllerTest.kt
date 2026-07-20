package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.response.NewSignupDTO
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.service.fcfb.NewSignupService
import com.fcfb.arceus.util.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class NewSignupControllerTest {
    private lateinit var mockMvc: MockMvc
    private val newSignupService: NewSignupService = mockk()
    private lateinit var newSignupController: NewSignupController

    @BeforeEach
    fun setup() {
        newSignupController = NewSignupController(newSignupService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(newSignupController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `should get new signups successfully`() {
        val signups =
            listOf(
                NewSignupDTO(
                    id = 1,
                    username = "testuser1",
                    coachName = "Test Coach 1",
                    discordTag = "test1#1234",
                    discordId = "123456789",
                    position = CoachPosition.HEAD_COACH,
                    teamChoiceOne = "team1",
                    teamChoiceTwo = "team2",
                    teamChoiceThree = "team3",
                    offensivePlaybook = OffensivePlaybook.AIR_RAID,
                    defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                    approved = false,
                ),
                NewSignupDTO(
                    id = 2,
                    username = "testuser2",
                    coachName = "Test Coach 2",
                    discordTag = "test2#1234",
                    discordId = "123456788",
                    position = CoachPosition.HEAD_COACH,
                    teamChoiceOne = "team1",
                    teamChoiceTwo = "team2",
                    teamChoiceThree = "team3",
                    offensivePlaybook = OffensivePlaybook.AIR_RAID,
                    defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                    approved = false,
                ),
            )

        every { newSignupService.getNewSignups() } returns signups

        mockMvc.perform(get("/api/v1/arceus/new_signups").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].username").value("testuser1"))
            .andExpect(jsonPath("$[0].coachName").value("Test Coach 1"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].username").value("testuser2"))
            .andExpect(jsonPath("$[1].coachName").value("Test Coach 2"))
    }

    @Test
    fun `should handle error when getting new signups`() {
        every { newSignupService.getNewSignups() } throws RuntimeException("Failed to get signups")

        mockMvc.perform(get("/api/v1/arceus/new_signups").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("Failed to get signups"))
    }
}
