package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.service.fcfb.LeagueStatsService
import com.fcfb.arceus.service.fcfb.PostseasonLeagueStatsService
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
@RequestMapping("${ApiConstants.FULL_PATH}/league-stats")
@CrossOrigin(origins = ["*"])
class LeagueStatsController(
    private val leagueStatsService: LeagueStatsService,
    private val postseasonLeagueStatsService: PostseasonLeagueStatsService,
) {
    @Operation(summary = "Get league stats filtered by subdivision and season")
    @GetMapping
    fun getFilteredLeagueStats(
        @RequestParam(required = false) subdivision: Subdivision?,
        @RequestParam(required = false) season: Int?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = leagueStatsService.getFilteredLeagueStats(
        subdivision = subdivision,
        season = season,
        pageable = pageable,
    )

    @Operation(summary = "Generate league stats for all teams")
    @PostMapping("/generate/all")
    fun generateAllLeagueStats() = leagueStatsService.generateAllLeagueStats()

    @Operation(summary = "Get postseason league stats filtered by subdivision and season")
    @GetMapping("/postseason")
    fun getFilteredPostseasonLeagueStats(
        @RequestParam(required = false) subdivision: Subdivision?,
        @RequestParam(required = false) season: Int?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = postseasonLeagueStatsService.getFilteredPostseasonLeagueStats(
        subdivision = subdivision,
        season = season,
        pageable = pageable,
    )

    @Operation(summary = "Generate postseason league stats for all teams")
    @PostMapping("/postseason/generate/all")
    fun generateAllPostseasonLeagueStats() = postseasonLeagueStatsService.generateAllPostseasonLeagueStats()
}
