package com.fcfb.arceus.service.discord

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fcfb.arceus.dto.request.GameFinalizationRollbackRequest
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.util.ServerUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import kotlin.test.assertEquals

class DiscordFinalizationRollbackTest {
    private val http = mockk<RestTemplate>()
    private val service = DiscordService(http, ServerUtils(), "http://bot/fcfb_discord", "guild", "token", "test-key")
    private val game =
        Game().apply {
            gameId = 2211
            homeTeam = "Ohio State"
            awayTeam = "Michigan"
        }

    @Test
    fun `rollback notice sends only the snake-case game identity with service authentication`() {
        val captured = slot<HttpEntity<GameFinalizationRollbackRequest>>()
        every { http.postForEntity("http://bot/fcfb_discord/game_finalization_rollback", capture(captured), String::class.java) } returns
            ResponseEntity.ok("Finalization rollback reported")
        service.notifyFinalizationRollback(game)
        assertEquals("test-key", captured.captured.headers.getFirst("X-Service-Key"))
        assertEquals(MediaType.APPLICATION_JSON, captured.captured.headers.contentType)
        assertEquals(
            """{"game_id":2211,"home_team":"Ohio State","away_team":"Michigan"}""",
            jacksonObjectMapper().writeValueAsString(captured.captured.body),
        )
    }

    @Test
    fun `notification delivery failure is logged without being retried or rethrown`() {
        every { http.postForEntity(any<String>(), any<HttpEntity<*>>(), String::class.java) } throws
            IllegalStateException("bot unavailable")
        service.notifyFinalizationRollback(game)
        verify(exactly = 1) { http.postForEntity(any<String>(), any<HttpEntity<*>>(), String::class.java) }
    }
}
