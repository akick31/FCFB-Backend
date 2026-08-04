package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.SeasonStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface SeasonStatsRepository : JpaRepository<SeasonStats, Int>, JpaSpecificationExecutor<SeasonStats> {
    fun findAllByOrderBySeasonNumberDescTeamAsc(): List<SeasonStats>

    fun findByTeamAndSeasonNumber(
        team: String,
        seasonNumber: Int,
    ): SeasonStats?

    fun findByTeamOrderBySeasonNumberDesc(team: String): List<SeasonStats>

    fun findBySeasonNumberOrderByTeamAsc(seasonNumber: Int): List<SeasonStats>

    fun findBySubdivisionAndSeasonNumber(
        subdivision: String,
        seasonNumber: Int,
    ): List<SeasonStats>

    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.wins DESC, s.losses ASC")
    fun findTopTeamsByWins(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.totalYards DESC")
    fun findTopTeamsByTotalYards(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.passYards DESC")
    fun findTopTeamsByPassYards(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.rushYards DESC")
    fun findTopTeamsByRushYards(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.touchdowns DESC")
    fun findTopTeamsByTouchdowns(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    @Query("SELECT s FROM SeasonStats s WHERE s.seasonNumber = :seasonNumber ORDER BY s.turnoverDifferential DESC")
    fun findTopTeamsByTurnoverDifferential(
        @Param("seasonNumber") seasonNumber: Int,
    ): List<SeasonStats>

    fun deleteBySeasonNumber(seasonNumber: Int)

    @Transactional
    @Modifying
    @Query("DELETE FROM SeasonStats s WHERE s.team = :team AND s.seasonNumber = :seasonNumber")
    fun deleteByTeamAndSeasonNumber(
        @Param("team") team: String,
        @Param("seasonNumber") seasonNumber: Int,
    )

    fun existsByTeamAndSeasonNumber(
        team: String,
        seasonNumber: Int,
    ): Boolean

    fun deleteByTeam(team: String)

    fun existsByTeam(team: String): Boolean

    fun existsBySeasonNumber(seasonNumber: Int): Boolean

    fun countByTeam(team: String): Long

    fun countBySeasonNumber(seasonNumber: Int): Long
}
