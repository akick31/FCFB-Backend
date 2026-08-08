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
    @Operation(summary = "Get ongoing game by ID")
    @GetMapping(params = ["id"])
    fun getOngoingGameById(
        @RequestParam("id") id: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameById(id))

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

    @Operation(summary = "Start game")
    @PostMapping
    suspend fun startGame(
        @RequestBody startRequest: StartRequest,
    ): ResponseEntity<Game> = ResponseEntity.status(201).body(gameService.startSingleGame(startRequest, null))

    @Operation(summary = "Start overtime game")
    @PostMapping("/overtime")
    suspend fun startOvertimeGame(
        @RequestBody startRequest: StartRequest,
    ): ResponseEntity<Game> = ResponseEntity.status(201).body(gameService.startOvertimeGame(startRequest))

    @Operation(summary = "Start week's games")
    @PostMapping("/week")
    fun startWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<GameWeekJobResponse> = ResponseEntity.status(202).body(gameService.startWeekAsync(season, week))

    @Operation(summary = "Get game week job status")
    @GetMapping("/week/status")
    fun getGameWeekJobStatus(
        @RequestParam("jobId") jobId: String,
    ): ResponseEntity<GameWeekJob> = ResponseEntity.ok(gameService.getGameWeekJobStatus(jobId))

    @Operation(summary = "List game week jobs")
    @GetMapping("/week/jobs")
    fun getAllGameWeekJobs(): ResponseEntity<List<GameWeekJob>> = ResponseEntity.ok(gameService.getAllGameWeekJobs())

    @Operation(summary = "Team season rankings history")
    @GetMapping("/rankings-history")
    fun getRankingsHistory(
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) season: Int?,
    ): ResponseEntity<List<Game>> = ResponseEntity.ok(gameService.getRankingsHistory(team, season))

    @Operation(summary = "Retry failed games")
    @PostMapping("/week/retry")
    fun retryFailedGames(
        @RequestParam("jobId") jobId: String,
    ): ResponseEntity<GameWeekJobResponse> = ResponseEntity.status(202).body(gameService.retryFailedGames(jobId))

    @Operation(summary = "End game by channel ID")
    @PostMapping("/end", params = ["channelId"])
    fun endGameByChannelId(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.endSingleGameByChannelId(channelId))

    @Operation(summary = "End game by ID")
    @PostMapping("/end", params = ["gameId"])
    fun endGameByGameId(
        @RequestParam("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.endSingleGameByGameId(gameId))

    @Operation(summary = "End all ongoing games")
    @PostMapping("/end-all")
    fun endAllGames(): ResponseEntity<List<Game>> = ResponseEntity.ok(gameService.endAllGames())

    @Operation(summary = "Chew clock by channel ID")
    @PostMapping("/chew", params = ["channelId"])
    fun chewGameByPlatformId(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.chewGameByPlatformId(channelId))

    @Operation(summary = "Chew clock by game ID")
    @PostMapping("/chew", params = ["gameId"])
    fun chewGameByGameId(
        @RequestParam("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.chewGameByGameId(gameId))

    @Operation(summary = "Chew clock on all games")
    @PostMapping("/chew-all")
    fun chewAllGames(): ResponseEntity<List<Game>> = ResponseEntity.ok(gameService.chewAllGames())

    @Operation(summary = "Run coin toss")
    @PutMapping("/coin-toss")
    fun runCoinToss(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("coinTossCall") coinTossCall: CoinTossCall,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.runCoinToss(gameId, coinTossCall))

    @Operation(summary = "Record coin toss choice")
    @PutMapping("/coin-toss-choice")
    fun makeCoinTossChoice(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("coinTossChoice") coinTossChoice: CoinTossChoice,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.makeCoinTossChoice(gameId, coinTossChoice))

    @Operation(summary = "Record overtime coin toss choice")
    @PutMapping("/overtime-coin-toss-choice")
    fun makeOvertimeCoinTossChoice(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("coinTossChoice") coinTossChoice: OvertimeCoinTossChoice,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.makeOvertimeCoinTossChoice(gameId, coinTossChoice))

    @Operation(summary = "Update request message ID")
    @PutMapping("/request-message")
    fun updateRequestMessageId(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateRequestMessageId(gameId, requestMessageId))

    @Operation(summary = "Update last message timestamp")
    @PutMapping("/last-message-timestamp")
    fun updateLastMessageTimestamp(
        @RequestParam("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateLastMessageTimestamp(gameId))

    @Operation(summary = "Get game by request message ID")
    @GetMapping("/request-message")
    fun getGameByRequestMessageId(
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameByRequestMessageId(requestMessageId))

    @Operation(summary = "Get game by channel ID")
    @GetMapping("/platform")
    fun getGameByPlatformId(
        @RequestParam("platformId") platformId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameByPlatformId(platformId))

    @Operation(summary = "Substitute coach into game")
    @PutMapping("/sub")
    fun subCoachIntoGame(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("team") team: String,
        @RequestParam("discordId") discordId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.subCoachIntoGame(gameId, team, discordId))

    @Operation(summary = "Restart game by channel ID")
    @PostMapping("/restart")
    suspend fun restartGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.restartGame(channelId))

    @Operation(summary = "Mark close game ping sent")
    @PutMapping("/close-game-pinged")
    fun markCloseGamePinged(
        @RequestParam("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markCloseGamePinged(gameId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Mark upset alert ping sent")
    @PutMapping("/upset-alert-pinged")
    fun markUpsetAlertPinged(
        @RequestParam("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markUpsetAlertPinged(gameId)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Update game")
    @PutMapping("")
    fun updateGame(
        @RequestBody game: Game,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateGame(game))

    @Operation(summary = "Delete game by channel ID")
    @DeleteMapping("")
    fun deleteOngoingGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(gameService.deleteOngoingGame(channelId))
}
