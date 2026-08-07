package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.BulkScheduleRequest
import com.fcfb.arceus.dto.request.ConferenceRulesRequest
import com.fcfb.arceus.dto.request.ConferenceScheduleRequest
import com.fcfb.arceus.dto.request.MoveGameRequest
import com.fcfb.arceus.dto.request.ScheduleEntry
import com.fcfb.arceus.dto.response.ConferenceRulesResponse
import com.fcfb.arceus.dto.response.OocGenerationResult
import com.fcfb.arceus.dto.response.ScheduleGenJob
import com.fcfb.arceus.dto.response.ScheduleGenJobResponse
import com.fcfb.arceus.dto.response.ScheduleValidationResult
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.service.fcfb.ScheduleService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/schedule")
class ScheduleController(
    private val scheduleService: ScheduleService,
) {
    @Operation(summary = "Get a team's scheduled opponent")
    @GetMapping("/opponent")
    fun getTeamOpponent(
        @RequestParam("team") team: String,
    ) = scheduleService.getTeamOpponent(team)

    @Operation(summary = "Get a team's schedule for a season")
    @GetMapping("/season")
    fun getScheduleBySeasonAndTeam(
        @RequestParam("season") season: Int,
        @RequestParam("team") team: String,
    ) = scheduleService.getScheduleBySeasonAndTeam(season, team)

    @Operation(summary = "Get the full schedule for a season")
    @GetMapping("/season/{season}")
    fun getScheduleBySeason(
        @PathVariable("season") season: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getScheduleBySeason(season))

    @Operation(summary = "Get the schedule for a season and week")
    @GetMapping("/season/{season}/week/{week}")
    fun getScheduleBySeasonAndWeek(
        @PathVariable("season") season: Int,
        @PathVariable("week") week: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getScheduleBySeasonAndWeek(season, week))

    @Operation(summary = "Get the schedule for a conference in a season")
    @GetMapping("/conference")
    fun getConferenceSchedule(
        @RequestParam("season") season: Int,
        @RequestParam("conference") conference: String,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getConferenceSchedule(season, conference))

    @Operation(summary = "Get the postseason schedule for a season")
    @GetMapping("/postseason/{season}")
    fun getPostseasonSchedule(
        @PathVariable("season") season: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getPostseasonSchedule(season))

    @Operation(summary = "Get a schedule entry by its ID")
    @GetMapping("/{id}")
    fun getScheduleById(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.getScheduleById(id))

    @Operation(summary = "Check whether a team is available in a given week")
    @GetMapping("/team-available")
    fun isTeamAvailable(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("team") team: String,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(!scheduleService.isTeamScheduledInWeek(season, week, team))

    @Operation(summary = "Create a new schedule entry")
    @PostMapping("")
    fun createScheduleEntry(
        @RequestBody entry: ScheduleEntry,
    ): ResponseEntity<Schedule> = ResponseEntity.status(201).body(scheduleService.createScheduleEntry(entry))

    @Operation(summary = "Create multiple schedule entries in bulk")
    @PostMapping("/bulk")
    fun createBulkScheduleEntries(
        @RequestBody request: BulkScheduleRequest,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.status(201).body(scheduleService.createBulkScheduleEntries(request.entries))

    @Operation(summary = "Generate a conference's schedule")
    @PostMapping("/generate-conference")
    fun generateConferenceSchedule(
        @RequestBody request: ConferenceScheduleRequest,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.status(201).body(scheduleService.generateConferenceSchedule(request))

    @Operation(summary = "Start generating schedules for all conferences in a season")
    @PostMapping("/generate-all-conferences/{season}")
    fun generateAllConferenceSchedules(
        @PathVariable("season") season: Int,
    ): ResponseEntity<ScheduleGenJobResponse> = ResponseEntity.status(202).body(scheduleService.startAllConferenceGenerationAsync(season))

    @Operation(summary = "Get the status of a conference schedule generation job")
    @GetMapping("/generate-all-conferences/status/{jobId}")
    fun getScheduleGenJobStatus(
        @PathVariable("jobId") jobId: String,
    ): ResponseEntity<ScheduleGenJob> = ResponseEntity.ok(scheduleService.getScheduleGenJobStatus(jobId))

    @Operation(summary = "Generate the out-of-conference schedule for a season")
    @PostMapping("/generate-ooc/{season}")
    fun generateOutOfConferenceSchedule(
        @PathVariable("season") season: Int,
    ): ResponseEntity<OocGenerationResult> = ResponseEntity.ok(scheduleService.generateOutOfConferenceSchedule(season))

    @Operation(summary = "Update a schedule entry")
    @PutMapping("/{id}")
    fun updateScheduleEntry(
        @PathVariable("id") id: Int,
        @RequestBody entry: ScheduleEntry,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.updateScheduleEntry(id, entry))

    @Operation(summary = "Move a scheduled game to a new date and time")
    @PutMapping("/move")
    fun moveGame(
        @RequestBody request: MoveGameRequest,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.moveGame(request))

    @Operation(summary = "Delete a schedule entry")
    @DeleteMapping("/{id}")
    fun deleteScheduleEntry(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Void> {
        scheduleService.deleteScheduleEntry(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Delete the entire schedule for a season")
    @DeleteMapping("/season/{season}")
    fun deleteScheduleBySeason(
        @PathVariable("season") season: Int,
    ): ResponseEntity<Void> {
        scheduleService.deleteScheduleBySeason(season)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Save scheduling rules for a conference")
    @PostMapping("/conference-rules")
    fun saveConferenceRules(
        @RequestBody request: ConferenceRulesRequest,
    ): ResponseEntity<ConferenceRulesResponse> = ResponseEntity.ok(scheduleService.saveConferenceRules(request))

    @Operation(summary = "Get scheduling rules for a conference")
    @GetMapping("/conference-rules")
    fun getConferenceRules(
        @RequestParam("conference") conference: String,
    ): ResponseEntity<ConferenceRulesResponse> = ResponseEntity.ok(scheduleService.getConferenceRules(conference))

    @Operation(summary = "Validate a season's schedule")
    @GetMapping("/season/{season}/validate")
    fun validateSchedule(
        @PathVariable("season") season: Int,
    ): ResponseEntity<ScheduleValidationResult> = ResponseEntity.ok(scheduleService.validateSchedule(season))
}
