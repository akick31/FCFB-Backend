package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.PostseasonSeasonStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface PostseasonSeasonStatsRepository : JpaRepository<PostseasonSeasonStats, Int>, JpaSpecificationExecutor<PostseasonSeasonStats> {
    fun findByTeamAndSeasonNumber(
        team: String,
        seasonNumber: Int,
    ): PostseasonSeasonStats?

    fun findAllByOrderBySeasonNumberDescTeamAsc(): List<PostseasonSeasonStats>

    fun findBySeasonNumberOrderByTeamAsc(seasonNumber: Int): List<PostseasonSeasonStats>

    @Transactional
    @Modifying
    @Query("DELETE FROM PostseasonSeasonStats s WHERE s.team = :team AND s.seasonNumber = :seasonNumber")
    fun deleteByTeamAndSeasonNumber(
        @Param("team") team: String,
        @Param("seasonNumber") seasonNumber: Int,
    )
}
