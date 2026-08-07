package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.Season
import com.fcfb.arceus.service.fcfb.SeasonService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/season")
class SeasonController(
    private var seasonService: SeasonService,
) {
    @Operation(summary = "Start a new season")
    @PostMapping
    fun startSeason(): ResponseEntity<Season> = ResponseEntity.ok(seasonService.startSeason())

    @Operation(summary = "End the current season manually")
    @PostMapping("/end")
    fun endSeason(): ResponseEntity<Season> = ResponseEntity.ok(seasonService.endSeasonManually())

    @Operation(summary = "Set the current season")
    @PutMapping("/{seasonNumber}/set-current")
    fun setCurrentSeason(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.ok(seasonService.setCurrentSeason(seasonNumber))

    @Operation(summary = "Update the current week of a season")
    @PutMapping("/{seasonNumber}/week")
    fun updateCurrentWeek(
        @PathVariable("seasonNumber") seasonNumber: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<Season> = ResponseEntity.ok(seasonService.updateCurrentWeek(seasonNumber, week))

    @Operation(summary = "Get the current season")
    @GetMapping("/current")
    fun getCurrentSeason(): ResponseEntity<Season> = ResponseEntity.ok(seasonService.getCurrentSeason())

    @Operation(summary = "Get the current week of the season")
    @GetMapping("/current/week")
    fun getCurrentWeek(): ResponseEntity<Int> = ResponseEntity.ok(seasonService.getCurrentWeek())

    @Operation(summary = "Get the upcoming season")
    @GetMapping("/upcoming")
    fun getUpcomingSeason(): ResponseEntity<Season?> = ResponseEntity.ok(seasonService.getUpcomingSeason())

    @Operation(summary = "Get the most recently completed season")
    @GetMapping("/latest-completed")
    fun getLatestCompletedSeason(): ResponseEntity<Season?> = ResponseEntity.ok(seasonService.getLatestCompletedSeason())

    @Operation(summary = "List all seasons")
    @GetMapping("/all")
    fun getAllSeasons(): ResponseEntity<List<Season>> = ResponseEntity.ok(seasonService.getAllSeasons())

    @Operation(summary = "Get a season by its number")
    @GetMapping("/{seasonNumber}")
    fun getSeasonByNumber(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.ok(seasonService.getSeasonByNumber(seasonNumber))

    @Operation(summary = "Check whether a season's schedule is locked")
    @GetMapping("/{seasonNumber}/schedule-locked")
    fun isScheduleLocked(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(seasonService.isScheduleLocked(seasonNumber))

    @Operation(summary = "Check whether a season's national championship has finished")
    @GetMapping("/{seasonNumber}/championship-finished")
    fun hasFinishedNationalChampionship(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(seasonService.hasFinishedNationalChampionship(seasonNumber))

    @Operation(summary = "Lock a season's schedule")
    @PutMapping("/{seasonNumber}/lock-schedule")
    fun lockSchedule(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.ok(seasonService.lockSchedule(seasonNumber))

    @Operation(summary = "Unlock a season's schedule")
    @PutMapping("/{seasonNumber}/unlock-schedule")
    fun unlockSchedule(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.ok(seasonService.unlockSchedule(seasonNumber))

    @Operation(summary = "Create a season for scheduling")
    @PostMapping("/{seasonNumber}")
    fun createSeasonForScheduling(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.status(201).body(seasonService.createSeasonForScheduling(seasonNumber))
}
