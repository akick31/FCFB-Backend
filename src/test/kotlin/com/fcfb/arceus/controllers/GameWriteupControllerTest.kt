package com.fcfb.arceus.controllers

import com.fcfb.arceus.exception.GlobalExceptionHandler
import com.fcfb.arceus.service.fcfb.game.GameWriteupService
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

class GameWriteupControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var gameWriteupService: GameWriteupService
    private lateinit var gameWriteupController: GameWriteupController

    @BeforeEach
    fun setup() {
        gameWriteupService = mockk()
        gameWriteupController = GameWriteupController(gameWriteupService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(gameWriteupController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `getGameMessageByScenario should return message for valid scenario and playCall`() {
        val scenario = "touchdown"
        val playCall = "pass"
        val mockMessage = "Touchdown pass! The quarterback throws a perfect spiral to the receiver in the end zone."
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle special characters in scenario`() {
        val scenario = "field-goal"
        val playCall = "kick"
        val mockMessage = "Field goal attempt! The kicker lines up and sends the ball through the uprights."
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle special characters in playCall`() {
        val scenario = "interception"
        val playCall = "pick-six"
        val mockMessage = "Interception! The defensive back picks off the pass and returns it for a touchdown!"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle uppercase scenario and playCall`() {
        val scenario = "TOUCHDOWN"
        val playCall = "PASS"
        val mockMessage = "Touchdown pass! The quarterback throws a perfect spiral to the receiver in the end zone."
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle service exception`() {
        val scenario = "touchdown"
        val playCall = "pass"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } throws RuntimeException("Service error")

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `getGameMessageByScenario should handle empty scenario`() {
        val scenario = ""
        val playCall = "pass"
        val mockMessage = "Empty scenario message"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle empty playCall`() {
        val scenario = "touchdown"
        val playCall = ""
        val mockMessage = "Empty play call message"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle long scenario name`() {
        val scenario = "very-long-scenario-name-with-many-words"
        val playCall = "pass"
        val mockMessage = "Long scenario message"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle long playCall name`() {
        val scenario = "touchdown"
        val playCall = "very-long-play-call-name-with-many-words"
        val mockMessage = "Long play call message"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle numbers in scenario`() {
        val scenario = "touchdown-2"
        val playCall = "pass"
        val mockMessage = "Touchdown number 2 message"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle numbers in playCall`() {
        val scenario = "touchdown"
        val playCall = "pass-3"
        val mockMessage = "Pass number 3 message"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle underscore in scenario`() {
        val scenario = "touchdown_pass"
        val playCall = "pass"
        val mockMessage = "Touchdown pass message"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }

    @Test
    fun `getGameMessageByScenario should handle underscore in playCall`() {
        val scenario = "touchdown"
        val playCall = "deep_pass"
        val mockMessage = "Deep pass message"
        every { gameWriteupService.getGameMessageByScenario(scenario, playCall) } returns mockMessage

        mockMvc.perform(
            get("/api/v1/arceus/game_writeup")
                .param("scenario", scenario)
                .param("playCall", playCall)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)
            .andExpect(content().string(mockMessage))

        verify { gameWriteupService.getGameMessageByScenario(scenario, playCall) }
    }
}
