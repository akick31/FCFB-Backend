package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.TeamResumeMetric
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Types

@Repository
class TeamResumeMetricBatchRepository(private val jdbcTemplate: JdbcTemplate) {
    fun batchInsert(rows: List<TeamResumeMetric>) {
        if (rows.isEmpty()) return
        val sql =
            """
            INSERT INTO team_resume_metric (
                season, week, team_id,
                overall_wins, overall_losses, conference_wins, conference_losses,
                q1_wins, q1_losses, q2_wins, q2_losses, th_wins, th_losses, q4_wins, q4_losses,
                t25_wins, t25_losses, t50_wins, t50_losses, t100_wins, t100_losses,
                avg_opponent_composite_rank, composite_sos
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        jdbcTemplate.batchUpdate(
            sql,
            rows,
            rows.size,
        ) { ps, row ->
            ps.setInt(1, row.season)
            ps.setInt(2, row.week)
            ps.setInt(3, row.teamId)
            ps.setInt(4, row.overallWins)
            ps.setInt(5, row.overallLosses)
            ps.setInt(6, row.conferenceWins)
            ps.setInt(7, row.conferenceLosses)
            ps.setInt(8, row.q1Wins)
            ps.setInt(9, row.q1Losses)
            ps.setInt(10, row.q2Wins)
            ps.setInt(11, row.q2Losses)
            ps.setInt(12, row.thWins)
            ps.setInt(13, row.thLosses)
            ps.setInt(14, row.q4Wins)
            ps.setInt(15, row.q4Losses)
            ps.setInt(16, row.t25Wins)
            ps.setInt(17, row.t25Losses)
            ps.setInt(18, row.t50Wins)
            ps.setInt(19, row.t50Losses)
            ps.setInt(20, row.t100Wins)
            ps.setInt(21, row.t100Losses)
            row.avgOpponentCompositeRank?.let { ps.setDouble(22, it) } ?: ps.setNull(22, Types.DOUBLE)
            row.compositeSos?.let { ps.setDouble(23, it) } ?: ps.setNull(23, Types.DOUBLE)
        }
    }
}
