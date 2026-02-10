package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.BulkScheduleRequest
import com.fcfb.arceus.dto.ConferenceScheduleRequest
import com.fcfb.arceus.dto.MoveGameRequest
import com.fcfb.arceus.dto.ScheduleEntry
import com.fcfb.arceus.dto.ScheduleGenJob
import com.fcfb.arceus.dto.ScheduleGenJobResponse
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.service.fcfb.ScheduleService
import com.fcfb.arceus.service.fcfb.SeasonService
import com.fcfb.arceus.service.fcfb.TeamService
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
    private var scheduleService: ScheduleService,
    private var seasonService: SeasonService,
    private var teamService: TeamService,
) {
    /**
     * Get an opponent for a given week and team
     * @param team
     * @return
     */
    @GetMapping("/opponent")
    fun getTeamOpponent(
        @RequestParam("team") team: String,
    ) = scheduleService.getTeamOpponent(team)

    /**
     * Get the schedule for a given season for a team
     * @param season
     * @param team
     * @return
     */
    @GetMapping("/season")
    fun getScheduleBySeasonAndTeam(
        @RequestParam("season") season: Int,
        @RequestParam("team") team: String,
    ) = scheduleService.getScheduleBySeasonAndTeam(season, team)

    /**
     * Get the full schedule for a season
     * @param season
     * @return
     */
    @GetMapping("/season/{season}")
    fun getScheduleBySeason(
        @PathVariable("season") season: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getScheduleBySeason(season))

    /**
     * Get the schedule for a season and week
     * @param season
     * @param week
     * @return
     */
    @GetMapping("/season/{season}/week/{week}")
    fun getScheduleBySeasonAndWeek(
        @PathVariable("season") season: Int,
        @PathVariable("week") week: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getScheduleBySeasonAndWeek(season, week))

    /**
     * Get the conference schedule
     * @param season
     * @param conference
     * @return
     */
    @GetMapping("/conference")
    fun getConferenceSchedule(
        @RequestParam("season") season: Int,
        @RequestParam("conference") conference: String,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getConferenceSchedule(season, conference))

    /**
     * Get the postseason schedule (playoffs, bowls, conference championships)
     * @param season
     * @return
     */
    @GetMapping("/postseason/{season}")
    fun getPostseasonSchedule(
        @PathVariable("season") season: Int,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.ok(scheduleService.getPostseasonSchedule(season))

    /**
     * Get a schedule entry by id
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    fun getScheduleById(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.getScheduleById(id))

    /**
     * Check if a team is scheduled in a specific week
     * @param season
     * @param week
     * @param team
     * @return
     */
    @GetMapping("/team-available")
    fun isTeamAvailable(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
        @RequestParam("team") team: String,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(!scheduleService.isTeamScheduledInWeek(season, week, team))

    /**
     * Create a single schedule entry
     * @param entry
     * @return
     */
    @PostMapping("")
    fun createScheduleEntry(
        @RequestBody entry: ScheduleEntry,
    ): ResponseEntity<Schedule> = ResponseEntity.status(201).body(scheduleService.createScheduleEntry(entry))

    /**
     * Create multiple schedule entries in bulk
     * @param request
     * @return
     */
    @PostMapping("/bulk")
    fun createBulkScheduleEntries(
        @RequestBody request: BulkScheduleRequest,
    ): ResponseEntity<List<Schedule>> = ResponseEntity.status(201).body(scheduleService.createBulkScheduleEntries(request.entries))

    /**
     * Auto-generate a conference schedule
     * @param request
     * @return
     */
    @PostMapping("/generate-conference")
    fun generateConferenceSchedule(
        @RequestBody request: ConferenceScheduleRequest,
    ): ResponseEntity<List<Schedule>> {
        val conferenceTeams = teamService.getTeamsInConference(request.conference) ?: emptyList()
        return ResponseEntity.status(201).body(scheduleService.generateConferenceSchedule(request, conferenceTeams))
    }

    /**
     * Auto-generate conference schedules for ALL conferences in a season (async fire-and-forget).
     * Returns a job ID immediately; processing happens in a background coroutine.
     * Poll /schedule/generate-all-conferences/status/{jobId} for progress.
     */
    @PostMapping("/generate-all-conferences/{season}")
    fun generateAllConferenceSchedules(
        @PathVariable("season") season: Int,
    ): ResponseEntity<ScheduleGenJobResponse> = ResponseEntity.status(202).body(scheduleService.startAllConferenceGenerationAsync(season))

    /**
     * Poll the status of an all-conference generation job.
     */
    @GetMapping("/generate-all-conferences/status/{jobId}")
    fun getScheduleGenJobStatus(
        @PathVariable("jobId") jobId: String,
    ): ResponseEntity<ScheduleGenJob> {
        val job =
            scheduleService.getScheduleGenJobStatus(jobId)
                ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(job)
    }

    /**
     * Update a schedule entry
     * @param id
     * @param entry
     * @return
     */
    @PutMapping("/{id}")
    fun updateScheduleEntry(
        @PathVariable("id") id: Int,
        @RequestBody entry: ScheduleEntry,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.updateScheduleEntry(id, entry))

    /**
     * Move a game to a different week
     * @param request
     * @return
     */
    @PutMapping("/move")
    fun moveGame(
        @RequestBody request: MoveGameRequest,
    ): ResponseEntity<Schedule> = ResponseEntity.ok(scheduleService.moveGame(request))

    /**
     * Delete a schedule entry
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    fun deleteScheduleEntry(
        @PathVariable("id") id: Int,
    ): ResponseEntity<Void> {
        scheduleService.deleteScheduleEntry(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Delete all schedule entries for a season
     * @param season
     * @return
     */
    @DeleteMapping("/season/{season}")
    fun deleteScheduleBySeason(
        @PathVariable("season") season: Int,
    ): ResponseEntity<Void> {
        scheduleService.deleteScheduleBySeason(season)
        return ResponseEntity.noContent().build()
    }
}
