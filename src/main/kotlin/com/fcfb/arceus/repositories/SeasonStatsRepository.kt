package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.SeasonStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SeasonStatsRepository : JpaRepository<SeasonStats, Int>, JpaSpecificationExecutor<SeasonStats> {
    /**
     * Find all season stats ordered by season and team
     */
    fun findAllByOrderBySeasonNumberDescTeamAsc(): List<SeasonStats>

    /**
     * Find season stats by team and season
     */
    fun findByTeamAndSeasonNumber(
        team: String,
        seasonNumber: Int,
    ): SeasonStats?

    /**
     * Find all season stats for a specific team
     */
    fun findByTeamOrderBySeasonNumberDesc(team: String): List<SeasonStats>

    /**
     * Find all season stats for a specific season
     */
    fun findBySeasonNumberOrderByTeamAsc(seasonNumber: Int): List<SeasonStats>

    /**
     * Find season stats by subdivision and season
     */
    fun findBySubdivisionAndSeasonNumber(
        subdivision: String,
        seasonNumber: Int,
    ): List<SeasonStats>

    /**
     * Find top teams by wins in a season
     */
    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.wins DESC, s.losses ASC")
    fun findTopTeamsByWins(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    /**
     * Find top teams by total yards in a season
     */
    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.totalYards DESC")
    fun findTopTeamsByTotalYards(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    /**
     * Find top teams by passing yards in a season
     */
    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.passYards DESC")
    fun findTopTeamsByPassYards(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    /**
     * Find top teams by rushing yards in a season
     */
    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.rushYards DESC")
    fun findTopTeamsByRushYards(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    /**
     * Find top teams by touchdowns in a season
     */
    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.touchdowns DESC")
    fun findTopTeamsByTouchdowns(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    /**
     * Find top teams by turnover differential in a season
     */
    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.turnoverDifferential DESC")
    fun findTopTeamsByTurnoverDifferential(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    /**
     * Delete all season stats for a specific season
     */
    fun deleteBySeasonNumber(seasonNumber: Int)

    /**
     * Delete season stats for a specific team and season
     */
    fun deleteByTeamAndSeasonNumber(
        team: String,
        seasonNumber: Int,
    )

    /**
     * Check if season stats exist for a team and season
     */
    fun existsByTeamAndSeasonNumber(
        team: String,
        seasonNumber: Int,
    ): Boolean

    /**
     * Delete all season stats for a specific team
     */
    fun deleteByTeam(team: String)

    /**
     * Check if season stats exist for a team
     */
    fun existsByTeam(team: String): Boolean

    /**
     * Check if season stats exist for a season
     */
    fun existsBySeasonNumber(seasonNumber: Int): Boolean

    /**
     * Count season stats for a specific team
     */
    fun countByTeam(team: String): Long

    /**
     * Count season stats for a specific season
     */
    fun countBySeasonNumber(seasonNumber: Int): Long
}
