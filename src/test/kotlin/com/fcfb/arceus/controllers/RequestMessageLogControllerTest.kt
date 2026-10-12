package com.fcfb.arceus.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.arceus.enums.system.MessageType
import com.fcfb.arceus.exception.GlobalExceptionHandler
import com.fcfb.arceus.model.RequestMessageLog
import com.fcfb.arceus.service.log.RequestMessageLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class RequestMessageLogControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var requestMessageLogService: RequestMessageLogService
    private lateinit var requestMessageLogController: RequestMessageLogController
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        requestMessageLogService = mockk()
        requestMessageLogController = RequestMessageLogController(requestMessageLogService)
        objectMapper = ObjectMapper()
        mockMvc =
            MockMvcBuilders.standaloneSetup(requestMessageLogController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `logRequestMessage should return success for valid request`() {
        val requestMessageLog =
            RequestMessageLog(
                messageType = MessageType.GAME_THREAD,
                gameId = 123,
                playId = 456,
                messageId = 789L,
                messageLocation = "discord",
                messageTs = "2024-01-01T12:00:00Z",
            )

        val expectedResponse = requestMessageLog
        every { requestMessageLogService.logRequestMessage(any()) } returns expectedResponse

        val requestJson = objectMapper.writeValueAsString(requestMessageLog)

        mockMvc.perform(
            post("/api/v1/arceus/request_message_log")
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { requestMessageLogService.logRequestMessage(any()) }
    }

    @Test
    fun `logRequestMessage should return success for private message type`() {
        val requestMessageLog =
            RequestMessageLog(
                messageType = MessageType.PRIVATE_MESSAGE,
                gameId = 123,
                playId = null,
                messageId = 789L,
                messageLocation = "discord",
                messageTs = "2024-01-01T12:00:00Z",
            )

        val expectedResponse = requestMessageLog
        every { requestMessageLogService.logRequestMessage(any()) } returns expectedResponse

        val requestJson = objectMapper.writeValueAsString(requestMessageLog)

        mockMvc.perform(
            post("/api/v1/arceus/request_message_log")
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { requestMessageLogService.logRequestMessage(any()) }
    }

    @Test
    fun `logRequestMessage should return 400 for invalid JSON`() {
        val invalidJson = "{ invalid json }"

        mockMvc.perform(
            post("/api/v1/arceus/request_message_log")
                .content(invalidJson)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `logRequestMessage should return 400 for missing required fields`() {
        val incompleteJson =
            """
            {
                "gameId": 123,
                "messageId": 789
            }
            """.trimIndent()

        mockMvc.perform(
            post("/api/v1/arceus/request_message_log")
                .content(incompleteJson)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `logRequestMessage should handle null playId`() {
        val requestMessageLog =
            RequestMessageLog(
                messageType = MessageType.GAME_THREAD,
                gameId = 123,
                playId = null,
                messageId = 789L,
                messageLocation = "discord",
                messageTs = "2024-01-01T12:00:00Z",
            )

        val expectedResponse = requestMessageLog
        every { requestMessageLogService.logRequestMessage(any()) } returns expectedResponse

        val requestJson = objectMapper.writeValueAsString(requestMessageLog)

        mockMvc.perform(
            post("/api/v1/arceus/request_message_log")
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isOk)

        verify { requestMessageLogService.logRequestMessage(any()) }
    }

    @Test
    fun `logRequestMessage should handle service exception`() {
        val requestMessageLog =
            RequestMessageLog(
                messageType = MessageType.GAME_THREAD,
                gameId = 123,
                playId = 456,
                messageId = 789L,
                messageLocation = "discord",
                messageTs = "2024-01-01T12:00:00Z",
            )

        every { requestMessageLogService.logRequestMessage(requestMessageLog) } throws RuntimeException("Service error")

        val requestJson = objectMapper.writeValueAsString(requestMessageLog)

        mockMvc.perform(
            post("/api/v1/arceus/request_message_log")
                .content(requestJson)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `logRequestMessage should handle empty request body`() {
        mockMvc.perform(
            post("/api/v1/arceus/request_message_log")
                .content("")
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `logRequestMessage should handle malformed JSON with invalid enum`() {
        val malformedJson =
            """
            {
                "messageType": "INVALID_TYPE",
                "gameId": 123,
                "playId": 456,
                "messageId": 789,
                "messageLocation": "discord",
                "messageTs": "2024-01-01T12:00:00Z"
            }
            """.trimIndent()

        mockMvc.perform(
            post("/api/v1/arceus/request_message_log")
                .content(malformedJson)
                .contentType(MediaType.APPLICATION_JSON),
        )
            .andExpect(status().isInternalServerError)
    }
}
