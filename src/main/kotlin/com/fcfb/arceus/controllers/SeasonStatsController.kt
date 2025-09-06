package com.fcfb.arceus.controllers

import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.service.fcfb.SeasonStatsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/season-stats")
@CrossOrigin(origins = ["*"])
class SeasonStatsController(
    private val seasonStatsService: SeasonStatsService,
) {
    /**
     * Get all season stats with optional filtering
     */
    @GetMapping("/all")
    fun getAllSeasonStats(
        @RequestParam(required = false) team: String?,
        @RequestParam(required = false) seasonNumber: Int?,
        @RequestParam(required = false) subdivision: String?,
    ): ResponseEntity<List<SeasonStats>> {
        val allStats = seasonStatsService.getAllSeasonStats()

        val filteredStats =
            allStats.filter { stats ->
                team?.let { stats.team.equals(it, ignoreCase = true) } ?: true &&
                    seasonNumber?.let { stats.seasonNumber == it } ?: true &&
                    subdivision?.let { stats.subdivision?.name.equals(it, ignoreCase = true) } ?: true
            }

        return ResponseEntity.ok(filteredStats)
    }

    /**
     * Get season stats for a specific team and season
     */
    @GetMapping("")
    fun getSeasonStatsByTeamAndSeason(
        @RequestParam team: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<SeasonStats> {
        val seasonStats = seasonStatsService.getSeasonStatsByTeamAndSeason(team, seasonNumber)
        return if (seasonStats != null) {
            ResponseEntity.ok(seasonStats)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all season stats for a specific team
     */
    @GetMapping("/team")
    fun getSeasonStatsByTeam(
        @RequestParam team: String,
    ): ResponseEntity<List<SeasonStats>> {
        val seasonStats = seasonStatsService.getSeasonStatsByTeam(team)
        return ResponseEntity.ok(seasonStats)
    }

    /**
     * Get all season stats for a specific season
     */
    @GetMapping("/season")
    fun getSeasonStatsBySeason(
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<List<SeasonStats>> {
        val seasonStats = seasonStatsService.getSeasonStatsBySeason(seasonNumber)
        return ResponseEntity.ok(seasonStats)
    }

    /**
     * Generate all season stats (recalculate all season stats)
     */
    @PostMapping("/generate/all")
    fun generateAllSeasonStats(): ResponseEntity<String> {
        return try {
            seasonStatsService.generateAllSeasonStats()
            ResponseEntity.ok("All season stats generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating season stats: ${e.message}")
        }
    }

    /**
     * Generate season stats for a specific team and season
     */
    @PostMapping("/generate/team-season")
    fun generateSeasonStatsForTeam(
        @RequestParam team: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<String> {
        return try {
            seasonStatsService.generateSeasonStatsForTeam(team, seasonNumber)
            ResponseEntity.ok("Season stats for $team in season $seasonNumber generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating season stats: ${e.message}")
        }
    }

    /**
     * Get leaderboard for a specific stat
     */
    @GetMapping("/leaderboard")
    fun getLeaderboard(
        @RequestParam statName: String,
        @RequestParam(required = false) seasonNumber: Int?,
        @RequestParam(required = false) subdivision: String?,
        @RequestParam(required = false) conference: String?,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "false") ascending: Boolean,
    ): ResponseEntity<List<SeasonStats>> {
        return try {
            val leaderboard =
                seasonStatsService.getLeaderboard(
                    statName = statName,
                    seasonNumber = seasonNumber,
                    subdivision = subdivision,
                    conference = conference,
                    limit = limit,
                    ascending = ascending,
                )
            ResponseEntity.ok(leaderboard)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }
}
