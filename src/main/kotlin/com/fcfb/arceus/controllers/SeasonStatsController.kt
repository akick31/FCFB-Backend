package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.SeasonStatsService
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
) {
    /**
     * Get filtered season stats with pagination
     */
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

    /**
     * Generate all season stats (recalculate all season stats)
     */
    @PostMapping("/generate/all")
    fun generateAllSeasonStats() = seasonStatsService.generateAllSeasonStats()

    /**
     * Generate season stats for a specific team and season
     */
    @PostMapping("/generate/team-season")
    fun generateSeasonStatsForTeam(
        @RequestParam team: String,
        @RequestParam seasonNumber: Int,
    ) = seasonStatsService.generateSeasonStatsForTeam(team, seasonNumber)

    /**
     * Get leaderboard for a specific stat
     */
    @GetMapping("/leaderboard")
    fun getLeaderboard(
        @RequestParam statName: String,
        @RequestParam(required = false) seasonNumber: Int?,
        @RequestParam(required = false) subdivision: String?,
        @RequestParam(required = false) conference: String?,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "false") ascending: Boolean,
    ) = seasonStatsService.getLeaderboard(statName, seasonNumber, subdivision, conference, limit, ascending)
}
