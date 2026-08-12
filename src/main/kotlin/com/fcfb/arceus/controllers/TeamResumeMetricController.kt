package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.response.TeamResumeMetricResponse
import com.fcfb.arceus.service.fcfb.TeamResumeMetricService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/team-resume-metric")
class TeamResumeMetricController(
    private val teamResumeMetricService: TeamResumeMetricService,
) {
    @Operation(summary = "Get every team's resume metrics (opponent-quality records, strength of schedule) for a week")
    @GetMapping
    fun getMetrics(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<List<TeamResumeMetricResponse>> = ResponseEntity.ok(teamResumeMetricService.getMetrics(season, week))
}
