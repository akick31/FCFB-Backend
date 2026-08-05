package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.RunoffType
import com.fcfb.arceus.model.Play
import com.fcfb.arceus.service.fcfb.PlayService
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
    /**
     * Start a new play, the defensive number was submitted. The defensive number is encrypted
     * @param gameId
     * @param defensiveNumber
     * @return
     */
    @PostMapping("/submit_defense")
    fun defensiveNumberSubmitted(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("defensiveSubmitter") defensiveSubmitter: String,
        @RequestParam("defensiveSubmitterId") defensiveSubmitterId: String,
        @RequestParam("defensiveNumber") defensiveNumber: Int,
        @RequestParam("timeoutCalled") timeoutCalled: Boolean = false,
    ) = playService.defensiveNumberSubmitted(gameId, defensiveSubmitter, defensiveSubmitterId, defensiveNumber, timeoutCalled)

    /**
     * The offensive number was submitted, run the play
     * @param gameId
     * @param offensiveNumber
     * @param playCall
     * @param runoffType
     * @param timeoutCalled
     * @return
     */
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

    @PutMapping("/rollback")
    fun rollbackPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.rollbackPlay(gameId)

    @GetMapping("/{playId}")
    fun getPlayById(
        @PathVariable("playId") playId: Int,
    ) = playService.getPlayById(playId)

    @GetMapping("/previous")
    fun getPreviousPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getPreviousPlay(gameId)

    @GetMapping("/current")
    fun getCurrentPlay(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getCurrentPlay(gameId)

    @GetMapping("/all")
    fun getAllPlaysByGameId(
        @RequestParam("gameId") gameId: Int,
    ) = playService.getAllPlaysByGameId(gameId)

    @GetMapping("/all/user")
    fun getAllPlaysByDiscordTag(
        @RequestParam("discordTag") discordTag: String,
    ) = playService.getAllPlaysByDiscordTag(discordTag)

    @GetMapping("/delay-of-game")
    fun getDelayOfGameCountsByWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ) = playService.getDelayOfGameCountsByWeek(season, week)

    @PutMapping("")
    fun updatePlay(
        @RequestBody play: Play,
    ) = playService.updatePlay(play)
}
