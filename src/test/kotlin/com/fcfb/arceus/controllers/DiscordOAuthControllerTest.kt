package com.fcfb.arceus.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fcfb.arceus.dto.response.LoginResponse
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.service.auth.AuthService
import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.service.fcfb.UserService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

class DiscordOAuthControllerTest {
    private val restTemplate: RestTemplate = mockk()
    private val sessionService: SessionService = mockk()
    private val userService: UserService = mockk()
    private val authService: AuthService = mockk()

    private val controller =
        DiscordOAuthController(
            restTemplate = restTemplate,
            sessionService = sessionService,
            userService = userService,
            authService = authService,
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
            redirectUri = "http://localhost/redirect",
            websiteUrl = "http://localhost",
        )

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should redirect successfully with discord user info`() {
        val code = "test-code"
        val accessToken = "test-access-token"
        val discordTag = "TestUser"
        val discordId = "123456"

        val tokenResponseBody = objectMapper.writeValueAsString(mapOf("access_token" to accessToken))
        val userResponseBody = objectMapper.writeValueAsString(mapOf("username" to "TestUser", "id" to discordId))

        every {
            restTemplate.exchange(
                "https://discord.com/api/oauth2/token",
                HttpMethod.POST,
                any(),
                String::class.java,
            )
        } returns ResponseEntity(tokenResponseBody, HttpStatus.OK)

        every {
            restTemplate.exchange(
                "https://discord.com/api/users/@me",
                HttpMethod.GET,
                any(),
                String::class.java,
            )
        } returns ResponseEntity(userResponseBody, HttpStatus.OK)

        every { authService.loginWithDiscord(discordId) } returns null

        mockkStatic(RestTemplate::class)
        every { RestTemplate() } returns restTemplate

        val response = controller.handleDiscordRedirect(code)

        assertEquals(HttpStatus.FOUND, response.statusCode)
        assertEquals(
            "http://localhost/register/complete?discordId=$discordId&discordTag=$discordTag",
            response.headers["Location"]?.first(),
        )
    }

    @Test
    fun `should log in and redirect when discord id is already linked to a user`() {
        val code = "test-code"
        val accessToken = "test-access-token"
        val discordTag = "TestUser"
        val discordId = "123456"

        val tokenResponseBody = objectMapper.writeValueAsString(mapOf("access_token" to accessToken))
        val userResponseBody = objectMapper.writeValueAsString(mapOf("username" to "TestUser", "id" to discordId))

        every {
            restTemplate.exchange(
                "https://discord.com/api/oauth2/token",
                HttpMethod.POST,
                any(),
                String::class.java,
            )
        } returns ResponseEntity(tokenResponseBody, HttpStatus.OK)

        every {
            restTemplate.exchange(
                "https://discord.com/api/users/@me",
                HttpMethod.GET,
                any(),
                String::class.java,
            )
        } returns ResponseEntity(userResponseBody, HttpStatus.OK)

        every { authService.loginWithDiscord(discordId) } returns LoginResponse("test-session-token", 42L, UserRole.USER)

        mockkStatic(RestTemplate::class)
        every { RestTemplate() } returns restTemplate

        val response = controller.handleDiscordRedirect(code)

        assertEquals(HttpStatus.FOUND, response.statusCode)
        assertEquals(
            "http://localhost/login?discordToken=test-session-token&discordUserId=42&discordRole=USER",
            response.headers["Location"]?.first(),
        )
    }

    @Test
    fun `should return error when access token is missing`() {
        val code = "test-code"
        val tokenResponseBody = objectMapper.writeValueAsString(emptyMap<String, String>())

        every {
            restTemplate.exchange(
                "https://discord.com/api/oauth2/token",
                HttpMethod.POST,
                any(),
                String::class.java,
            )
        } returns ResponseEntity(tokenResponseBody, HttpStatus.OK)

        mockkStatic(RestTemplate::class)
        every { RestTemplate() } returns restTemplate

        val response = controller.handleDiscordRedirect(code)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Access token not found in the response", response.body)
    }

    @Test
    fun `should return error when token exchange fails`() {
        val code = "test-code"

        every {
            restTemplate.exchange(
                "https://discord.com/api/oauth2/token",
                HttpMethod.POST,
                any(),
                String::class.java,
            )
        } returns ResponseEntity("Error exchanging token", HttpStatus.BAD_REQUEST)

        mockkStatic(RestTemplate::class)
        every { RestTemplate() } returns restTemplate

        val response = controller.handleDiscordRedirect(code)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Failed to exchange code for token: Error exchanging token", response.body)
    }

    @Test
    fun `should return error when fetching user info fails`() {
        val code = "test-code"
        val accessToken = "test-access-token"
        val tokenResponseBody = objectMapper.writeValueAsString(mapOf("access_token" to accessToken))

        every {
            restTemplate.exchange(
                "https://discord.com/api/oauth2/token",
                HttpMethod.POST,
                any(),
                String::class.java,
            )
        } returns ResponseEntity(tokenResponseBody, HttpStatus.OK)

        every {
            restTemplate.exchange(
                "https://discord.com/api/users/@me",
                HttpMethod.GET,
                any(),
                String::class.java,
            )
        } returns ResponseEntity("Error fetching user info", HttpStatus.BAD_REQUEST)

        mockkStatic(RestTemplate::class)
        every { RestTemplate() } returns restTemplate

        val response = controller.handleDiscordRedirect(code)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Failed to fetch user info: Error fetching user info", response.body)
    }
}
