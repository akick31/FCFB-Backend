package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.ConferenceStats
import com.fcfb.arceus.service.fcfb.ConferenceStatsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/conference-stats")
@CrossOrigin(origins = ["*"])
class ConferenceStatsController(
    private val conferenceStatsService: ConferenceStatsService,
) {
    /**
     * Get all conference stats with optional filtering
     */
    @GetMapping("/all")
    fun getAllConferenceStats(
        @RequestParam(required = false) subdivision: String?,
        @RequestParam(required = false) conference: String?,
        @RequestParam(required = false) seasonNumber: Int?,
    ): ResponseEntity<List<ConferenceStats>> {
        val allStats = conferenceStatsService.getAllConferenceStats()

        val filteredStats =
            allStats.filter { stats ->
                subdivision?.let { stats.subdivision.name.equals(it, ignoreCase = true) } ?: true &&
                    conference?.let { stats.conference.name.equals(it, ignoreCase = true) } ?: true &&
                    seasonNumber?.let { stats.seasonNumber == it } ?: true
            }

        return ResponseEntity.ok(filteredStats)
    }

    /**
     * Get conference stats for a specific subdivision, conference, and season
     */
    @GetMapping("")
    fun getConferenceStatsBySubdivisionAndConferenceAndSeason(
        @RequestParam subdivision: String,
        @RequestParam conference: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<ConferenceStats> {
        val subdivisionEnum =
            try {
                Subdivision.valueOf(subdivision.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val conferenceEnum =
            try {
                Conference.valueOf(conference.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val conferenceStats =
            conferenceStatsService.getConferenceStatsBySubdivisionAndConferenceAndSeason(
                subdivisionEnum,
                conferenceEnum,
                seasonNumber,
            )
        return if (conferenceStats != null) {
            ResponseEntity.ok(conferenceStats)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all conference stats for a specific subdivision
     */
    @GetMapping("/subdivision")
    fun getConferenceStatsBySubdivision(
        @RequestParam subdivision: String,
    ): ResponseEntity<List<ConferenceStats>> {
        val subdivisionEnum =
            try {
                Subdivision.valueOf(subdivision.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val conferenceStats = conferenceStatsService.getConferenceStatsBySubdivision(subdivisionEnum)
        return ResponseEntity.ok(conferenceStats)
    }

    /**
     * Get all conference stats for a specific conference
     */
    @GetMapping("/conference")
    fun getConferenceStatsByConference(
        @RequestParam conference: String,
    ): ResponseEntity<List<ConferenceStats>> {
        val conferenceEnum =
            try {
                Conference.valueOf(conference.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val conferenceStats = conferenceStatsService.getConferenceStatsByConference(conferenceEnum)
        return ResponseEntity.ok(conferenceStats)
    }

    /**
     * Get all conference stats for a specific season
     */
    @GetMapping("/season")
    fun getConferenceStatsBySeason(
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<List<ConferenceStats>> {
        val conferenceStats = conferenceStatsService.getConferenceStatsBySeason(seasonNumber)
        return ResponseEntity.ok(conferenceStats)
    }

    /**
     * Generate all conference stats (recalculate all conference stats)
     */
    @PostMapping("/generate/all")
    fun generateAllConferenceStats(): ResponseEntity<String> {
        return try {
            conferenceStatsService.generateAllConferenceStats()
            ResponseEntity.ok("All conference stats generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating conference stats: ${e.message}")
        }
    }

    /**
     * Generate conference stats for a specific subdivision and season (all conferences)
     */
    @PostMapping("/generate/subdivision-season")
    fun generateConferenceStatsForSubdivisionAndSeason(
        @RequestParam subdivision: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<String> {
        val subdivisionEnum =
            try {
                Subdivision.valueOf(subdivision.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body("Invalid subdivision: $subdivision")
            }

        return try {
            conferenceStatsService.generateConferenceStatsForSubdivisionAndSeason(subdivisionEnum, seasonNumber)
            ResponseEntity.ok("Conference stats for $subdivision in season $seasonNumber generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating conference stats: ${e.message}")
        }
    }

    /**
     * Generate conference stats for a specific subdivision, conference, and season
     */
    @PostMapping("/generate/subdivision-conference-season")
    fun generateConferenceStatsForSubdivisionAndConferenceAndSeason(
        @RequestParam subdivision: String,
        @RequestParam conference: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<String> {
        val subdivisionEnum =
            try {
                Subdivision.valueOf(subdivision.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body("Invalid subdivision: $subdivision")
            }

        val conferenceEnum =
            try {
                Conference.valueOf(conference.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body("Invalid conference: $conference")
            }

        return try {
            conferenceStatsService.generateConferenceStatsForSubdivisionAndConferenceAndSeason(
                subdivisionEnum,
                conferenceEnum,
                seasonNumber,
            )
            ResponseEntity.ok("Conference stats for $subdivision/$conference in season $seasonNumber generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating conference stats: ${e.message}")
        }
    }
}
