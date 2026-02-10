package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Schedule
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface ScheduleRepository : CrudRepository<Schedule, Int> {
    @Query(value = "SELECT * FROM schedule WHERE season = ? AND week = ? AND started = false", nativeQuery = true)
    fun getGamesToStartBySeasonAndWeek(
        season: Int,
        week: Int,
    ): List<Schedule>?

    @Query(
        value = """
        SELECT CASE 
            WHEN home_team = :team THEN away_team 
            WHEN away_team = :team THEN home_team 
            ELSE NULL 
        END AS opponent
        FROM schedule 
        WHERE season = :season AND week = :week AND (home_team = :team OR away_team = :team)
    """,
        nativeQuery = true,
    )
    fun getTeamOpponent(
        season: Int,
        week: Int,
        team: String,
    ): String?

    @Query(
        value = "SELECT * FROM schedule WHERE season = :season AND (home_team = :team OR away_team = :team)",
        nativeQuery = true,
    )
    fun getScheduleBySeasonAndTeam(
        season: Int,
        team: String,
    ): List<Schedule>?

    @Query(
        value =
            "SELECT * FROM schedule " +
                "WHERE home_team = :homeTeam AND " +
                "away_team = :awayTeam AND " +
                "season = :season AND " +
                "week = :week",
        nativeQuery = true,
    )
    fun findGameInSchedule(
        homeTeam: String,
        awayTeam: String,
        season: Int,
        week: Int,
    ): Schedule?

    @Query(
        value = """
        SELECT 
            CASE 
                WHEN NOT EXISTS (
                    SELECT 1 
                    FROM schedule 
                    WHERE season = :season AND week = :week AND finished = false
                ) THEN true
                ELSE false
            END AS allFinished
        """,
        nativeQuery = true,
    )
    fun checkIfWeekIsOver(
        season: Int,
        week: Int,
    ): Int

    @Query(
        value = "SELECT * FROM schedule WHERE season = :season ORDER BY week, id",
        nativeQuery = true,
    )
    fun getScheduleBySeason(season: Int): List<Schedule>?

    @Query(
        value = """
        SELECT s.* FROM schedule s
        WHERE s.season = :season
          AND s.home_team IN (SELECT t.name FROM team t WHERE t.conference = :conference)
          AND s.away_team IN (SELECT t.name FROM team t WHERE t.conference = :conference)
        ORDER BY s.week, s.id
    """,
        nativeQuery = true,
    )
    fun getConferenceSchedule(
        season: Int,
        conference: String,
    ): List<Schedule>?

    @Query(
        value = "SELECT * FROM schedule WHERE season = :season AND week = :week ORDER BY id",
        nativeQuery = true,
    )
    fun getScheduleBySeasonAndWeek(
        season: Int,
        week: Int,
    ): List<Schedule>?

    @Modifying
    @Transactional
    @Query(
        value = "DELETE FROM schedule WHERE season = :season",
        nativeQuery = true,
    )
    fun deleteScheduleBySeason(season: Int)

    @Modifying
    @Transactional
    @Query(
        value = "DELETE FROM schedule WHERE season = :season AND game_type = :gameType",
        nativeQuery = true,
    )
    fun deleteScheduleBySeasonAndGameType(
        season: Int,
        gameType: String,
    )

    @Query(
        value = """
        SELECT * FROM schedule 
        WHERE season = :season AND week = :week 
        AND (home_team = :team OR away_team = :team)
    """,
        nativeQuery = true,
    )
    fun getScheduleBySeasonWeekAndTeam(
        season: Int,
        week: Int,
        team: String,
    ): Schedule?

    @Query(
        value = """
        SELECT * FROM schedule 
        WHERE season = :season 
        AND game_type IN ('PLAYOFFS', 'NATIONAL_CHAMPIONSHIP', 'CONFERENCE_CHAMPIONSHIP', 'BOWL')
        ORDER BY game_type, week, playoff_round, id
    """,
        nativeQuery = true,
    )
    fun getPostseasonSchedule(season: Int): List<Schedule>?

    fun findByHomeTeam(homeTeam: String): List<Schedule>

    fun findByAwayTeam(awayTeam: String): List<Schedule>

    fun findBySeason(season: Int): List<Schedule>

    fun findByWeek(week: Int): List<Schedule>

    fun findByGameType(gameType: GameType): List<Schedule>

    fun findByStarted(started: Boolean): List<Schedule>

    fun findByFinished(finished: Boolean): List<Schedule>
}
