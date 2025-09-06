package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.LeagueStats
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface LeagueStatsRepository : CrudRepository<LeagueStats, Int>, JpaSpecificationExecutor<LeagueStats> {
    fun findAllByOrderBySeasonNumberDescSubdivisionAsc(): List<LeagueStats>

    fun findBySubdivisionAndSeasonNumber(
        subdivision: com.fcfb.arceus.enums.team.Subdivision,
        seasonNumber: Int,
    ): LeagueStats?

    fun findBySubdivisionOrderBySeasonNumberDesc(subdivision: com.fcfb.arceus.enums.team.Subdivision): List<LeagueStats>

    fun findBySeasonNumberOrderBySubdivisionAsc(seasonNumber: Int): List<LeagueStats>

    fun existsBySubdivisionAndSeasonNumber(
        subdivision: com.fcfb.arceus.enums.team.Subdivision,
        seasonNumber: Int,
    ): Boolean

    fun countBySubdivision(subdivision: com.fcfb.arceus.enums.team.Subdivision): Long

    fun countBySeasonNumber(seasonNumber: Int): Long
}
