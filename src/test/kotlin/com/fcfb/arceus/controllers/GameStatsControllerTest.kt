package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.service.fcfb.GameStatsService
import com.fcfb.arceus.util.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class GameStatsControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var gameStatsService: GameStatsService
    private lateinit var gameStatsController: GameStatsController

    @BeforeEach
    fun setup() {
        gameStatsService = mockk()
        gameStatsController = GameStatsController(gameStatsService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(gameStatsController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `getGameStatsByIdAndTeam should return game stats for valid parameters`() {
        val gameId = 123
        val team = "Texas"
        val mockGameStats = mockk<GameStats>(relaxed = true)
        every { gameStatsService.getGameStatsByIdAndTeam(gameId, team) } returns mockGameStats

        mockMvc.perform(
            get("/api/v1/arceus/game-stats")
                .param("gameId", gameId.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.getGameStatsByIdAndTeam(gameId, team) }
    }

    @Test
    fun `getGameStatsByIdAndTeam should return 400 when gameId parameter is missing`() {
        val team = "Texas"
        mockMvc.perform(
            get("/api/v1/arceus/game-stats")
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getGameStatsByIdAndTeam should return 500 when team parameter is missing`() {
        val gameId = 123
        mockMvc.perform(
            get("/api/v1/arceus/game-stats")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getGameStatsByIdAndTeam should return 500 when both parameters are missing`() {
        mockMvc.perform(
            get("/api/v1/arceus/game-stats")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getGameStatsByIdAndTeam should return 500 when gameId is not a number`() {
        val team = "Texas"
        mockMvc.perform(
            get("/api/v1/arceus/game-stats")
                .param("gameId", "invalid")
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getGameStatsByIdAndTeam should handle service exception`() {
        val gameId = 123
        val team = "Texas"
        every { gameStatsService.getGameStatsByIdAndTeam(gameId, team) } throws RuntimeException("Service error")

        mockMvc.perform(
            get("/api/v1/arceus/game-stats")
                .param("gameId", gameId.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `generateGameStats should return success for valid gameId`() {
        val gameId = 123
        every { gameStatsService.generateGameStats(gameId) } returns Unit

        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.generateGameStats(gameId) }
    }

    @Test
    fun `generateGameStats should return 500 when gameId parameter is missing`() {
        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `generateGameStats should return 500 when gameId is not a number`() {
        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate")
                .param("gameId", "invalid")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `generateGameStats should handle service exception`() {
        val gameId = 123
        every { gameStatsService.generateGameStats(gameId) } throws RuntimeException("Service error")

        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `generateAllGameStatsMoreRecentThanGameId should return success for valid gameId`() {
        val gameId = 123
        every { gameStatsService.generateGameStatsForGamesMoreRecentThanGameId(gameId) } returns Unit

        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate/all/more_recent_than")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.generateGameStatsForGamesMoreRecentThanGameId(gameId) }
    }

    @Test
    fun `generateAllGameStatsMoreRecentThanGameId should return 500 when gameId parameter is missing`() {
        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate/all/more_recent_than")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `generateAllGameStatsMoreRecentThanGameId should return 500 when gameId is not a number`() {
        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate/all/more_recent_than")
                .param("gameId", "invalid")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `generateAllGameStatsMoreRecentThanGameId should handle service exception`() {
        val gameId = 123
        every { gameStatsService.generateGameStatsForGamesMoreRecentThanGameId(gameId) } throws RuntimeException("Service error")

        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate/all/more_recent_than")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `generateAllGameStats should return success`() {
        every { gameStatsService.generateAllGameStats() } returns Unit

        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate/all")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.generateAllGameStats() }
    }

    @Test
    fun `generateAllGameStats should handle service exception`() {
        every { gameStatsService.generateAllGameStats() } throws RuntimeException("Service error")

        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate/all")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getGameStatsByIdAndTeam should handle negative gameId`() {
        val gameId = -1
        val team = "Texas"
        val mockGameStats = mockk<GameStats>(relaxed = true)
        every { gameStatsService.getGameStatsByIdAndTeam(gameId, team) } returns mockGameStats

        mockMvc.perform(
            get("/api/v1/arceus/game-stats")
                .param("gameId", gameId.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.getGameStatsByIdAndTeam(gameId, team) }
    }

    @Test
    fun `getGameStatsByIdAndTeam should handle large gameId`() {
        val gameId = 999999
        val team = "Texas"
        val mockGameStats = mockk<GameStats>(relaxed = true)
        every { gameStatsService.getGameStatsByIdAndTeam(gameId, team) } returns mockGameStats

        mockMvc.perform(
            get("/api/v1/arceus/game-stats")
                .param("gameId", gameId.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.getGameStatsByIdAndTeam(gameId, team) }
    }

    @Test
    fun `generateGameStats should handle negative gameId`() {
        val gameId = -1
        every { gameStatsService.generateGameStats(gameId) } returns Unit

        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.generateGameStats(gameId) }
    }

    @Test
    fun `generateGameStats should handle large gameId`() {
        val gameId = 999999
        every { gameStatsService.generateGameStats(gameId) } returns Unit

        mockMvc.perform(
            post("/api/v1/arceus/game-stats/generate")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.generateGameStats(gameId) }
    }
}
