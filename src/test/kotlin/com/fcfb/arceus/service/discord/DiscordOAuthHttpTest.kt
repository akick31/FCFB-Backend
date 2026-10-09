package com.fcfb.arceus.service.discord

import com.fcfb.arceus.config.AppConfig
import com.fcfb.arceus.service.auth.AuthService
import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.service.fcfb.UserService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import kotlin.test.assertEquals

class DiscordOAuthHttpTest {
    @Test
    fun `Discord JSON responses redirect new users to complete registration`() {
        val restTemplate = AppConfig().restTemplate()
        val server = MockRestServiceServer.bindTo(restTemplate).build()
        val authService = mockk<AuthService>()
        every { authService.loginWithDiscord("123456") } returns null
        val service =
            DiscordOAuthService(
                restTemplate = restTemplate,
                sessionService = mockk<SessionService>(),
                userService = mockk<UserService>(),
                authService = authService,
                clientId = "test-client-id",
                clientSecret = "test-client-secret",
                redirectUri = "http://localhost/redirect",
                websiteUrl = "http://localhost",
            )

        server.expect(requestTo("https://discord.com/api/oauth2/token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"access_token":"test-access-token","token_type":"Bearer","expires_in":604800,"scope":"identify"}""",
                    MediaType.APPLICATION_JSON,
                ),
            )
        server.expect(requestTo("https://discord.com/api/users/@me"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer test-access-token"))
            .andRespond(withSuccess("""{"id":"123456","username":"TestUser","avatar":null}""", MediaType.APPLICATION_JSON))

        val response = service.handleRedirect("test-code", null)

        assertEquals(HttpStatus.FOUND, response.statusCode)
        assertEquals("http://localhost/register/complete?discordId=123456&discordTag=TestUser", response.headers.location?.toString())
        server.verify()
    }
}
