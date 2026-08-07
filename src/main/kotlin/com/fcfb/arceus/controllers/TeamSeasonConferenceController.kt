package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.TeamSeasonConferenceService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/team-season-conference")
class TeamSeasonConferenceController(
    private val teamSeasonConferenceService: TeamSeasonConferenceService,
) {
    @Operation(summary = "Get team-conference assignments for a season")
    @GetMapping
    fun getForSeason(
        @RequestParam season: Int,
    ) = teamSeasonConferenceService.getConferencesForSeason(season)
}
