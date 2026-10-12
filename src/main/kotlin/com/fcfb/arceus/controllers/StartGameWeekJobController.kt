package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.response.GameWeekJob
import com.fcfb.arceus.dto.response.GameWeekJobResponse
import com.fcfb.arceus.service.fcfb.game.StartGameWeekJobService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/start-game-week-job")
class StartGameWeekJobController(
    private val startGameWeekJobService: StartGameWeekJobService,
) {
    @Operation(summary = "Start week's games")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun startWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): GameWeekJobResponse = startGameWeekJobService.startWeekAsync(season, week)

    @Operation(summary = "Get game week job status")
    @GetMapping("/status")
    fun getGameWeekJobStatus(
        @RequestParam("jobId") jobId: String,
    ): GameWeekJob = startGameWeekJobService.getGameWeekJobStatus(jobId)

    @Operation(summary = "List game week jobs")
    @GetMapping("/jobs")
    fun getAllGameWeekJobs(): List<GameWeekJob> = startGameWeekJobService.getAllGameWeekJobs()

    @Operation(summary = "Retry failed games")
    @PostMapping("/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun retryFailedGames(
        @RequestParam("jobId") jobId: String,
    ): GameWeekJobResponse = startGameWeekJobService.retryFailedGames(jobId)
}
