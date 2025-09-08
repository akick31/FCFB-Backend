package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.service.fcfb.PlaybookStatsService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
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
     * Get filtered playbook stats with pagination
     */
    @GetMapping
    fun getFilteredPlaybookStats(
        @RequestParam(required = false) offensivePlaybook: OffensivePlaybook?,
        @RequestParam(required = false) defensivePlaybook: DefensivePlaybook?,
        @RequestParam(required = false) season: Int?,
        @PageableDefault(size = 20) pageable: Pageable,
    ) = playbookStatsService.getFilteredPlaybookStats(
        offensivePlaybook = offensivePlaybook,
        defensivePlaybook = defensivePlaybook,
        season = season,
        pageable = pageable,
    )

    /**
     * Generate all playbook stats
     */
    @PostMapping("/generate/all")
    fun generateAll() = playbookStatsService.generateAllPlaybookStats()
}
