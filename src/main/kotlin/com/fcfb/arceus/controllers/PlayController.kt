package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.PlayService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
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
@RequestMapping("${ApiConstants.FULL_PATH}/play")
class PlayController(
    private var playService: PlayService,
) {
    @Operation(summary = "Submit the defensive number for a play")
    @PostMapping("/submit_defense")
    fun defensiveNumberSubmitted(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("defensiveSubmitter") defensiveSubmitter: String,
        @RequestParam("defensiveSubmitterId") defensiveSubmitterId: String,
        @RequestParam("defensiveNumber") defensiveNumber: Int,
        @RequestParam("timeoutCalled") timeoutCalled: Boolean = false,
    ) = playService.defensiveNumberSubmitted(gameId, defensiveSubmitter, defensiveSubmitterId, defensiveNumber, timeoutCalled)

    @Operation(summary = "Submit the offensive number and play call for a play")
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

    @Operation(summary = "Roll back the most recent play in a game")
    @PutMapping("/rollback")
    fun rollbackPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.rollbackPlay(gameId)

    @Operation(summary = "Get a play by its ID")
    @GetMapping("/{playId}")
    fun getPlayById(
        @PathVariable("playId") playId: Int,
    ) = playService.getPlayById(playId)

    @Operation(summary = "Get the previous play in a game")
    @GetMapping("/previous")
    fun getPreviousPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getPreviousPlay(gameId)

    @Operation(summary = "Get the current play in a game")
    @GetMapping("/current")
    fun getCurrentPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getCurrentPlay(gameId)

    @Operation(summary = "List all plays in a game")
    @GetMapping("/all")
    fun getAllPlaysByGameId(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getAllPlaysByGameId(gameId)

    @Operation(summary = "List all plays submitted by a user")
    @GetMapping("/all/user")
    fun getAllPlaysByDiscordTag(
        @RequestParam("discordTag") discordTag: String,
    ) = playService.getAllPlaysByDiscordTag(discordTag)

    @Operation(summary = "Get delay of game counts for a season and week")
    @GetMapping("/delay-of-game")
    fun getDelayOfGameCountsByWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ) = playService.getDelayOfGameCountsByWeek(season, week)

    @Operation(summary = "Update an existing play")
    @PutMapping("")
    fun updatePlay(
        @RequestBody play: Play,
    ) = playService.updatePlay(play)
}
