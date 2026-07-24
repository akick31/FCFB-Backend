package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.Ranking
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface RankingRepository : CrudRepository<Ranking, Int> {
    @Query(
        value = """
            SELECT *
            FROM ranking
            WHERE season = :season
            AND week = :week
            AND poll_type = :pollType
            ORDER BY poll_rank ASC
        """,
        nativeQuery = true,
    )
    fun findBySeasonWeekAndPollType(
        season: Int,
        week: Int,
        pollType: String,
    ): List<Ranking>

    @Query(
        value = """
            SELECT DISTINCT week
            FROM ranking
            WHERE season = :season
            AND poll_type = :pollType
            ORDER BY week ASC
        """,
        nativeQuery = true,
    )
    fun findWeeks(
        season: Int,
        pollType: String,
    ): List<Int>

    @Query(
        value = """
            SELECT COUNT(*)
            FROM ranking
            WHERE season = :season
            AND week = :week
            AND poll_type = :pollType
        """,
        nativeQuery = true,
    )
    fun existsForWeek(
        season: Int,
        week: Int,
        pollType: String,
    ): Int

    @Modifying
    @Transactional
    @Query(
        value = """
            DELETE FROM ranking
            WHERE season = :season
            AND week = :week
            AND poll_type = :pollType
        """,
        nativeQuery = true,
    )
    fun deleteBySeasonWeekAndPollType(
        season: Int,
        week: Int,
        pollType: String,
    )
}
