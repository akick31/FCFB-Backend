package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.service.fcfb.ConferenceStatsService
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
@RequestMapping("${ApiConstants.FULL_PATH}/conference-stats")
@CrossOrigin(origins = ["*"])
class ConferenceStatsController(
    private val conferenceStatsService: ConferenceStatsService,
) {
    /**
     * Get filtered conference stats with pagination
     */
    @GetMapping
    fun getFilteredConferenceStats(
        @RequestParam(required = false) conference: Conference?,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) subdivision: Subdivision?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<Page<com.fcfb.arceus.model.ConferenceStats>> =
        ResponseEntity.ok(
            conferenceStatsService.getFilteredConferenceStats(
                conference = conference,
                season = season,
                subdivision = subdivision,
                pageable = pageable,
            ),
        )

    /**
     * Get all conference stats
     */
    @GetMapping("/all")
    fun getAllConferenceStats() = conferenceStatsService.getAllConferenceStats()

    /**
     * Get conference stats for a specific subdivision, conference, and season
     */
    @GetMapping("")
    fun getConferenceStatsBySubdivisionAndConferenceAndSeason(
        @RequestParam subdivision: Subdivision,
        @RequestParam conference: Conference,
        @RequestParam seasonNumber: Int,
    ) = conferenceStatsService.getConferenceStatsBySubdivisionAndConferenceAndSeason(
        subdivision,
        conference,
        seasonNumber,
    )

    /**
     * Get all conference stats for a specific subdivision
     */
    @GetMapping("/subdivision")
    fun getConferenceStatsBySubdivision(
        @RequestParam subdivision: Subdivision,
    ) = conferenceStatsService.getConferenceStatsBySubdivision(subdivision)

    /**
     * Get all conference stats for a specific conference
     */
    @GetMapping("/conference")
    fun getConferenceStatsByConference(
        @RequestParam conference: Conference,
    ) = conferenceStatsService.getConferenceStatsByConference(conference)

    /**
     * Get all conference stats for a specific season
     */
    @GetMapping("/season")
    fun getConferenceStatsBySeason(
        @RequestParam seasonNumber: Int,
    ) = conferenceStatsService.getConferenceStatsBySeason(seasonNumber)

    /**
     * Generate all conference stats (recalculate all conference stats)
     */
    @PostMapping("/generate/all")
    fun generateAllConferenceStats() = conferenceStatsService.generateAllConferenceStats()
}
