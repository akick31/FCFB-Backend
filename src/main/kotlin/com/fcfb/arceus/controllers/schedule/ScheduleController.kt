package com.fcfb.arceus.controllers.schedule

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.dto.request.BulkScheduleRequest
import com.fcfb.arceus.dto.request.ConferenceRulesRequest
import com.fcfb.arceus.dto.request.ConferenceScheduleRequest
import com.fcfb.arceus.dto.request.MoveGameRequest
import com.fcfb.arceus.dto.request.ScheduleEntry
import com.fcfb.arceus.dto.response.ConferenceRulesResponse
import com.fcfb.arceus.dto.response.OocGenerationResult
import com.fcfb.arceus.dto.response.ScheduleValidationResult
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.model.season.Bowl
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
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
    ): Bowl? = scheduleService.getBowl(name)

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
    ): List<Schedule> = scheduleService.getScheduleBySeason(season)

    @Operation(summary = "Get schedule by week")
    @GetMapping("/season/week")
    fun getScheduleBySeasonAndWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): List<Schedule> = scheduleService.getScheduleBySeasonAndWeek(season, week)

    @Operation(summary = "Get conference schedule")
    @GetMapping("/conference")
    fun getConferenceSchedule(
        @RequestParam("season") season: Int,
        @RequestParam("conference") conference: String,
    ): List<Schedule> = scheduleService.getConferenceSchedule(season, conference)

    @Operation(summary = "Get postseason schedule")
    @GetMapping("/postseason")
    fun getPostseasonSchedule(
        @RequestParam("season") season: Int,
    ): List<Schedule> = scheduleService.getPostseasonSchedule(season)

    @Operation(summary = "Get schedule entry by ID")
    @GetMapping("")
    fun getScheduleById(
        @RequestParam("id") id: Int,
    ): Schedule = scheduleService.getScheduleById(id)

    @Operation(summary = "Check team availability")
    @GetMapping("/team-available")
    fun isTeamAvailable(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("team") team: String,
    ): Boolean = scheduleService.isTeamAvailable(season, week, team)

    @Operation(summary = "Create schedule entry")
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun createScheduleEntry(
        @RequestBody entry: ScheduleEntry,
    ): Schedule = scheduleService.createScheduleEntry(entry)

    @Operation(summary = "Bulk create schedule entries")
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    fun createBulkScheduleEntries(
        @RequestBody request: BulkScheduleRequest,
    ): List<Schedule> = scheduleService.createBulkScheduleEntries(request)

    @Operation(summary = "Generate conference schedule")
    @PostMapping("/generate-conference")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateConferenceSchedule(
        @RequestBody request: ConferenceScheduleRequest,
    ): List<Schedule> = scheduleService.generateConferenceSchedule(request)

    @Operation(summary = "Generate out-of-conference schedule")
    @PostMapping("/generate-ooc")
    fun generateOutOfConferenceSchedule(
        @RequestParam("season") season: Int,
    ): OocGenerationResult = scheduleService.generateOutOfConferenceSchedule(season)

    @Operation(summary = "Update schedule entry")
    @PutMapping("")
    fun updateScheduleEntry(
        @RequestParam("id") id: Int,
        @RequestBody entry: ScheduleEntry,
    ): Schedule = scheduleService.updateScheduleEntry(id, entry)

    @Operation(summary = "Reschedule game")
    @PutMapping("/move")
    fun moveGame(
        @RequestBody request: MoveGameRequest,
    ): Schedule = scheduleService.moveGame(request)

    @Operation(summary = "Delete schedule entry")
    @DeleteMapping("")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteScheduleEntry(
        @RequestParam("id") id: Int,
    ) = scheduleService.deleteScheduleEntry(id)

    @Operation(summary = "Delete season schedule")
    @DeleteMapping("/season")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteScheduleBySeason(
        @RequestParam("season") season: Int,
    ) = scheduleService.deleteScheduleBySeason(season)

    @Operation(summary = "Save conference scheduling rules")
    @PostMapping("/conference-rules")
    fun saveConferenceRules(
        @RequestBody request: ConferenceRulesRequest,
    ): ConferenceRulesResponse = scheduleService.saveConferenceRules(request)

    @Operation(summary = "Get conference scheduling rules")
    @GetMapping("/conference-rules")
    fun getConferenceRules(
        @RequestParam("conference") conference: String,
    ): ConferenceRulesResponse = scheduleService.getConferenceRules(conference)

    @Operation(summary = "Validate season schedule")
    @GetMapping("/season/validate")
    fun validateSchedule(
        @RequestParam("season") season: Int,
    ): ScheduleValidationResult = scheduleService.validateSchedule(season)
}
