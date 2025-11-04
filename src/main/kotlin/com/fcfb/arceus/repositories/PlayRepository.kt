package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.model.Play
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface PlayRepository : CrudRepository<Play, Int> {
    @Query(value = "SELECT * FROM play WHERE play_id =?", nativeQuery = true)
    fun getPlayById(playId: Int): Play?

    @Query(value = "SELECT * FROM play WHERE game_id = ? ORDER BY play_id DESC", nativeQuery = true)
    fun getAllPlaysByGameId(gameId: Int): List<Play>

    @Query(
        value =
            "SELECT play.* " +
                "FROM play " +
                "JOIN game g ON play.game_id = g.game_id " +
                "WHERE (offensive_submitter = :discordTag OR defensive_submitter = :discordTag)" +
                " AND g.game_type != 'SCRIMMAGE' " +
                "ORDER BY play_id DESC;",
        nativeQuery = true,
    )
    fun getAllPlaysByDiscordTag(discordTag: String): List<Play>

    @Query(value = "SELECT * FROM play WHERE game_id = ? AND play_finished = false ORDER BY play_id DESC LIMIT 1", nativeQuery = true)
    fun getCurrentPlay(gameId: Int): Play?

    @Query(value = "SELECT * FROM play WHERE game_id = ? AND play_finished = true ORDER BY play_id DESC LIMIT 1", nativeQuery = true)
    fun getPreviousPlay(gameId: Int): Play?

    @Query(
        value = "SELECT COUNT(*) FROM play WHERE game_id = :gameId AND result = 'DELAY OF GAME ON HOME TEAM'",
        nativeQuery = true,
    )
    fun getHomeDelayOfGameInstances(gameId: Int): Int?

    @Query(
        value = "SELECT COUNT(*) FROM play WHERE game_id = :gameId AND result = 'DELAY OF GAME ON AWAY TEAM'",
        nativeQuery = true,
    )
    fun getAwayDelayOfGameInstances(gameId: Int): Int?

    @Query(
        value =
            "SELECT AVG(" +
                "CASE " +
                "WHEN p.offensive_submitter_id = :discordId THEN p.offensive_response_speed " +
                "WHEN p.defensive_submitter_id = :discordId THEN p.defensive_response_speed " +
                "END " +
                ") AS avg_response_time " +
                "FROM play p " +
                "JOIN game g ON p.game_id = g.game_id " +
                "WHERE (p.offensive_submitter_id = :discordId OR p.defensive_submitter_id = :discordId) " +
                "AND g.season = :season",
        nativeQuery = true,
    )
    fun getUserAverageResponseTime(
        discordId: String,
        season: Int,
    ): Double?

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM play WHERE game_id =?", nativeQuery = true)
    fun deleteAllPlaysByGameId(gameId: Int)

    fun findByGameId(gameId: Int): List<Play>

    fun findByOffensiveSubmitter(offensiveSubmitter: String): List<Play>

    fun findByDefensiveSubmitter(defensiveSubmitter: String): List<Play>

    fun findByPlayCall(playCall: PlayCall): List<Play>

    fun findByActualResult(actualResult: ActualResult): List<Play>

    fun findByResult(result: Scenario): List<Play>

    fun findByPlayFinished(playFinished: Boolean): List<Play>
}
