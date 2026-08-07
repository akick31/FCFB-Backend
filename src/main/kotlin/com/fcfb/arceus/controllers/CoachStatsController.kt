package com.fcfb.arceus.controllers

import com.fcfb.arceus.service.fcfb.CoachStatsService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/coach-stats")
@CrossOrigin(origins = ["*"])
class CoachStatsController(
    private val coachStatsService: CoachStatsService,
) {
    @GetMapping
    fun getCoachStats(
        @RequestParam coach: String,
    ) = coachStatsService.getCoachStats(coach)
}
