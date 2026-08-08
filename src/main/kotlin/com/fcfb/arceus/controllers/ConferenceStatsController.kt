package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.service.fcfb.ConferenceStatsService
import com.fcfb.arceus.service.fcfb.PostseasonConferenceStatsService
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
@RequestMapping("${ApiConstants.FULL_PATH}/conference-stats")
@CrossOrigin(origins = ["*"])
class ConferenceStatsController(
    private val conferenceStatsService: ConferenceStatsService,
    private val postseasonConferenceStatsService: PostseasonConferenceStatsService,
) {
    @Operation(summary = "Get conference stats")
    @GetMapping
    fun getFilteredConferenceStats(
        @RequestParam(required = false) conference: String?,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) subdivision: Subdivision?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = conferenceStatsService.getFilteredConferenceStats(
        conference = conference,
        season = season,
        subdivision = subdivision,
        pageable = pageable,
    )

    @Operation(summary = "Generate conference stats")
    @PostMapping("/generate/all")
    fun generateAllConferenceStats() = conferenceStatsService.generateAllConferenceStats()

    @Operation(summary = "Get postseason conference stats")
    @GetMapping("/postseason")
    fun getFilteredPostseasonConferenceStats(
        @RequestParam(required = false) conference: String?,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) subdivision: Subdivision?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = postseasonConferenceStatsService.getFilteredPostseasonConferenceStats(
        conference = conference,
        season = season,
        subdivision = subdivision,
        pageable = pageable,
    )

    @Operation(summary = "Generate postseason conference stats")
    @PostMapping("/postseason/generate/all")
    fun generateAllPostseasonConferenceStats() = postseasonConferenceStatsService.generateAllPostseasonConferenceStats()
}
