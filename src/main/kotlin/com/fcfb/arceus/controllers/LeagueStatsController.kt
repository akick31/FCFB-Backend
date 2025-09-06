package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.LeagueStats
import com.fcfb.arceus.service.fcfb.LeagueStatsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/league-stats")
@CrossOrigin(origins = ["*"])
class LeagueStatsController(
    private val leagueStatsService: LeagueStatsService,
) {
    /**
     * Get all league stats with optional filtering
     */
    @GetMapping("/all")
    fun getAllLeagueStats(
        @RequestParam(required = false) subdivision: String?,
        @RequestParam(required = false) seasonNumber: Int?,
    ): ResponseEntity<List<LeagueStats>> {
        val allStats = leagueStatsService.getAllLeagueStats()

        val filteredStats =
            allStats.filter { stats ->
                subdivision?.let { stats.subdivision.name.equals(it, ignoreCase = true) } ?: true &&
                    seasonNumber?.let { stats.seasonNumber == it } ?: true
            }

        return ResponseEntity.ok(filteredStats)
    }

    /**
     * Get league stats for a specific subdivision and season
     */
    @GetMapping("")
    fun getLeagueStatsBySubdivisionAndSeason(
        @RequestParam subdivision: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<LeagueStats> {
        val subdivisionEnum =
            try {
                Subdivision.valueOf(subdivision.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val leagueStats = leagueStatsService.getLeagueStatsBySubdivisionAndSeason(subdivisionEnum, seasonNumber)
        return if (leagueStats != null) {
            ResponseEntity.ok(leagueStats)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all league stats for a specific subdivision
     */
    @GetMapping("/subdivision")
    fun getLeagueStatsBySubdivision(
        @RequestParam subdivision: String,
    ): ResponseEntity<List<LeagueStats>> {
        val subdivisionEnum =
            try {
                Subdivision.valueOf(subdivision.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val leagueStats = leagueStatsService.getLeagueStatsBySubdivision(subdivisionEnum)
        return ResponseEntity.ok(leagueStats)
    }

    /**
     * Get all league stats for a specific season
     */
    @GetMapping("/season")
    fun getLeagueStatsBySeason(
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<List<LeagueStats>> {
        val leagueStats = leagueStatsService.getLeagueStatsBySeason(seasonNumber)
        return ResponseEntity.ok(leagueStats)
    }

    /**
     * Generate all league stats (recalculate all league stats)
     */
    @PostMapping("/generate/all")
    fun generateAllLeagueStats(): ResponseEntity<String> {
        return try {
            leagueStatsService.generateAllLeagueStats()
            ResponseEntity.ok("All league stats generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating league stats: ${e.message}")
        }
    }

    /**
     * Generate league stats for a specific subdivision and season
     */
    @PostMapping("/generate/subdivision-season")
    fun generateLeagueStatsForSubdivisionAndSeason(
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
            leagueStatsService.generateLeagueStatsForSubdivisionAndSeason(subdivisionEnum, seasonNumber)
            ResponseEntity.ok("League stats for $subdivision in season $seasonNumber generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating league stats: ${e.message}")
        }
    }
}
