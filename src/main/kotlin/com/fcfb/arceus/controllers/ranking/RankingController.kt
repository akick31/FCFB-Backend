package com.fcfb.arceus.controllers.ranking

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.dto.request.UploadRankingsRequest
import com.fcfb.arceus.dto.response.RankingResponse
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.service.fcfb.ranking.RankingService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/ranking")
class RankingController(
    private val rankingService: RankingService,
) {
    @Operation(summary = "Get rankings by week")
    @GetMapping
    fun getRankings(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("pollType") pollType: String,
    ): List<RankingResponse> = rankingService.getRankings(season, week, pollType)

    @Operation(summary = "Get a team's rank in every uploaded week of a poll")
    @GetMapping("/team")
    fun getTeamRankings(
        @RequestParam("teamId") teamId: Int,
        @RequestParam("pollType") pollType: String,
    ): List<RankingResponse> = rankingService.getTeamRankings(teamId, pollType)

    @Operation(summary = "Get games where a team was ranked")
    @GetMapping("/games")
    fun getRankedGames(
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) season: Int?,
    ): List<Game> = rankingService.getRankedGames(team, season)

    @Operation(summary = "Get the most recently uploaded rankings for a poll")
    @GetMapping("/latest")
    fun getLatest(
        @RequestParam("pollType") pollType: String,
    ): List<RankingResponse> = rankingService.getLatestRankings(pollType)

    @Operation(summary = "Get available ranked weeks")
    @GetMapping("/weeks")
    fun getWeeks(
        @RequestParam("season") season: Int,
        @RequestParam("pollType") pollType: String,
    ): List<Int> = rankingService.getAvailableWeeks(season, pollType)

    @Operation(summary = "Upload rankings")
    @PostMapping
    fun uploadRankings(
        @RequestBody request: UploadRankingsRequest,
    ): List<RankingResponse> = rankingService.uploadRankings(request.season, request.week, request.pollType, request.teams)
}
