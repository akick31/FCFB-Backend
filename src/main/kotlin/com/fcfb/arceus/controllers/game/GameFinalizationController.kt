package com.fcfb.arceus.controllers.game

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.service.fcfb.game.GameFinalizationService
import com.fcfb.arceus.service.fcfb.game.GameReopenService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/game-finalization")
class GameFinalizationController(
    private val service: GameFinalizationService,
    private val reopenService: GameReopenService,
) {
    @Operation(summary = "End game by channel ID")
    @PostMapping("", params = ["channelId"])
    fun endGameByChannelId(
        @RequestParam("channelId") channelId: ULong,
    ): Game = service.endSingleGameByChannelId(channelId)

    @Operation(summary = "End game by ID")
    @PostMapping("", params = ["gameId"])
    fun endGameByGameId(
        @RequestParam("gameId") gameId: Int,
    ): Game = service.endSingleGameByGameId(gameId)

    @Operation(summary = "End all ongoing games")
    @PostMapping("/all")
    fun endAllGames(): List<Game> = service.endAllGames()

    @Operation(summary = "Reopen a game that ended on a final touchdown")
    @PostMapping("/reopen")
    fun reopenGame(
        @RequestParam("gameId") gameId: Int,
    ): Game = reopenService.reopenGame(gameId)
}
