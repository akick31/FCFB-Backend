package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.GameWeekJob
import com.fcfb.arceus.dto.GameWeekJobResponse
import com.fcfb.arceus.dto.StartRequest
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.service.fcfb.GameService
import com.fcfb.arceus.service.specification.GameSpecificationService.GameCategory
import com.fcfb.arceus.service.specification.GameSpecificationService.GameFilter
import com.fcfb.arceus.service.specification.GameSpecificationService.GameSort
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
    @GetMapping("/{id}")
    fun getOngoingGameById(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameById(id))

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
                filters = filters ?: emptyList(),
                category = category,
                conference = conference,
                season = season,
                week = week,
                gameMode = gameMode,
                sort = sort,
                pageable = pageable,
            ),
        )

    @PostMapping
    suspend fun startGame(
        @RequestBody startRequest: StartRequest,
    ): ResponseEntity<Game> = ResponseEntity.status(201).body(gameService.startSingleGame(startRequest, null))

    @PostMapping("/overtime")
    suspend fun startOvertimeGame(
        @RequestBody startRequest: StartRequest,
    ): ResponseEntity<Game> = ResponseEntity.status(201).body(gameService.startOvertimeGame(startRequest))

    @PostMapping("/week")
    fun startWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<GameWeekJobResponse> = ResponseEntity.status(202).body(gameService.startWeekAsync(season, week))

    @GetMapping("/week/status/{jobId}")
    fun getGameWeekJobStatus(
        @PathVariable("jobId") jobId: String,
    ): ResponseEntity<GameWeekJob> {
        val job =
            gameService.getGameWeekJobStatus(jobId)
                ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    @GetMapping("/week/jobs")
    fun getAllGameWeekJobs(): ResponseEntity<List<GameWeekJob>> = ResponseEntity.ok(gameService.getAllGameWeekJobs())

    @GetMapping("/rankings-history")
    fun getRankingsHistory(
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) season: Int?,
    ): ResponseEntity<List<Game>> {
        val games = gameService.getRankingsHistory(team, season)
        return ResponseEntity.ok(games)
    }

    @PostMapping("/week/retry/{jobId}")
    fun retryFailedGames(
        @PathVariable("jobId") jobId: String,
    ): ResponseEntity<GameWeekJobResponse> = ResponseEntity.status(202).body(gameService.retryFailedGames(jobId))

    @PostMapping("/end")
    fun endGameByChannelId(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.endSingleGameByChannelId(channelId))

    @PostMapping("{gameId}/end")
    fun endGameByGameId(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.endSingleGameByGameId(gameId))

    @PostMapping("/end-all")
    fun endAllGames(): ResponseEntity<List<Game>> = ResponseEntity.ok(gameService.endAllGames())

    @PostMapping("/chew")
    fun chewGameByPlatformId(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.chewGame(gameService.getGameByPlatformId(channelId)))

    @PostMapping("{gameId}/chew")
    fun chewGameByGameId(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.chewGame(gameService.getGameById(gameId)))

    @PutMapping("/{gameId}/coin-toss")
    fun runCoinToss(
        @PathVariable("gameId") gameId: String,
        @RequestParam("coinTossCall") coinTossCall: CoinTossCall,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.runCoinToss(gameId, coinTossCall))

    @PutMapping("/{gameId}/coin-toss-choice")
    fun makeCoinTossChoice(
        @PathVariable("gameId") gameId: String,
        @RequestParam("coinTossChoice") coinTossChoice: CoinTossChoice,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.makeCoinTossChoice(gameId, coinTossChoice))

    @PutMapping("/{gameId}/overtime-coin-toss-choice")
    fun makeOvertimeCoinTossChoice(
        @PathVariable("gameId") gameId: String,
        @RequestParam("coinTossChoice") coinTossChoice: OvertimeCoinTossChoice,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.makeOvertimeCoinTossChoice(gameId, coinTossChoice))

    @PutMapping("/{gameId}/request-message")
    fun updateRequestMessageId(
        @PathVariable("gameId") gameId: Int,
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateRequestMessageId(gameId, requestMessageId))

    @PutMapping("/{gameId}/last-message-timestamp")
    fun updateLastMessageTimestamp(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateLastMessageTimestamp(gameId))

    @GetMapping("/request-message")
    fun getGameByRequestMessageId(
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameByRequestMessageId("\"$requestMessageId\""))

    @GetMapping("/platform")
    fun getGameByPlatformId(
        @RequestParam("platformId") platformId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.getGameByPlatformId(platformId))

    @PutMapping("/{gameId}/sub")
    fun subCoachIntoGame(
        @PathVariable("gameId") gameId: Int,
        @RequestParam("team") team: String,
        @RequestParam("discordId") discordId: String,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.subCoachIntoGame(gameId, team, discordId))

    @PostMapping("/restart")
    suspend fun restartGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.restartGame(channelId))

    @PutMapping("/{gameId}/close-game-pinged")
    fun markCloseGamePinged(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markCloseGamePinged(gameId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{gameId}/upset-alert-pinged")
    fun markUpsetAlertPinged(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markUpsetAlertPinged(gameId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("")
    fun updateGame(
        @RequestBody game: Game,
    ): ResponseEntity<Game> = ResponseEntity.ok(gameService.updateGame(game))

    @DeleteMapping("")
    fun deleteOngoingGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(gameService.deleteOngoingGame(channelId))
}
