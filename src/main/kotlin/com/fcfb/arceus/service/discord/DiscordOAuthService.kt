package com.fcfb.arceus.service.discord

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fcfb.arceus.service.auth.AuthService
import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.service.fcfb.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class DiscordOAuthService(
    private val restTemplate: RestTemplate,
    private val sessionService: SessionService,
    private val userService: UserService,
    private val authService: AuthService,
    @Value("\${discord.client.id}")
    private val clientId: String,
    @Value("\${discord.client.secret}")
    private val clientSecret: String,
    @Value("\${discord.oauth.redirect}")
    private val redirectUri: String,
    @Value("\${website.url}")
    private val websiteUrl: String,
) {
    private val objectMapper = jacksonObjectMapper()

    fun handleRedirect(
        code: String,
        state: String?,
    ): ResponseEntity<String> {
        val tokenResponse = exchangeCodeForToken(code)
        if (!tokenResponse.statusCode.is2xxSuccessful) {
            return ResponseEntity.status(tokenResponse.statusCode).body("Failed to exchange code for token: ${tokenResponse.body}")
        }

        val accessToken =
            parseAccessToken(tokenResponse.body!!)
                ?: return ResponseEntity.status(500).body("Access token not found in the response")

        val userResponse = fetchDiscordUser(accessToken)
        if (!userResponse.statusCode.is2xxSuccessful) {
            return ResponseEntity.status(userResponse.statusCode).body("Failed to fetch user info: ${userResponse.body}")
        }

        val (discordId, discordTag) = parseDiscordUser(userResponse.body!!)

        if (state != null) {
            return handleLinkAttempt(state, discordId, discordTag)
        }
        return handleLogin(discordId, discordTag)
    }

    private fun exchangeCodeForToken(code: String): ResponseEntity<String> {
        val params: MultiValueMap<String, String> =
            LinkedMultiValueMap<String, String>().apply {
                add("client_id", clientId)
                add("client_secret", clientSecret)
                add("code", code)
                add("grant_type", "authorization_code")
                add("redirect_uri", redirectUri)
                add("scope", "identify")
            }
        val headers = HttpHeaders().apply { set("Content-Type", "application/x-www-form-urlencoded") }
        return restTemplate.exchange(
            "https://discord.com/api/oauth2/token",
            HttpMethod.POST,
            HttpEntity(params, headers),
            String::class.java,
        )
    }

    private fun parseAccessToken(tokenResponseBody: String): String? {
        val tokenResponseMap: Map<String, String> = objectMapper.readValue(tokenResponseBody)
        return tokenResponseMap["access_token"]
    }

    private fun fetchDiscordUser(accessToken: String): ResponseEntity<String> {
        val headers = HttpHeaders().apply { set("Authorization", "Bearer $accessToken") }
        return restTemplate.exchange("https://discord.com/api/users/@me", HttpMethod.GET, HttpEntity("", headers), String::class.java)
    }

    private fun parseDiscordUser(userResponseBody: String): Pair<String, String> {
        val userResponseMap: Map<String, Any> = objectMapper.readValue(userResponseBody)
        return "${userResponseMap["id"]}" to "${userResponseMap["username"]}"
    }

    private fun handleLogin(
        discordId: String,
        discordTag: String,
    ): ResponseEntity<String> {
        val loginResponse = authService.loginWithDiscord(discordId)
        if (loginResponse != null) {
            val token = URLEncoder.encode(loginResponse.token, StandardCharsets.UTF_8)
            return redirectTo(
                "$websiteUrl/login?discordToken=$token&discordUserId=${loginResponse.userId}&discordRole=${loginResponse.role}",
            )
        }
        val encodedTag = URLEncoder.encode(discordTag, StandardCharsets.UTF_8)
        val encodedId = URLEncoder.encode(discordId, StandardCharsets.UTF_8)
        return redirectTo("$websiteUrl/register/complete?discordId=$encodedId&discordTag=$encodedTag")
    }

    private fun handleLinkAttempt(
        state: String,
        discordId: String,
        discordTag: String,
    ): ResponseEntity<String> {
        if (!sessionService.validateToken(state) || sessionService.isSessionBlacklisted(state)) {
            return redirectTo("$websiteUrl/profile?discordError=link_expired")
        }
        val userId = sessionService.extractUserIdFromToken(state)
        return try {
            userService.linkDiscord(userId, discordId, discordTag)
            redirectTo("$websiteUrl/profile?discordLinked=true")
        } catch (e: Exception) {
            redirectTo("$websiteUrl/profile?discordError=${URLEncoder.encode(e.message ?: "link_failed", StandardCharsets.UTF_8)}")
        }
    }

    private fun redirectTo(location: String): ResponseEntity<String> = ResponseEntity.status(302).header("Location", location).build()
}
