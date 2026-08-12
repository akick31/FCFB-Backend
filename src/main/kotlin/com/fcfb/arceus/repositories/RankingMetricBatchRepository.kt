package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.RankingMetric
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types

@Repository
class RankingMetricBatchRepository(private val jdbcTemplate: JdbcTemplate) {
    fun batchInsert(rows: List<RankingMetric>) {
        if (rows.isEmpty()) return
        val sql = "INSERT INTO ranking_metric (season, week, metric_type, team_id, value, wins, losses) VALUES (?, ?, ?, ?, ?, ?, ?)"
        jdbcTemplate.batchUpdate(
            sql,
            rows,
            rows.size,
        ) { ps, row ->
            ps.setInt(1, row.season)
            ps.setInt(2, row.week)
            ps.setString(3, row.metricType?.name)
            ps.setInt(4, row.teamId)
            ps.setDouble(5, row.value)
            row.wins?.let { ps.setInt(6, it) } ?: ps.setNull(6, Types.INTEGER)
            row.losses?.let { ps.setInt(7, it) } ?: ps.setNull(7, Types.INTEGER)
        }
    }
}
