package com.fcfb.arceus.service.discord

import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.util.ServerUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class DiscordServiceCancellationTest {
    @Test
    fun `thread creation propagates cancellation without null fallback or retries`() =
        runBlocking {
            val restTemplate = mockk<RestTemplate>()
            val cancellation = CancellationException("Cancelled")
            every { restTemplate.postForEntity(any<String>(), any(), String::class.java) } throws cancellation
            val service = DiscordService(restTemplate, ServerUtils(), "http://localhost", "guild", "token", "key")
            assertSame(cancellation, assertFailsWith<CancellationException> { service.createGameThread(Game()) })
            verify(exactly = 1) { restTemplate.postForEntity(any<String>(), any(), String::class.java) }
        }
}
