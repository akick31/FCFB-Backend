package com.fcfb.arceus.controllers

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
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RestController
class DiscordOAuthController(
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

    @GetMapping("${ApiConstants.FULL_PATH}/discord/redirect")
    fun handleDiscordRedirect(
        @RequestParam("code") code: String,
        @RequestParam(required = false) state: String? = null,
    ): ResponseEntity<String> {
        val tokenUrl = "https://discord.com/api/oauth2/token"

        val params: MultiValueMap<String, String> =
            LinkedMultiValueMap<String, String>().apply {
                add("client_id", clientId)
                add("client_secret", clientSecret)
                add("code", code)
                add("grant_type", "authorization_code")
                add("redirect_uri", redirectUri)
                add("scope", "identify")
            }

        val headers =
            HttpHeaders().apply {
                set("Content-Type", "application/x-www-form-urlencoded")
            }
        val entity = HttpEntity(params, headers)

        val tokenResponse = restTemplate.exchange(tokenUrl, HttpMethod.POST, entity, String::class.java)

        if (tokenResponse.statusCode.is2xxSuccessful) {
            val tokenResponseBody = tokenResponse.body
            val tokenResponseMap: Map<String, String> = objectMapper.readValue(tokenResponseBody!!)
            val accessToken = tokenResponseMap["access_token"]

            if (accessToken != null) {
                val userUrl = "https://discord.com/api/users/@me"
                val userResponse =
                    restTemplate.exchange(
                        userUrl,
                        HttpMethod.GET,
                        HttpEntity(
                            "",
                            HttpHeaders().apply {
                                set("Authorization", "Bearer $accessToken")
                            },
                        ),
                        String::class.java,
                    )

                if (userResponse.statusCode.is2xxSuccessful) {
                    val userResponseBody = userResponse.body
                    val userResponseMap: Map<String, Any> = objectMapper.readValue(userResponseBody!!)
                    val rawDiscordTag = "${userResponseMap["username"]}"
                    val rawDiscordId = "${userResponseMap["id"]}"

                    if (state != null) {
                        return handleLinkAttempt(state, rawDiscordId, rawDiscordTag)
                    }

                    val loginResponse = authService.loginWithDiscord(rawDiscordId)
                    if (loginResponse != null) {
                        val token = URLEncoder.encode(loginResponse.token, StandardCharsets.UTF_8)
                        return redirectTo(
                            "$websiteUrl/login?discordToken=$token&discordUserId=${loginResponse.userId}&discordRole=${loginResponse.role}",
                        )
                    }

                    val discordTag = URLEncoder.encode(rawDiscordTag, StandardCharsets.UTF_8)
                    val discordId = URLEncoder.encode(rawDiscordId, StandardCharsets.UTF_8)

                    return redirectTo("$websiteUrl/register/complete?discordId=$discordId&discordTag=$discordTag")
                } else {
                    return ResponseEntity.status(userResponse.statusCode).body("Failed to fetch user info: ${userResponse.body}")
                }
            } else {
                return ResponseEntity.status(500).body("Access token not found in the response")
            }
        } else {
            return ResponseEntity.status(tokenResponse.statusCode).body("Failed to exchange code for token: ${tokenResponse.body}")
        }
    }

    /**
     * Completes a "link Discord to my existing account" flow. [state] is the requesting user's own
     * session token, passed through the OAuth `state` param and echoed back by Discord — this is how
     * the redirect (a plain browser navigation, not an authenticated API call) knows which logged-in
     * user initiated it.
     */
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
