package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.model.Game
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface GameRepository : CrudRepository<Game, Int>, JpaSpecificationExecutor<Game> {
    @Query(value = "SELECT * FROM game WHERE game_id =?", nativeQuery = true)
    fun getGameById(gameId: Int): Game?

    @Query(value = "SELECT * FROM game WHERE JSON_CONTAINS(request_message_id, ?, '\$')", nativeQuery = true)
    fun getGameByRequestMessageId(requestMessageId: String): Game?

    @Query(value = "SELECT * FROM game WHERE home_platform_id = :platformId OR away_platform_id = :platformId", nativeQuery = true)
    fun getGameByPlatformId(platformId: ULong): Game?

    @Query(value = "SELECT * FROM game WHERE game_type != 'SCRIMMAGE'", nativeQuery = true)
    fun getAllGames(): List<Game>

    @Query(value = "SELECT * FROM game WHERE game_type != 'SCRIMMAGE' AND game_id >= :gameId", nativeQuery = true)
    fun getAllGamesMoreRecentThanGameId(gameId: Int): List<Game>

    @Query(value = "SELECT * FROM game WHERE game_status != 'FINAL' AND game_type != 'SCRIMMAGE'", nativeQuery = true)
    fun getAllOngoingGames(): List<Game>

    @Query(
        "SELECT DISTINCT * FROM game WHERE (home_team_rank BETWEEN 1 AND 25 OR away_team_rank BETWEEN 1 AND 25)",
        nativeQuery = true,
    )
    fun getRankedGames(): List<Game>

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE (home_team = :team OR away_team = :team) " +
                "AND season = :season " +
                "AND week = :week " +
                "AND game_type != 'SCRIMMAGE'",
        nativeQuery = true,
    )
    fun getGamesByTeamSeasonAndWeek(
        team: String,
        season: Int,
        week: Int,
    ): Game?

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE season = :season " +
                "AND game_type != 'SCRIMMAGE' " +
                "AND ((home_team = :firstTeam AND away_team = :secondTeam) " +
                "     OR (home_team = :secondTeam AND away_team = :firstTeam))",
        nativeQuery = true,
    )
    fun getGamesBySeasonAndMatchup(
        season: Int,
        firstTeam: String,
        secondTeam: String,
    ): List<Game>

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE STR_TO_DATE(game_timer, '%m/%d/%Y %H:%i:%s') <= CONVERT_TZ(NOW(), 'UTC', 'America/New_York') " +
                "AND game_status != 'FINAL' " +
                "AND (game_warning = 'FIRST_WARNING' OR game_warning = 'SECOND_WARNING')",
        nativeQuery = true,
    )
    fun findExpiredTimers(): List<Game>

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE STR_TO_DATE(game_timer, '%m/%d/%Y %H:%i:%s') " +
                "BETWEEN DATE_ADD(CONVERT_TZ(NOW(), 'UTC', 'America/New_York'), INTERVAL 6 HOUR) " +
                "AND DATE_ADD(CONVERT_TZ(NOW(), 'UTC', 'America/New_York'), INTERVAL 12 HOUR) " +
                "AND game_status != 'FINAL' " +
                "AND game_warning = 'NONE'",
        nativeQuery = true,
    )
    fun findGamesToWarnFirstInstance(): List<Game>

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE STR_TO_DATE(game_timer, '%m/%d/%Y %H:%i:%s') " +
                "BETWEEN CONVERT_TZ(NOW(), 'UTC', 'America/New_York') " +
                "AND DATE_ADD(CONVERT_TZ(NOW(), 'UTC', 'America/New_York'), INTERVAL 6 HOUR) " +
                "AND game_status != 'FINAL' " +
                "AND game_warning = 'FIRST_WARNING'",
        nativeQuery = true,
    )
    fun findGamesToWarnSecondInstance(): List<Game>

    @Transactional
    @Modifying
    @Query(value = "UPDATE game SET game_warning = 'FIRST_WARNING' WHERE game_id = ?", nativeQuery = true)
    fun updateGameAsFirstWarning(gameId: Int)

    @Transactional
    @Modifying
    @Query(value = "UPDATE game SET game_warning = 'SECOND_WARNING' WHERE game_id = ?", nativeQuery = true)
    fun updateGameAsSecondWarning(gameId: Int)

    @Transactional
    @Modifying
    @Query(value = "UPDATE game SET close_game_pinged = true WHERE game_id = ?", nativeQuery = true)
    fun markCloseGamePinged(gameId: Int)

    @Transactional
    @Modifying
    @Query(value = "UPDATE game SET upset_alert_pinged = true WHERE game_id = ?", nativeQuery = true)
    fun markUpsetAlertPinged(gameId: Int)

    fun findByHomeTeam(homeTeam: String): List<Game>

    fun findByAwayTeam(awayTeam: String): List<Game>

    fun findByGameStatus(gameStatus: GameStatus): List<Game>

    fun findByGameType(gameType: GameType): List<Game>

    fun findByGameWarning(gameWarning: GameWarning): List<Game>

    fun findByCloseGamePinged(closeGamePinged: Boolean): List<Game>

    fun findByUpsetAlertPinged(upsetAlertPinged: Boolean): List<Game>

    @Query(
        value =
            "SELECT * FROM game " +
                "WHERE season = :season " +
                "AND week = :week " +
                "AND game_type != 'SCRIMMAGE'",
        nativeQuery = true,
    )
    fun getGamesBySeasonAndWeek(
        season: Int,
        week: Int,
    ): List<Game>
}
