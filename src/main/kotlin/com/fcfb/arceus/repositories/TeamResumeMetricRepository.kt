package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.TeamResumeMetric
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TeamResumeMetricRepository : CrudRepository<TeamResumeMetric, Int> {
    @Query(
        value = """
            SELECT *
            FROM team_resume_metric
            WHERE season = :season
            AND week = :week
        """,
        nativeQuery = true,
    )
    fun findBySeasonAndWeek(
        season: Int,
        week: Int,
    ): List<TeamResumeMetric>

    @Modifying
    @Transactional
    @Query(
        value = """
            DELETE FROM team_resume_metric
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
