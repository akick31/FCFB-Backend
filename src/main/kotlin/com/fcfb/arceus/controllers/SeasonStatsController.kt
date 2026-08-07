package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.PostseasonSeasonStatsService
import com.fcfb.arceus.service.fcfb.SeasonStatsService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/season-stats")
@CrossOrigin(origins = ["*"])
class SeasonStatsController(
    private val seasonStatsService: SeasonStatsService,
    private val postseasonSeasonStatsService: PostseasonSeasonStatsService,
) {
    @Operation(summary = "List season stats matching the given team and season filters")
    @GetMapping
    fun getFilteredSeasonStats(
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) season: Int?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = seasonStatsService.getFilteredSeasonStats(
        team = team,
        season = season,
        pageable = pageable,
    )

    @Operation(summary = "Generate season stats for all teams")
    @PostMapping("/generate/all")
    fun generateAllSeasonStats() = seasonStatsService.generateAllSeasonStats()

    @Operation(summary = "Generate season stats for a single team's season")
    @PostMapping("/generate/team-season")
    fun generateSeasonStatsForTeam(
        @RequestParam team: String,
        @RequestParam seasonNumber: Int,
    ) = seasonStatsService.generateSeasonStatsForTeam(team, seasonNumber)

    @Operation(summary = "Get the leaderboard for a given season stat")
    @GetMapping("/leaderboard")
    fun getLeaderboard(
        @RequestParam statName: String,
        @RequestParam(required = false) seasonNumber: Int?,
        @RequestParam(required = false) subdivision: String?,
        @RequestParam(required = false) conference: String?,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "false") ascending: Boolean,
    ) = seasonStatsService.getLeaderboard(statName, seasonNumber, subdivision, conference, limit, ascending)

    @Operation(summary = "List postseason season stats matching the given team and season filters")
    @GetMapping("/postseason")
    fun getFilteredPostseasonSeasonStats(
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) season: Int?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = postseasonSeasonStatsService.getFilteredPostseasonSeasonStats(
        team = team,
        season = season,
        pageable = pageable,
    )

    @Operation(summary = "Generate postseason season stats for all teams")
    @PostMapping("/postseason/generate/all")
    fun generateAllPostseasonSeasonStats() = postseasonSeasonStatsService.generateAllPostseasonSeasonStats()
}
