package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.GameWriteupService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/game_writeup")
class GameWriteupController(
    private var gameMessagesService: GameWriteupService,
) {
    @Operation(summary = "Get game writeup message")
    @GetMapping("")
    fun getGameMessageByScenario(
        @RequestParam scenario: String,
        @RequestParam playCall: String,
    ) = gameMessagesService.getGameMessageByScenario(scenario, playCall)

    @Operation(summary = "List writeup scenarios")
    @GetMapping("/scenarios")
    fun getScenarios() = gameMessagesService.getDistinctScenarios()
}
