package com.fcfb.arceus.controllers.schedule

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.dto.response.ScheduleGenJob
import com.fcfb.arceus.dto.response.ScheduleGenJobResponse
import com.fcfb.arceus.service.fcfb.schedule.ConferenceScheduleGenerationJobService
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
@RequestMapping("${ApiConstants.FULL_PATH}/conference-schedule-generation-job")
class ConferenceScheduleGenerationJobController(
    private val conferenceScheduleGenerationJobService: ConferenceScheduleGenerationJobService,
) {
    @Operation(summary = "Start schedule generation")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun generateAllConferenceSchedules(
        @RequestParam("season") season: Int,
    ): ScheduleGenJobResponse = conferenceScheduleGenerationJobService.startAllConferenceGenerationAsync(season)

    @Operation(summary = "Get schedule generation status")
    @GetMapping("/status")
    fun getScheduleGenJobStatus(
        @RequestParam("jobId") jobId: String,
    ): ScheduleGenJob = conferenceScheduleGenerationJobService.getScheduleGenJobStatus(jobId)
}
