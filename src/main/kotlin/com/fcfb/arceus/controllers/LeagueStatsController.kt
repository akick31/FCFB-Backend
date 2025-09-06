package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.service.fcfb.LeagueStatsService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
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
) {
    /**
     * Get filtered league stats with pagination
     */
    @GetMapping
    fun getFilteredLeagueStats(
        @RequestParam(required = false) subdivision: Subdivision?,
        @RequestParam(required = false) season: Int?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<Page<com.fcfb.arceus.model.LeagueStats>> =
        ResponseEntity.ok(
            leagueStatsService.getFilteredLeagueStats(
                subdivision = subdivision,
                season = season,
                pageable = pageable,
            ),
        )

    /**
     * Generate all league stats (recalculate all league stats)
     */
    @PostMapping("/generate/all")
    fun generateAllLeagueStats() = leagueStatsService.generateAllLeagueStats()
}
