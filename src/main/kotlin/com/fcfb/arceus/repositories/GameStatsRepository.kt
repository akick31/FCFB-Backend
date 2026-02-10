package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.GameStats
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface GameStatsRepository : CrudRepository<GameStats, Int> {
    @Query(value = "SELECT * FROM game_stats WHERE game_id = ? and team = ?", nativeQuery = true)
    fun getGameStatsByIdAndTeam(
        gameId: Int,
        team: String,
    ): GameStats?

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM game_stats WHERE game_id = ?", nativeQuery = true)
    fun deleteByGameId(gameId: Int)

    fun findByGameId(gameId: Int): List<GameStats>

    fun findByTeam(team: String): List<GameStats>

    fun findByTeamAndSeason(
        team: String,
        season: Int,
    ): List<GameStats>

    fun findAllByOrderBySeasonDescGameIdAsc(): List<GameStats>

    fun findBySeasonOrderByGameIdAsc(season: Int): List<GameStats>

    @Query(
        value = "SELECT * FROM game_stats WHERE season >= :minSeason ORDER BY season DESC, game_id ASC",
        nativeQuery = true,
    )
    fun findBySeasonGreaterThanEqualOrderBySeasonDescGameIdAsc(minSeason: Int): List<GameStats>

    @Query(
        value = "SELECT MAX(season) FROM game_stats",
        nativeQuery = true,
    )
    fun findMaxSeason(): Int?

    @Query(
        value =
            "SELECT gs.* FROM game_stats gs " +
                "JOIN game g ON gs.game_id = g.game_id " +
                "WHERE g.season = :season " +
                "AND g.week = :week " +
                "AND g.game_type != 'SCRIMMAGE'",
        nativeQuery = true,
    )
    fun getGameStatsBySeasonAndWeek(
        season: Int,
        week: Int,
    ): List<GameStats>
}
