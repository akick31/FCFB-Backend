package com.fcfb.arceus.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

    private val controller =
        DiscordOAuthController(
            restTemplate = restTemplate,
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
