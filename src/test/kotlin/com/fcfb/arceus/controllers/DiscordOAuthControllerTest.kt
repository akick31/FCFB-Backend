package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.discord.DiscordOAuthService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity

class DiscordOAuthControllerTest {
    private val discordOAuthService: DiscordOAuthService = mockk()
    private val controller = DiscordOAuthController(discordOAuthService)

    @Test
    fun `handleDiscordRedirect delegates to DiscordOAuthService`() {
        val redirect = ResponseEntity.status(302).header("Location", "http://localhost/login").build<String>()
        every { discordOAuthService.handleRedirect("test-code", "test-state") } returns redirect

        val response = controller.handleDiscordRedirect("test-code", "test-state")

        assertEquals(redirect, response)
        verify { discordOAuthService.handleRedirect("test-code", "test-state") }
    }
}
