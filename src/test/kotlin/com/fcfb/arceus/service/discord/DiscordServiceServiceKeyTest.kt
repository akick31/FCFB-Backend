package com.fcfb.arceus.service.discord

import com.fcfb.arceus.util.ServerUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class DiscordServiceServiceKeyTest {
    @Test
    fun attachesServiceKeyHeaderToBotCalls() =
        runBlocking {
            val restTemplate = mockk<RestTemplate>()
            val captured = slot<HttpEntity<*>>()
            every {
                restTemplate.postForEntity("http://bot/start_game", capture(captured), String::class.java)
            } returns ResponseEntity.ok("thread1,thread2")

            val service = DiscordService(restTemplate, ServerUtils(), "http://bot", "guild", "token", "THE_SERVICE_KEY")
            val result = service.createGameThread(mockk(relaxed = true))

            assertEquals(listOf("thread1", "thread2"), result)
            assertEquals("THE_SERVICE_KEY", captured.captured.headers.getFirst("X-Service-Key"))
        }
}
