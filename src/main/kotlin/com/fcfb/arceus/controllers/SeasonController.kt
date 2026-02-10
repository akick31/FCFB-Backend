package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.Season
import com.fcfb.arceus.service.fcfb.SeasonService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/season")
class SeasonController(
    private var seasonService: SeasonService,
) {
    @PostMapping
    fun startSeason(): ResponseEntity<Season> = ResponseEntity.ok(seasonService.startSeason())

    @GetMapping("/current")
    fun getCurrentSeason(): ResponseEntity<Season> = ResponseEntity.ok(seasonService.getCurrentSeason())

    @GetMapping("/current/week")
    fun getCurrentWeek(): ResponseEntity<Int> = ResponseEntity.ok(seasonService.getCurrentWeek())

    @GetMapping("/all")
    fun getAllSeasons(): ResponseEntity<List<Season>> = ResponseEntity.ok(seasonService.getAllSeasons())

    @GetMapping("/{seasonNumber}")
    fun getSeasonByNumber(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.ok(seasonService.getSeasonByNumber(seasonNumber))

    @GetMapping("/{seasonNumber}/schedule-locked")
    fun isScheduleLocked(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(seasonService.isScheduleLocked(seasonNumber))

    @PutMapping("/{seasonNumber}/lock-schedule")
    fun lockSchedule(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.ok(seasonService.lockSchedule(seasonNumber))

    @PutMapping("/{seasonNumber}/unlock-schedule")
    fun unlockSchedule(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.ok(seasonService.unlockSchedule(seasonNumber))

    @PostMapping("/{seasonNumber}")
    fun createSeasonForScheduling(
        @PathVariable("seasonNumber") seasonNumber: Int,
    ): ResponseEntity<Season> = ResponseEntity.status(201).body(seasonService.createSeasonForScheduling(seasonNumber))
}
