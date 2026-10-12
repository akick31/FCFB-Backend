package com.fcfb.arceus.controllers

import com.fcfb.arceus.exception.GlobalExceptionHandler
import com.fcfb.arceus.exception.statistics.InvalidGameStatsRequestException
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
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
        every { gameStatsService.getGameStatsForGame(gameId, team) } returns mockGameStats

        mockMvc.perform(
            get("/api/v1/arceus/game-stats/by-game")
                .param("gameId", gameId.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.getGameStatsForGame(gameId, team) }
    }

    @Test
    fun `getGameStatsByIdAndTeam should return 400 when gameId parameter is missing`() {
        val team = "Texas"
        every { gameStatsService.getGameStatsForGame(null, team) } throws
            InvalidGameStatsRequestException("Either gameId or season parameter is required")

        mockMvc.perform(
            get("/api/v1/arceus/game-stats/by-game")
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `getGameStatsByIdAndTeam should return 500 when team parameter is missing`() {
        val gameId = 123
        mockMvc.perform(
            get("/api/v1/arceus/game-stats/by-game")
                .param("gameId", gameId.toString())
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getGameStatsByIdAndTeam should return 500 when both parameters are missing`() {
        mockMvc.perform(
            get("/api/v1/arceus/game-stats/by-game")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getGameStatsByIdAndTeam should return 500 when gameId is not a number`() {
        val team = "Texas"
        mockMvc.perform(
            get("/api/v1/arceus/game-stats/by-game")
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
        every { gameStatsService.getGameStatsForGame(gameId, team) } throws RuntimeException("Service error")

        mockMvc.perform(
            get("/api/v1/arceus/game-stats/by-game")
                .param("gameId", gameId.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `team season read returns an array while game read returns an object`() {
        val stats = GameStats(gameId = 123, team = "Texas", season = 12)
        every { gameStatsService.getGameStatsForGame(123, "Texas") } returns stats
        every { gameStatsService.getGameStatsForTeamSeason("Texas", 12) } returns listOf(stats)

        mockMvc.perform(get("/api/v1/arceus/game-stats/by-game").param("gameId", "123").param("team", "Texas"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isMap)
            .andExpect(jsonPath("$.team").value("Texas"))
        mockMvc.perform(get("/api/v1/arceus/game-stats/by-team-season").param("season", "12").param("team", "Texas"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[0].team").value("Texas"))
    }

    @Test
    fun `team season read retains an empty array when no rows exist`() {
        every { gameStatsService.getGameStatsForTeamSeason("Texas", 12) } returns emptyList()
        mockMvc.perform(get("/api/v1/arceus/game-stats/by-team-season").param("season", "12").param("team", "Texas"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `team season read rejects a missing season through service validation`() {
        every { gameStatsService.getGameStatsForTeamSeason("Texas", null) } throws
            InvalidGameStatsRequestException("season parameter is required")
        mockMvc.perform(get("/api/v1/arceus/game-stats/by-team-season").param("team", "Texas"))
            .andExpect(status().isBadRequest)
        verify { gameStatsService.getGameStatsForTeamSeason("Texas", null) }
    }

    @Test
    fun `legacy read route is no longer mapped`() {
        mockMvc.perform(get("/api/v1/arceus/game-stats").param("gameId", "123").param("team", "Texas"))
            .andExpect(status().isNotFound)
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
        every { gameStatsService.getGameStatsForGame(gameId, team) } returns mockGameStats

        mockMvc.perform(
            get("/api/v1/arceus/game-stats/by-game")
                .param("gameId", gameId.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.getGameStatsForGame(gameId, team) }
    }

    @Test
    fun `getGameStatsByIdAndTeam should handle large gameId`() {
        val gameId = 999999
        val team = "Texas"
        val mockGameStats = mockk<GameStats>(relaxed = true)
        every { gameStatsService.getGameStatsForGame(gameId, team) } returns mockGameStats

        mockMvc.perform(
            get("/api/v1/arceus/game-stats/by-game")
                .param("gameId", gameId.toString())
                .param("team", team)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { gameStatsService.getGameStatsForGame(gameId, team) }
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
