package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.game.GameCategory
import com.fcfb.arceus.enums.game.GameFilter
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameSort
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.service.fcfb.game.GameService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/game")
class GameController(
    private val gameService: GameService,
) {
    @Operation(summary = "Get ongoing game by ID")
    @GetMapping("/ongoing")
    fun getOngoingGameById(
        @RequestParam("id") id: Int,
    ): Game = gameService.getGameById(id)

    @Operation(summary = "Get games by ID")
    @PostMapping("/batch")
    fun getGamesByIds(
        @RequestBody gameIds: List<Int>,
    ): List<Game> = gameService.getGamesByIds(gameIds)

    @Operation(summary = "List games")
    @GetMapping
    fun getFilteredGames(
        @RequestParam(required = false) filters: List<GameFilter>?,
        @RequestParam(required = false) category: GameCategory?,
        @RequestParam(defaultValue = "CLOSEST_TO_END") sort: GameSort,
        @RequestParam(required = false) conference: String?,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) week: Int?,
        @RequestParam(required = false) gameMode: GameMode?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<Game> =
        gameService.getFilteredGames(
            filters = filters,
            category = category,
            conference = conference,
            season = season,
            week = week,
            gameMode = gameMode,
            sort = sort,
            pageable = pageable,
        )

    @Operation(summary = "Chew clock by channel ID")
    @PostMapping("/chew", params = ["channelId"])
    fun chewGameByPlatformId(
        @RequestParam("channelId") channelId: ULong,
    ): Game = gameService.chewGameByPlatformId(channelId)

    @Operation(summary = "Chew clock by game ID")
    @PostMapping("/chew", params = ["gameId"])
    fun chewGameByGameId(
        @RequestParam("gameId") gameId: Int,
    ): Game = gameService.chewGameByGameId(gameId)

    @Operation(summary = "Chew clock on all games")
    @PostMapping("/chew-all")
    fun chewAllGames(): List<Game> = gameService.chewAllGames()

    @Operation(summary = "Return a game to normal clock by channel ID")
    @PostMapping("/unchew", params = ["channelId"])
    fun unchewGameByPlatformId(
        @RequestParam("channelId") channelId: ULong,
    ): Game = gameService.unchewGameByPlatformId(channelId)

    @Operation(summary = "Return a game to normal clock by game ID")
    @PostMapping("/unchew", params = ["gameId"])
    fun unchewGameByGameId(
        @RequestParam("gameId") gameId: Int,
    ): Game = gameService.unchewGameByGameId(gameId)

    @Operation(summary = "Return all games to normal clock")
    @PostMapping("/unchew-all")
    fun unchewAllGames(): List<Game> = gameService.unchewAllGames()

    @Operation(summary = "Update request message ID")
    @PutMapping("/request-message")
    fun updateRequestMessageId(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("requestMessageId") requestMessageId: String,
    ): Game = gameService.updateRequestMessageId(gameId, requestMessageId)

    @Operation(summary = "Update last message timestamp")
    @PutMapping("/last-message-timestamp")
    fun updateLastMessageTimestamp(
        @RequestParam("gameId") gameId: Int,
    ): Game = gameService.updateLastMessageTimestamp(gameId)

    @Operation(
        summary =
            "Reset the delay of game timer without recording a play, " +
                "for use when a Discord notification failed to send",
    )
    @PutMapping("/reset-delay-of-game-timer")
    fun resetDelayOfGameTimer(
        @RequestParam("gameId") gameId: Int,
    ): Game = gameService.resetDelayOfGameTimer(gameId)

    @Operation(summary = "Get game by request message ID")
    @GetMapping("/request-message")
    fun getGameByRequestMessageId(
        @RequestParam("requestMessageId") requestMessageId: String,
    ): Game = gameService.getGameByRequestMessageId(requestMessageId)

    @Operation(summary = "Get game by channel ID")
    @GetMapping("/platform")
    fun getGameByPlatformId(
        @RequestParam("platformId") platformId: ULong,
    ): Game = gameService.getGameByPlatformId(platformId)

    @Operation(summary = "Substitute coach into game")
    @PutMapping("/sub")
    fun subCoachIntoGame(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("team") team: String,
        @RequestParam("discordId") discordId: String,
    ): Game = gameService.subCoachIntoGame(gameId, team, discordId)

    @Operation(summary = "Mark close game ping sent")
    @PutMapping("/close-game-pinged")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markCloseGamePinged(
        @RequestParam("gameId") gameId: Int,
    ) = gameService.markCloseGamePinged(gameId)

    @Operation(summary = "Mark upset alert ping sent")
    @PutMapping("/upset-alert-pinged")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markUpsetAlertPinged(
        @RequestParam("gameId") gameId: Int,
    ) = gameService.markUpsetAlertPinged(gameId)

    @Operation(summary = "Update game")
    @PutMapping("")
    fun updateGame(
        @RequestBody game: Game,
    ): Game = gameService.updateGame(game)

    @Operation(summary = "Delete game by channel ID")
    @DeleteMapping("")
    fun deleteOngoingGame(
        @RequestParam("channelId") channelId: ULong,
    ): Boolean = gameService.deleteOngoingGame(channelId)
}
