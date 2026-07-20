package com.fcfb.arceus.controllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
                    val discordTag = URLEncoder.encode("${userResponseMap["username"]}", StandardCharsets.UTF_8)
                    val discordId = URLEncoder.encode("${userResponseMap["id"]}", StandardCharsets.UTF_8)

                    return ResponseEntity.status(302)
                        .header(
                            "Location",
                            "$websiteUrl/register/complete?" +
                                "discordId=$discordId&discordTag=$discordTag",
                        )
                        .build()
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
}
