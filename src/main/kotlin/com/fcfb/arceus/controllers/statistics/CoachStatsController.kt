package com.fcfb.arceus.controllers.statistics

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.service.fcfb.statistics.CoachStatsService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/coach-stats")
@CrossOrigin(origins = ["*"])
class CoachStatsController(
    private val coachStatsService: CoachStatsService,
) {
    @Operation(summary = "Get coach stats")
    @GetMapping
    fun getCoachStats(
        @RequestParam coach: String,
    ) = coachStatsService.getCoachStats(coach)

    @Operation(summary = "Generate all coach season stats")
    @PostMapping("/generate/all")
    fun generateAllCoachSeasonStats() = coachStatsService.generateAllCoachSeasonStats()

    @Operation(summary = "Generate coach season stats for a single coach")
    @PostMapping("/generate/coach")
    fun generateCoachSeasonStatsForCoach(
        @RequestParam coach: String,
    ) = coachStatsService.regenerateForCoach(coach)
}
