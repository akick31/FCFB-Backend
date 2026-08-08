package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.PlayService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/play")
class PlayController(
    private var playService: PlayService,
) {
    @Operation(summary = "Submit defensive number")
    @PostMapping("/submit_defense")
    fun defensiveNumberSubmitted(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("defensiveSubmitter") defensiveSubmitter: String,
        @RequestParam("defensiveSubmitterId") defensiveSubmitterId: String,
        @RequestParam("defensiveNumber") defensiveNumber: Int,
        @RequestParam("timeoutCalled") timeoutCalled: Boolean = false,
    ) = playService.defensiveNumberSubmitted(gameId, defensiveSubmitter, defensiveSubmitterId, defensiveNumber, timeoutCalled)

    @Operation(summary = "Submit offensive play call")
    @PutMapping("/submit_offense")
    fun offensiveNumberSubmitted(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("offensiveSubmitter") offensiveSubmitter: String,
        @RequestParam("offensiveSubmitterId") offensiveSubmitterId: String,
        @RequestParam("offensiveNumber") offensiveNumber: Int?,
        @RequestParam("playCall") playCall: PlayCall,
        @RequestParam("runoffType") runoffType: RunoffType,
        @RequestParam("timeoutCalled") timeoutCalled: Boolean,
    ) = playService.offensiveNumberSubmitted(
        gameId,
        offensiveSubmitter,
        offensiveSubmitterId,
        offensiveNumber,
        playCall,
        runoffType,
        timeoutCalled,
    )

    @Operation(summary = "Roll back last play")
    @PutMapping("/rollback")
    fun rollbackPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.rollbackPlay(gameId)

    @Operation(summary = "Get play by ID")
    @GetMapping("")
    fun getPlayById(
        @RequestParam("playId") playId: Int,
    ) = playService.getPlayById(playId)

    @Operation(summary = "Get previous play")
    @GetMapping("/previous")
    fun getPreviousPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getPreviousPlay(gameId)

    @Operation(summary = "Get current play")
    @GetMapping("/current")
    fun getCurrentPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getCurrentPlay(gameId)

    @Operation(summary = "List game plays")
    @GetMapping("/all")
    fun getAllPlaysByGameId(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getAllPlaysByGameId(gameId)

    @Operation(summary = "List user's plays")
    @GetMapping("/all/user")
    fun getAllPlaysByDiscordTag(
        @RequestParam("discordTag") discordTag: String,
    ) = playService.getAllPlaysByDiscordTag(discordTag)

    @Operation(summary = "Delay of game counts")
    @GetMapping("/delay-of-game")
    fun getDelayOfGameCountsByWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ) = playService.getDelayOfGameCountsByWeek(season, week)

    @Operation(summary = "Update play")
    @PutMapping("")
    fun updatePlay(
        @RequestBody play: Play,
    ) = playService.updatePlay(play)
}
