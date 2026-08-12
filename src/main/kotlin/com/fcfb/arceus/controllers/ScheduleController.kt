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
import com.fcfb.arceus.model.Bowl
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.service.fcfb.ScheduleService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
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
    @Operation(summary = "Get team's opponent")
    @GetMapping("/opponent")
    fun getTeamOpponent(
        @RequestParam("team") team: String,
    ) = scheduleService.getTeamOpponent(team)

    @Operation(summary = "Get saved bowl info")
    @GetMapping("/bowl")
    fun getBowl(
        @RequestParam("name") name: String,
    ): ResponseEntity<Bowl?> = ResponseEntity.ok(scheduleService.getBowl(name))

    @Operation(summary = "Get team season schedule")
    @GetMapping("/season", params = ["team"])
    fun getScheduleBySeasonAndTeam(
        @RequestParam("season") season: Int,
        @RequestParam("team") team: String,
    ) = scheduleService.getScheduleBySeasonAndTeam(season, team)

    @Operation(summary = "Get full season schedule")
    @GetMapping("/season", params = ["!team"])
    fun getScheduleBySeason(
        @RequestParam("season") season: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getScheduleBySeason(season))

    @Operation(summary = "Get schedule by week")
    @GetMapping("/season/week")
    fun getScheduleBySeasonAndWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getScheduleBySeasonAndWeek(season, week))

    @Operation(summary = "Get conference schedule")
    @GetMapping("/conference")
    fun getConferenceSchedule(
        @RequestParam("season") season: Int,
        @RequestParam("conference") conference: String,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getConferenceSchedule(season, conference))

    @Operation(summary = "Get postseason schedule")
    @GetMapping("/postseason")
    fun getPostseasonSchedule(
        @RequestParam("season") season: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getPostseasonSchedule(season))

    @Operation(summary = "Get schedule entry by ID")
    @GetMapping("")
    fun getScheduleById(
        @RequestParam("id") id: Int,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.getScheduleById(id))

    @Operation(summary = "Check team availability")
    @GetMapping("/team-available")
    fun isTeamAvailable(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("team") team: String,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(!scheduleService.isTeamScheduledInWeek(season, week, team))

    @Operation(summary = "Create schedule entry")
    @PostMapping("")
    fun createScheduleEntry(
        @RequestBody entry: ScheduleEntry,
    ): ResponseEntity<Schedule> = ResponseEntity.status(201).body(scheduleService.createScheduleEntry(entry))

    @Operation(summary = "Bulk create schedule entries")
    @PostMapping("/bulk")
    fun createBulkScheduleEntries(
        @RequestBody request: BulkScheduleRequest,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.status(201).body(scheduleService.createBulkScheduleEntries(request.entries))

    @Operation(summary = "Generate conference schedule")
    @PostMapping("/generate-conference")
    fun generateConferenceSchedule(
        @RequestBody request: ConferenceScheduleRequest,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.status(201).body(scheduleService.generateConferenceSchedule(request))

    @Operation(summary = "Start schedule generation")
    @PostMapping("/generate-all-conferences")
    fun generateAllConferenceSchedules(
        @RequestParam("season") season: Int,
    ): ResponseEntity<ScheduleGenJobResponse> = ResponseEntity.status(202).body(scheduleService.startAllConferenceGenerationAsync(season))

    @Operation(summary = "Get schedule generation status")
    @GetMapping("/generate-all-conferences/status")
    fun getScheduleGenJobStatus(
        @RequestParam("jobId") jobId: String,
    ): ResponseEntity<ScheduleGenJob> = ResponseEntity.ok(scheduleService.getScheduleGenJobStatus(jobId))

    @Operation(summary = "Generate out-of-conference schedule")
    @PostMapping("/generate-ooc")
    fun generateOutOfConferenceSchedule(
        @RequestParam("season") season: Int,
    ): ResponseEntity<OocGenerationResult> = ResponseEntity.ok(scheduleService.generateOutOfConferenceSchedule(season))

    @Operation(summary = "Update schedule entry")
    @PutMapping("")
    fun updateScheduleEntry(
        @RequestParam("id") id: Int,
        @RequestBody entry: ScheduleEntry,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.updateScheduleEntry(id, entry))

    @Operation(summary = "Reschedule game")
    @PutMapping("/move")
    fun moveGame(
        @RequestBody request: MoveGameRequest,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.moveGame(request))

    @Operation(summary = "Delete schedule entry")
    @DeleteMapping("")
    fun deleteScheduleEntry(
        @RequestParam("id") id: Int,
    ): ResponseEntity<Void> {
        scheduleService.deleteScheduleEntry(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Delete season schedule")
    @DeleteMapping("/season")
    fun deleteScheduleBySeason(
        @RequestParam("season") season: Int,
    ): ResponseEntity<Void> {
        scheduleService.deleteScheduleBySeason(season)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Save conference scheduling rules")
    @PostMapping("/conference-rules")
    fun saveConferenceRules(
        @RequestBody request: ConferenceRulesRequest,
    ): ResponseEntity<ConferenceRulesResponse> = ResponseEntity.ok(scheduleService.saveConferenceRules(request))

    @Operation(summary = "Get conference scheduling rules")
    @GetMapping("/conference-rules")
    fun getConferenceRules(
        @RequestParam("conference") conference: String,
    ): ResponseEntity<ConferenceRulesResponse> = ResponseEntity.ok(scheduleService.getConferenceRules(conference))

    @Operation(summary = "Validate season schedule")
    @GetMapping("/season/validate")
    fun validateSchedule(
        @RequestParam("season") season: Int,
    ): ResponseEntity<ScheduleValidationResult> = ResponseEntity.ok(scheduleService.validateSchedule(season))
}
