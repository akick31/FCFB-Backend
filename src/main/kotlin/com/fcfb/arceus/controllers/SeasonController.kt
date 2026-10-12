package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.Season
import com.fcfb.arceus.service.fcfb.season.SeasonService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/season")
class SeasonController(
    private val seasonService: SeasonService,
) {
    @Operation(summary = "Start new season")
    @PostMapping
    fun startSeason(): Season = seasonService.startSeason()

    @Operation(summary = "End current season")
    @PostMapping("/end")
    fun endSeason(): Season = seasonService.endSeasonManually()

    @Operation(summary = "Set current season")
    @PutMapping("/set-current")
    fun setCurrentSeason(
        @RequestParam("seasonNumber") seasonNumber: Int,
    ): Season = seasonService.setCurrentSeason(seasonNumber)

    @Operation(summary = "Update season week")
    @PutMapping("/week")
    fun updateCurrentWeek(
        @RequestParam("seasonNumber") seasonNumber: Int,
        @RequestParam("week") week: Int,
    ): Season = seasonService.updateCurrentWeek(seasonNumber, week)

    @Operation(summary = "Get current season")
    @GetMapping("/current")
    fun getCurrentSeason(): Season = seasonService.getCurrentSeason()

    @Operation(summary = "Get current season week")
    @GetMapping("/current/week")
    fun getCurrentWeek(): Int = seasonService.getCurrentWeek()

    @Operation(summary = "Get upcoming season")
    @GetMapping("/upcoming")
    fun getUpcomingSeason(): Season? = seasonService.getUpcomingSeason()

    @Operation(summary = "Get most recent season")
    @GetMapping("/latest-completed")
    fun getLatestCompletedSeason(): Season? = seasonService.getLatestCompletedSeason()

    @Operation(summary = "List all seasons")
    @GetMapping("/all")
    fun getAllSeasons(): List<Season> = seasonService.getAllSeasons()

    @Operation(summary = "Get season by number")
    @GetMapping("")
    fun getSeasonByNumber(
        @RequestParam("seasonNumber") seasonNumber: Int,
    ): Season = seasonService.getSeasonByNumber(seasonNumber)

    @Operation(summary = "Check schedule locked")
    @GetMapping("/schedule-locked")
    fun isScheduleLocked(
        @RequestParam("seasonNumber") seasonNumber: Int,
    ): Boolean = seasonService.isScheduleLocked(seasonNumber)

    @Operation(summary = "Check championship finished")
    @GetMapping("/championship-finished")
    fun hasFinishedNationalChampionship(
        @RequestParam("seasonNumber") seasonNumber: Int,
    ): Boolean = seasonService.hasFinishedNationalChampionship(seasonNumber)

    @Operation(summary = "Lock season schedule")
    @PutMapping("/lock-schedule")
    fun lockSchedule(
        @RequestParam("seasonNumber") seasonNumber: Int,
    ): Season = seasonService.lockSchedule(seasonNumber)

    @Operation(summary = "Unlock season schedule")
    @PutMapping("/unlock-schedule")
    fun unlockSchedule(
        @RequestParam("seasonNumber") seasonNumber: Int,
    ): Season = seasonService.unlockSchedule(seasonNumber)

    @Operation(summary = "Create season")
    @PostMapping("", params = ["seasonNumber"])
    @ResponseStatus(HttpStatus.CREATED)
    fun createSeasonForScheduling(
        @RequestParam("seasonNumber") seasonNumber: Int,
    ): Season = seasonService.createSeasonForScheduling(seasonNumber)
}
