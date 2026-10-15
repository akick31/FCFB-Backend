package com.fcfb.arceus.repositories.statistics

import com.fcfb.arceus.model.statistics.CoachSeasonStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CoachSeasonStatsRepository : JpaRepository<CoachSeasonStats, Int> {
    fun findByUserId(userId: Long): List<CoachSeasonStats>

    fun findByUserIdOrderBySeasonNumberDescTeamAsc(userId: Long): List<CoachSeasonStats>

    fun findByUserIdAndTeamAndSeasonNumber(
        userId: Long,
        team: String,
        seasonNumber: Int,
    ): CoachSeasonStats?
}
