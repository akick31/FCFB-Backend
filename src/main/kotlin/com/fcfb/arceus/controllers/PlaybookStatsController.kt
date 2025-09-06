package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.PlaybookStats
import com.fcfb.arceus.service.fcfb.PlaybookStatsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/playbook-stats")
@CrossOrigin(origins = ["*"])
class PlaybookStatsController(
    private val playbookStatsService: PlaybookStatsService,
) {
    /**
     * Get all playbook stats with optional filtering
     */
    @GetMapping("/all")
    fun getAllPlaybookStats(
        @RequestParam(required = false) offensivePlaybook: String?,
        @RequestParam(required = false) defensivePlaybook: String?,
        @RequestParam(required = false) seasonNumber: Int?,
    ): ResponseEntity<List<PlaybookStats>> {
        val allStats = playbookStatsService.getAllPlaybookStats()

        val filteredStats =
            allStats.filter { stats ->
                offensivePlaybook?.let { stats.offensivePlaybook.name.equals(it, ignoreCase = true) } ?: true &&
                    defensivePlaybook?.let { stats.defensivePlaybook.name.equals(it, ignoreCase = true) } ?: true &&
                    seasonNumber?.let { stats.seasonNumber == it } ?: true
            }

        return ResponseEntity.ok(filteredStats)
    }

    /**
     * Get playbook stats for a specific offensive playbook, defensive playbook, and season
     */
    @GetMapping("")
    fun getPlaybookStatsByOffensivePlaybookAndDefensivePlaybookAndSeason(
        @RequestParam offensivePlaybook: String,
        @RequestParam defensivePlaybook: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<PlaybookStats> {
        val offensivePlaybookEnum =
            try {
                OffensivePlaybook.valueOf(offensivePlaybook.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val defensivePlaybookEnum =
            try {
                DefensivePlaybook.valueOf(defensivePlaybook.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val playbookStats =
            playbookStatsService.getPlaybookStatsByOffensivePlaybookAndDefensivePlaybookAndSeason(
                offensivePlaybookEnum,
                defensivePlaybookEnum,
                seasonNumber,
            )
        return if (playbookStats != null) {
            ResponseEntity.ok(playbookStats)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all playbook stats for a specific offensive playbook
     */
    @GetMapping("/offensive-playbook")
    fun getPlaybookStatsByOffensivePlaybook(
        @RequestParam offensivePlaybook: String,
    ): ResponseEntity<List<PlaybookStats>> {
        val offensivePlaybookEnum =
            try {
                OffensivePlaybook.valueOf(offensivePlaybook.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val playbookStats = playbookStatsService.getPlaybookStatsByOffensivePlaybook(offensivePlaybookEnum)
        return ResponseEntity.ok(playbookStats)
    }

    /**
     * Get all playbook stats for a specific defensive playbook
     */
    @GetMapping("/defensive-playbook")
    fun getPlaybookStatsByDefensivePlaybook(
        @RequestParam defensivePlaybook: String,
    ): ResponseEntity<List<PlaybookStats>> {
        val defensivePlaybookEnum =
            try {
                DefensivePlaybook.valueOf(defensivePlaybook.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().build()
            }

        val playbookStats = playbookStatsService.getPlaybookStatsByDefensivePlaybook(defensivePlaybookEnum)
        return ResponseEntity.ok(playbookStats)
    }

    /**
     * Get all playbook stats for a specific season
     */
    @GetMapping("/season")
    fun getPlaybookStatsBySeason(
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<List<PlaybookStats>> {
        val playbookStats = playbookStatsService.getPlaybookStatsBySeason(seasonNumber)
        return ResponseEntity.ok(playbookStats)
    }

    /**
     * Generate all playbook stats (recalculate all playbook stats)
     */
    @PostMapping("/generate/all")
    fun generateAllPlaybookStats(): ResponseEntity<String> {
        return try {
            playbookStatsService.generateAllPlaybookStats()
            ResponseEntity.ok("All playbook stats generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating playbook stats: ${e.message}")
        }
    }

    /**
     * Generate playbook stats for a specific offensive playbook and season (all defensive playbooks)
     */
    @PostMapping("/generate/offensive-playbook-season")
    fun generatePlaybookStatsForOffensivePlaybookAndSeason(
        @RequestParam offensivePlaybook: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<String> {
        val offensivePlaybookEnum =
            try {
                OffensivePlaybook.valueOf(offensivePlaybook.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body("Invalid offensive playbook: $offensivePlaybook")
            }

        return try {
            playbookStatsService.generatePlaybookStatsForOffensivePlaybookAndSeason(offensivePlaybookEnum, seasonNumber)
            ResponseEntity.ok("Playbook stats for $offensivePlaybook in season $seasonNumber generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating playbook stats: ${e.message}")
        }
    }

    /**
     * Generate playbook stats for a specific defensive playbook and season (all offensive playbooks)
     */
    @PostMapping("/generate/defensive-playbook-season")
    fun generatePlaybookStatsForDefensivePlaybookAndSeason(
        @RequestParam defensivePlaybook: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<String> {
        val defensivePlaybookEnum =
            try {
                DefensivePlaybook.valueOf(defensivePlaybook.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body("Invalid defensive playbook: $defensivePlaybook")
            }

        return try {
            playbookStatsService.generatePlaybookStatsForDefensivePlaybookAndSeason(defensivePlaybookEnum, seasonNumber)
            ResponseEntity.ok("Playbook stats for $defensivePlaybook in season $seasonNumber generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating playbook stats: ${e.message}")
        }
    }

    /**
     * Generate playbook stats for a specific offensive playbook, defensive playbook, and season
     */
    @PostMapping("/generate/offensive-defensive-playbook-season")
    fun generatePlaybookStatsForOffensivePlaybookAndDefensivePlaybookAndSeason(
        @RequestParam offensivePlaybook: String,
        @RequestParam defensivePlaybook: String,
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<String> {
        val offensivePlaybookEnum =
            try {
                OffensivePlaybook.valueOf(offensivePlaybook.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body("Invalid offensive playbook: $offensivePlaybook")
            }

        val defensivePlaybookEnum =
            try {
                DefensivePlaybook.valueOf(defensivePlaybook.uppercase())
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body("Invalid defensive playbook: $defensivePlaybook")
            }

        return try {
            playbookStatsService.generatePlaybookStatsForOffensivePlaybookAndDefensivePlaybookAndSeason(
                offensivePlaybookEnum,
                defensivePlaybookEnum,
                seasonNumber,
            )
            ResponseEntity.ok("Playbook stats for $offensivePlaybook/$defensivePlaybook in season $seasonNumber generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating playbook stats: ${e.message}")
        }
    }
}
