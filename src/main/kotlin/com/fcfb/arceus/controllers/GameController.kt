package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.dto.response.GameWeekJob
import com.fcfb.arceus.dto.response.GameWeekJobResponse
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter
import com.fcfb.arceus.service.specification.GameSpecificationService.GameSort
import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/game")
class GameController(
    private val gameService: GameService,
) {
    @Operation(summary = "Get an ongoing game by its ID")
    @GetMapping("/{id}")
    fun getOngoingGameById(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameById(id))

    @Operation(summary = "List games matching the given filters, category, and sort order")
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
    ): ResponseEntity<Page<Game>> =
        ResponseEntity.ok(
            gameService.getFilteredGames(
                filters = filters,
                category = category,
                conference = conference,
                season = season,
                week = week,
                gameMode = gameMode,
                sort = sort,
                pageable = pageable,
            ),
        )

    @Operation(summary = "Start a new game")
    @PostMapping
    suspend fun startGame(
        @RequestBody startRequest: StartRequest,
    ): ResponseEntity<Game> = ResponseEntity.status(201).body(gameService.startSingleGame(startRequest, null))

    @Operation(summary = "Start a new overtime game")
    @PostMapping("/overtime")
    suspend fun startOvertimeGame(
        @RequestBody startRequest: StartRequest,
    ): ResponseEntity<Game> = ResponseEntity.status(201).body(gameService.startOvertimeGame(startRequest))

    @Operation(summary = "Start all games for a given season and week")
    @PostMapping("/week")
    fun startWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<GameWeekJobResponse> = ResponseEntity.status(202).body(gameService.startWeekAsync(season, week))

    @Operation(summary = "Get the status of a game week job by its ID")
    @GetMapping("/week/status/{jobId}")
    fun getGameWeekJobStatus(
        @PathVariable("jobId") jobId: String,
    ): ResponseEntity<GameWeekJob> = ResponseEntity.ok(gameService.getGameWeekJobStatus(jobId))

    @Operation(summary = "List all game week jobs")
    @GetMapping("/week/jobs")
    fun getAllGameWeekJobs(): ResponseEntity<List<GameWeekJob>> = ResponseEntity.ok(gameService.getAllGameWeekJobs())

    @Operation(summary = "Get a team's rankings history for a season")
    @GetMapping("/rankings-history")
    fun getRankingsHistory(
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) season: Int?,
    ): ResponseEntity<List<Game>> = ResponseEntity.ok(gameService.getRankingsHistory(team, season))

    @Operation(summary = "Retry the failed games in a game week job")
    @PostMapping("/week/retry/{jobId}")
    fun retryFailedGames(
        @PathVariable("jobId") jobId: String,
    ): ResponseEntity<GameWeekJobResponse> = ResponseEntity.status(202).body(gameService.retryFailedGames(jobId))

    @Operation(summary = "End a game by its channel ID")
    @PostMapping("/end")
    fun endGameByChannelId(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.endSingleGameByChannelId(channelId))

    @Operation(summary = "End a game by its game ID")
    @PostMapping("{gameId}/end")
    fun endGameByGameId(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.endSingleGameByGameId(gameId))

    @Operation(summary = "End all ongoing games")
    @PostMapping("/end-all")
    fun endAllGames(): ResponseEntity<List<Game>> = ResponseEntity.ok(gameService.endAllGames())

    @Operation(summary = "Chew the clock on a game by its platform channel ID")
    @PostMapping("/chew")
    fun chewGameByPlatformId(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.chewGameByPlatformId(channelId))

    @Operation(summary = "Chew the clock on a game by its game ID")
    @PostMapping("{gameId}/chew")
    fun chewGameByGameId(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.chewGameByGameId(gameId))

    @Operation(summary = "Chew the clock on all ongoing games")
    @PostMapping("/chew-all")
    fun chewAllGames(): ResponseEntity<List<Game>> = ResponseEntity.ok(gameService.chewAllGames())

    @Operation(summary = "Run the coin toss for a game")
    @PutMapping("/{gameId}/coin-toss")
    fun runCoinToss(
        @PathVariable("gameId") gameId: String,
        @RequestParam("coinTossCall") coinTossCall: CoinTossCall,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.runCoinToss(gameId, coinTossCall))

    @Operation(summary = "Record a coach's coin toss choice for a game")
    @PutMapping("/{gameId}/coin-toss-choice")
    fun makeCoinTossChoice(
        @PathVariable("gameId") gameId: String,
        @RequestParam("coinTossChoice") coinTossChoice: CoinTossChoice,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.makeCoinTossChoice(gameId, coinTossChoice))

    @Operation(summary = "Record a coach's overtime coin toss choice for a game")
    @PutMapping("/{gameId}/overtime-coin-toss-choice")
    fun makeOvertimeCoinTossChoice(
        @PathVariable("gameId") gameId: String,
        @RequestParam("coinTossChoice") coinTossChoice: OvertimeCoinTossChoice,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.makeOvertimeCoinTossChoice(gameId, coinTossChoice))

    @Operation(summary = "Update the request message ID for a game")
    @PutMapping("/{gameId}/request-message")
    fun updateRequestMessageId(
        @PathVariable("gameId") gameId: Int,
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateRequestMessageId(gameId, requestMessageId))

    @Operation(summary = "Update the last message timestamp for a game")
    @PutMapping("/{gameId}/last-message-timestamp")
    fun updateLastMessageTimestamp(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateLastMessageTimestamp(gameId))

    @Operation(summary = "Get a game by its request message ID")
    @GetMapping("/request-message")
    fun getGameByRequestMessageId(
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameByRequestMessageId(requestMessageId))

    @Operation(summary = "Get a game by its platform channel ID")
    @GetMapping("/platform")
    fun getGameByPlatformId(
        @RequestParam("platformId") platformId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameByPlatformId(platformId))

    @Operation(summary = "Substitute a coach into a game")
    @PutMapping("/{gameId}/sub")
    fun subCoachIntoGame(
        @PathVariable("gameId") gameId: Int,
        @RequestParam("team") team: String,
        @RequestParam("discordId") discordId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.subCoachIntoGame(gameId, team, discordId))

    @Operation(summary = "Restart a game by its channel ID")
    @PostMapping("/restart")
    suspend fun restartGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.restartGame(channelId))

    @Operation(summary = "Mark a game as having sent its close game ping")
    @PutMapping("/{gameId}/close-game-pinged")
    fun markCloseGamePinged(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markCloseGamePinged(gameId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Mark a game as having sent its upset alert ping")
    @PutMapping("/{gameId}/upset-alert-pinged")
    fun markUpsetAlertPinged(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markUpsetAlertPinged(gameId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Update an existing game")
    @PutMapping("")
    fun updateGame(
        @RequestBody game: Game,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateGame(game))

    @Operation(summary = "Delete an ongoing game by its channel ID")
    @DeleteMapping("")
    fun deleteOngoingGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(gameService.deleteOngoingGame(channelId))
}
