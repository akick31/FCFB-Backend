package com.fcfb.arceus.service.discord

import com.fcfb.arceus.dto.standard.SignupInfo
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.util.ServerUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class DiscordServiceServiceKeyTest {
    @ParameterizedTest
    @ValueSource(strings = ["start_game", "delay_of_game", "game_mode", "delay_of_game_warning", "new_signup"])
    fun attachesServiceKeyHeaderToBotCalls(endpoint: String) =
        runBlocking {
            val restTemplate = mockk<RestTemplate>()
            val captured = slot<HttpEntity<*>>()
            val game = mockk<Game>(relaxed = true)
            val signup = SignupInfo("coach", "123", "Team One", "Team Two", "Team Three")
            val path =
                when (endpoint) {
                    "delay_of_game" -> "$endpoint?isDelayOfGameOut=true"
                    "delay_of_game_warning" -> "$endpoint?instance=2"
                    else -> endpoint
                }
            every {
                restTemplate.postForEntity("http://bot/$path", capture(captured), String::class.java)
            } returns ResponseEntity.ok("thread1,thread2")

            val service = DiscordService(restTemplate, ServerUtils(), "http://bot", "guild", "token", "THE_SERVICE_KEY")
            when (endpoint) {
                "start_game" -> assertEquals(listOf("thread1", "thread2"), service.createGameThread(game))
                "delay_of_game" -> service.notifyDelayOfGame(game, true)
                "game_mode" -> service.notifyGameModeChange(game)
                "delay_of_game_warning" -> assertEquals(true, service.notifyWarning(game, 2))
                "new_signup" -> service.sendRegistrationNotice(signup)
            }

            assertEquals("THE_SERVICE_KEY", captured.captured.headers.getFirst("X-Service-Key"))
            assertEquals(MediaType.APPLICATION_JSON, captured.captured.headers.contentType)
            assertSame(if (endpoint == "new_signup") signup else game, captured.captured.body)
        }
}
