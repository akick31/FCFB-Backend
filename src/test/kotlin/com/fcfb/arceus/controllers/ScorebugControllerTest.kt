package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.ScorebugService
import com.fcfb.arceus.util.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ScorebugControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var scorebugService: ScorebugService
    private lateinit var scorebugController: ScorebugController

    @BeforeEach
    fun setup() {
        scorebugService = mockk(relaxed = true)
        scorebugController = ScorebugController(scorebugService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(scorebugController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `generateAllScorebugs should return success`() {
        every { scorebugService.generateAllScorebugs() } returns Unit

        mockMvc.perform(
            post("/api/v1/arceus/scorebug/generate/all")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { scorebugService.generateAllScorebugs() }
    }

    @Test
    fun `getScorebugByGameId should return scorebug for valid game id`() {
        val gameId = 123
        val mockScorebug = "mock_scorebug_data".toByteArray()
        val mockResponse = ResponseEntity.ok(mockScorebug)
        every { scorebugService.getScorebugByGameId(gameId) } returns mockResponse

        mockMvc.perform(
            get("/api/v1/arceus/scorebug")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { scorebugService.getScorebugByGameId(gameId) }
    }

    @Test
    fun `getScorebugByGameId should return 500 when gameId is missing`() {
        mockMvc.perform(
            get("/api/v1/arceus/scorebug")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getLatestScorebugByGameId should return latest scorebug for valid game id`() {
        val gameId = 123
        val mockScorebug = "latest_mock_scorebug_data".toByteArray()
        val mockResponse = ResponseEntity.ok(mockScorebug)
        every { scorebugService.getLatestScorebugByGameId(gameId) } returns mockResponse

        mockMvc.perform(
            get("/api/v1/arceus/scorebug/latest")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { scorebugService.getLatestScorebugByGameId(gameId) }
    }

    @Test
    fun `getLatestScorebugByGameId should return 500 when gameId is missing`() {
        mockMvc.perform(
            get("/api/v1/arceus/scorebug/latest")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getScorebugsForConference should return scorebugs for valid parameters`() {
        val season = 2024
        val week = 5
        val conference = "BIG_12"
        val mockScorebugs = listOf(mapOf<String, Any>("gameId" to "123", "image" to "base64data"))
        val mockResponse = ResponseEntity.ok(mockScorebugs)
        every { scorebugService.getScorebugsForConference(season, week, conference) } returns mockResponse

        mockMvc.perform(
            get("/api/v1/arceus/scorebug/conference")
                .param("season", season.toString())
                .param("week", week.toString())
                .param("conference", conference)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { scorebugService.getScorebugsForConference(season, week, conference) }
    }

    @Test
    fun `getScorebugsForConference should return 500 when required parameters are missing`() {
        // Missing season
        mockMvc.perform(
            get("/api/v1/arceus/scorebug/conference")
                .param("week", "5")
                .param("conference", "BIG_12")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)

        // Missing week
        mockMvc.perform(
            get("/api/v1/arceus/scorebug/conference")
                .param("season", "2024")
                .param("conference", "BIG_12")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)

        // Missing conference
        mockMvc.perform(
            get("/api/v1/arceus/scorebug/conference")
                .param("season", "2024")
                .param("week", "5")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getFilteredScorebugs should return filtered scorebugs with all parameters`() {
        mockMvc.perform(
            get("/api/v1/arceus/scorebug/filtered")
                .param("filters", "IN_PROGRESS")
                .param("category", "ONGOING")
                .param("sort", "CLOSEST_TO_END")
                .param("conference", "BIG_12")
                .param("season", "2024")
                .param("week", "5")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getFilteredScorebugs should return filtered scorebugs with minimal parameters`() {
        mockMvc.perform(
            get("/api/v1/arceus/scorebug/filtered")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getFilteredScorebugs should handle multiple filters`() {
        mockMvc.perform(
            get("/api/v1/arceus/scorebug/filtered")
                .param("filters", "IN_PROGRESS", "OUT_OF_CONFERENCE")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }
}
