package com.fcfb.arceus.controllers.game

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.service.fcfb.game.GameCreationService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/game-creation")
class GameCreationController(
    private val service: GameCreationService,
) {
    @Operation(summary = "Start game")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun startGame(
        @RequestBody startRequest: StartRequest,
    ): Game = service.startSingleGame(startRequest, null)

    @Operation(summary = "Start overtime game")
    @PostMapping("/overtime")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun startOvertimeGame(
        @RequestBody startRequest: StartRequest,
    ): Game = service.startOvertimeGame(startRequest)

    @Operation(summary = "Restart game by channel ID")
    @PostMapping("/restart")
    suspend fun restartGame(
        @RequestParam("channelId") channelId: String,
    ): Game = service.restartGame(channelId)
}
