package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.PlaybookStats
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PlaybookStatsRepository : CrudRepository<PlaybookStats, Int> {
    fun findAllByOrderBySeasonNumberDescOffensivePlaybookAscDefensivePlaybookAsc(): List<PlaybookStats>

    fun findByOffensivePlaybookAndDefensivePlaybookAndSeasonNumber(
        offensivePlaybook: com.fcfb.arceus.enums.team.OffensivePlaybook,
        defensivePlaybook: com.fcfb.arceus.enums.team.DefensivePlaybook,
        seasonNumber: Int,
    ): PlaybookStats?

    fun findByOffensivePlaybookAndSeasonNumber(
        offensivePlaybook: com.fcfb.arceus.enums.team.OffensivePlaybook,
        seasonNumber: Int,
    ): List<PlaybookStats>

    fun findByDefensivePlaybookAndSeasonNumber(
        defensivePlaybook: com.fcfb.arceus.enums.team.DefensivePlaybook,
        seasonNumber: Int,
    ): List<PlaybookStats>

    fun findByOffensivePlaybookOrderBySeasonNumberDesc(offensivePlaybook: com.fcfb.arceus.enums.team.OffensivePlaybook): List<PlaybookStats>

    fun findByDefensivePlaybookOrderBySeasonNumberDesc(defensivePlaybook: com.fcfb.arceus.enums.team.DefensivePlaybook): List<PlaybookStats>

    fun findBySeasonNumberOrderByOffensivePlaybookAscDefensivePlaybookAsc(seasonNumber: Int): List<PlaybookStats>

    fun existsByOffensivePlaybookAndDefensivePlaybookAndSeasonNumber(
        offensivePlaybook: com.fcfb.arceus.enums.team.OffensivePlaybook,
        defensivePlaybook: com.fcfb.arceus.enums.team.DefensivePlaybook,
        seasonNumber: Int,
    ): Boolean

    fun existsByOffensivePlaybookAndSeasonNumber(
        offensivePlaybook: com.fcfb.arceus.enums.team.OffensivePlaybook,
        seasonNumber: Int,
    ): Boolean

    fun existsByDefensivePlaybookAndSeasonNumber(
        defensivePlaybook: com.fcfb.arceus.enums.team.DefensivePlaybook,
        seasonNumber: Int,
    ): Boolean

    fun countByOffensivePlaybook(offensivePlaybook: com.fcfb.arceus.enums.team.OffensivePlaybook): Long

    fun countByDefensivePlaybook(defensivePlaybook: com.fcfb.arceus.enums.team.DefensivePlaybook): Long

    fun countBySeasonNumber(seasonNumber: Int): Long
}
