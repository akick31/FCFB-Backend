package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.discord.DiscordOAuthService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class DiscordOAuthController(
    private val discordOAuthService: DiscordOAuthService,
) {
    @Operation(summary = "Handle the Discord OAuth redirect callback")
    @GetMapping("${ApiConstants.FULL_PATH}/discord/redirect")
    fun handleDiscordRedirect(
        @RequestParam("code") code: String,
        @RequestParam(required = false) state: String? = null,
    ): ResponseEntity<String> = discordOAuthService.handleRedirect(code, state)
}
