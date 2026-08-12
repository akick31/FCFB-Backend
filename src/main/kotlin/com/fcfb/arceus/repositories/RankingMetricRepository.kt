package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.RankingMetric
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface RankingMetricRepository : CrudRepository<RankingMetric, Int> {
    @Query(
        value = """
            SELECT *
            FROM ranking_metric
            WHERE season = :season
            AND week = :week
            AND metric_type = :metricType
        """,
        nativeQuery = true,
    )
    fun findBySeasonWeekAndMetricType(
        season: Int,
        week: Int,
        metricType: String,
    ): List<RankingMetric>

    @Query(
        value = """
            SELECT *
            FROM ranking_metric
            WHERE season = :season
            AND team_id = :teamId
            AND metric_type = :metricType
            ORDER BY week ASC
        """,
        nativeQuery = true,
    )
    fun findByTeamAndMetricType(
        season: Int,
        teamId: Int,
        metricType: String,
    ): List<RankingMetric>

    @Query(
        value = """
            SELECT DISTINCT week
            FROM ranking_metric
            WHERE season = :season
            AND metric_type = :metricType
            ORDER BY week ASC
        """,
        nativeQuery = true,
    )
    fun findWeeks(
        season: Int,
        metricType: String,
    ): List<Int>

    @Query(
        value = """
            SELECT COUNT(*)
            FROM ranking_metric
            WHERE season = :season
            AND week = :week
            AND metric_type = :metricType
        """,
        nativeQuery = true,
    )
    fun existsForWeek(
        season: Int,
        week: Int,
        metricType: String,
    ): Int

    @Modifying
    @Transactional
    @Query(
        value = """
            DELETE FROM ranking_metric
            WHERE season = :season
            AND week = :week
            AND metric_type = :metricType
        """,
        nativeQuery = true,
    )
    fun deleteBySeasonWeekAndMetricType(
        season: Int,
        week: Int,
        metricType: String,
    )

    @Modifying
    @Transactional
    @Query(
        value = """
            DELETE FROM ranking_metric
            WHERE season = :season
            AND week = :week
        """,
        nativeQuery = true,
    )
    fun deleteBySeasonAndWeek(
        season: Int,
        week: Int,
    )
}
