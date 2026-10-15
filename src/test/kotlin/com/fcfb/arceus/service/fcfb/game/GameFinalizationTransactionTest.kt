package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.account.User
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.model.season.Season
import com.fcfb.arceus.model.statistics.GameStats
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.play.PlayRepository
import com.fcfb.arceus.repositories.schedule.ScheduleRepository
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.appearance.GameFieldService
import com.fcfb.arceus.service.fcfb.probability.WinProbabilityService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import com.fcfb.arceus.service.fcfb.season.SeasonService
import com.fcfb.arceus.service.fcfb.statistics.GameStatsService
import com.fcfb.arceus.service.fcfb.statistics.record.RecordService
import com.fcfb.arceus.service.fcfb.statistics.season.SeasonStatsService
import com.fcfb.arceus.service.fcfb.team.TeamService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GameFinalizationTransactionTest {
    private val dataSource = DriverManagerDataSource("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1", "sa", "")
    private val jdbc = JdbcTemplate(dataSource)
    private val games = mockk<GameRepository>()
    private val plays = mockk<PlayRepository>()
    private val teams = mockk<TeamService>(relaxed = true)
    private val users = mockk<UserService>(relaxed = true)
    private val discord = mockk<DiscordService>(relaxed = true)
    private val stats = mockk<GameStatsService>()
    private val seasons = mockk<SeasonService>(relaxed = true)
    private val schedules = mockk<ScheduleRepository>()
    private val fields = mockk<GameFieldService>()
    private val probability = mockk<WinProbabilityService>(relaxed = true)
    private val records = mockk<RecordService>()
    private val aggregates = mockk<SeasonStatsService>()
    private val scheduleService =
        ScheduleService(seasons, schedules, games, mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
    private val finalization =
        GameFinalizationService(
            games,
            plays,
            teams,
            discord,
            users,
            stats,
            seasons,
            scheduleService,
            records,
            aggregates,
            mockk(),
            mockk(relaxed = true),
            probability,
            fields,
            DataSourceTransactionManager(dataSource),
        )
    private val service =
        GameService(
            games,
            plays,
            teams,
            discord,
            users,
            stats,
            mockk(),
            probability,
            mockk(relaxed = true),
            finalization,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    private val notifications = mutableListOf<Int>()

    @BeforeEach
    fun prepareDatabase() {
        jdbc.execute("CREATE TABLE game_state (game_id INT PRIMARY KEY, status VARCHAR, home_score INT, away_score INT)")
        jdbc.execute("CREATE TABLE effects (game_id INT, label VARCHAR)")
        jdbc.execute("CREATE TABLE schedule_state (game_id INT PRIMARY KEY, finished BOOLEAN, home_score INT, away_score INT)")
        jdbc.execute("CREATE TABLE stats_state (game_id INT, team VARCHAR, status VARCHAR)")
        jdbc.update("INSERT INTO schedule_state VALUES (1, FALSE, 0, 0)")
        jdbc.update("INSERT INTO stats_state VALUES (1, 'Home 1', 'IN_PROGRESS'), (1, 'Away 1', 'IN_PROGRESS')")
        jdbc.update("INSERT INTO game_state VALUES (1, 'IN_PROGRESS', 21, 14)")
        every { games.getGameById(any()) } answers { storedGame(firstArg()) }
        every { games.getAllOngoingGames() } answers { listOf(storedGame(1), storedGame(2)) }
        every { games.save(any()) } answers {
            firstArg<Game>().also {
                jdbc.update(
                    "UPDATE game_state SET status = ?, home_score = ?, away_score = ? WHERE game_id = ?",
                    it.gameStatus!!.name, it.homeScore, it.awayScore, it.gameId,
                )
            }
        }
        every { fields.snapshotIfAbsent(any()) } answers {
            record(firstArg<Game>().gameId, "field")
            Unit
        }
        every { plays.getPreviousPlay(any()) } answers {
            val id = firstArg<Int>()
            Play().apply {
                gameId = id
                actualResult = if (effects(id).contains("terminal play")) ActualResult.END_OF_GAME else ActualResult.TOUCHDOWN
            }
        }
        every { plays.save(any()) } answers { firstArg<Play>().also { record(it.gameId, "terminal play") } }
        every { teams.updateTeamWinsAndLosses(any()) } answers {
            record(firstArg<Game>().gameId, "team records")
            Unit
        }
        every { users.updateUserWinsAndLosses(any()) } answers {
            record(firstArg<Game>().gameId, "user records")
            Unit
        }
        every { teams.getTeamByName(any()) } answers { Team().apply { name = firstArg<String>() } }
        every { users.getUserByDiscordId(any()) } answers {
            User().apply {
                id = 1L
                username = "coach"
                discordId = firstArg<String>()
            }
        }
        every { seasons.getCurrentSeason() } returns Season().apply { seasonNumber = 12 }
        every { plays.getUserAverageResponseTime(any(), any()) } returns 40.0
        every { schedules.findGameInSchedule(any(), any(), any(), any()) } answers {
            Schedule().apply {
                homeTeam = firstArg()
                awayTeam = secondArg()
                gameType = GameType.CONFERENCE_GAME
            }
        }
        every { schedules.save(any()) } answers {
            firstArg<Schedule>().also {
                val id = it.homeTeam!!.removePrefix("Home ").toInt()
                record(id, "schedule")
                jdbc.update(
                    "UPDATE schedule_state SET finished = ?, home_score = ?, away_score = ? WHERE game_id = ?",
                    it.finished, it.homeScore, it.awayScore, id,
                )
            }
        }
        every { schedules.checkIfWeekIsOver(any(), any()) } returns 0
        every { stats.getGameStatsByIdAndTeam(any(), any()) } answers {
            GameStats().apply {
                gameId = firstArg()
                team = secondArg()
            }
        }
        every { stats.saveGameStats(any()) } answers {
            firstArg<GameStats>().also {
                record(it.gameId!!, "${it.team} stats")
                jdbc.update("UPDATE stats_state SET status = ? WHERE game_id = ? AND team = ?", it.gameStatus!!.name, it.gameId, it.team)
            }
        }
        every { records.checkAndUpdateRecordsForGame(any()) } answers {
            record(firstArg<Game>().gameId, "records")
            Unit
        }
        every { aggregates.updateSeasonStatsForGame(any()) } answers {
            record(firstArg<GameStats>().gameId!!, "aggregate")
            Unit
        }
        every { discord.notifyFinalizationRollback(any()) } answers {
            val game = firstArg<Game>()
            assertEquals(GameStatus.IN_PROGRESS, storedGame(game.gameId).gameStatus)
            assertEquals(emptyList(), effects(game.gameId))
            assertEquals(
                false,
                jdbc.queryForObject("SELECT finished FROM schedule_state WHERE game_id = ?", Boolean::class.java, game.gameId),
            )
            assertEquals(
                listOf("IN_PROGRESS", "IN_PROGRESS"),
                jdbc.queryForList("SELECT status FROM stats_state WHERE game_id = ?", String::class.java, game.gameId),
            )
            notifications.add(game.gameId)
            Unit
        }
    }

    @AfterEach
    fun closeDatabase() {
        jdbc.execute("SHUTDOWN")
    }

    private fun record(
        id: Int,
        label: String,
    ) {
        jdbc.update("INSERT INTO effects VALUES (?, ?)", id, label)
    }

    private fun effects(id: Int): List<String> = jdbc.queryForList("SELECT label FROM effects WHERE game_id = ?", String::class.java, id)

    private fun storedGame(id: Int): Game =
        jdbc.queryForObject("SELECT * FROM game_state WHERE game_id = ?", { row, _ ->
            Game().apply {
                gameId = id
                homeTeam = "Home $id"
                awayTeam = "Away $id"
                homeScore = row.getInt("home_score")
                awayScore = row.getInt("away_score")
                gameStatus = GameStatus.valueOf(row.getString("status"))
                gameType = GameType.CONFERENCE_GAME
                season = 12
                week = 1
                homeCoachDiscordIds = listOf("coach")
            }
        }, id)!!

    @ParameterizedTest
    @ValueSource(strings = ["field", "elo", "coach", "response time", "schedule", "stats", "records", "aggregate"])
    fun `each failed finalization operation rolls back and notifies after rollback`(step: String) {
        val failure = IllegalStateException("failure at $step")
        when (step) {
            "field" ->
                every { fields.snapshotIfAbsent(any()) } answers {
                    record(1, "partial field")
                    throw failure
                }
            "elo" -> every { teams.updateCurrentElo(any()) } throws failure
            "coach" -> every { users.getUserByDiscordId(any()) } throws failure
            "response time" -> every { plays.getUserAverageResponseTime(any(), any()) } throws failure
            "schedule" ->
                every { schedules.save(any()) } answers {
                    record(1, "partial schedule")
                    throw failure
                }
            "stats" ->
                every { stats.saveGameStats(any()) } answers {
                    record(1, "partial stats")
                    throw failure
                }
            "records" -> every { records.checkAndUpdateRecordsForGame(any()) } throws failure
            "aggregate" ->
                every { aggregates.updateSeasonStatsForGame(any()) } answers {
                    record(1, "partial aggregate")
                    throw failure
                }
        }
        assertSame(failure, assertFailsWith<IllegalStateException> { finalization.endSingleGameByGameId(1) })
        assertEquals(GameStatus.IN_PROGRESS, storedGame(1).gameStatus)
        assertEquals(emptyList(), effects(1))
        assertEquals(listOf(1), notifications)
    }

    @Test
    fun `retry after rollback commits record writes once`() {
        val failure = IllegalStateException("stats unavailable")
        every { stats.getGameStatsByIdAndTeam(1, "Home 1") } throws failure andThen
            GameStats().apply {
                gameId = 1
                team = "Home 1"
            }
        assertFailsWith<IllegalStateException> { finalization.endSingleGameByGameId(1) }
        finalization.endSingleGameByGameId(1)
        assertEquals(GameStatus.FINAL, storedGame(1).gameStatus)
        assertEquals(1, effects(1).count { it == "team records" })
        assertEquals(1, effects(1).count { it == "terminal play" })
        assertEquals(1, effects(1).count { it == "schedule" })
        assertEquals(listOf(1), notifications)
    }

    @Test
    fun `automatic completion rollback includes the pre-final game save`() {
        every { records.checkAndUpdateRecordsForGame(any()) } throws IllegalStateException("records unavailable")
        assertFailsWith<IllegalStateException> {
            service.updateGameInformation(storedGame(1), Play(), 28, 14, TeamSide.HOME, 0, 0, 50, 1, 10, false, false, false)
        }
        assertEquals(GameStatus.IN_PROGRESS, storedGame(1).gameStatus)
        assertEquals(21, storedGame(1).homeScore)
        assertEquals(listOf(1), notifications)
    }

    @Test
    fun `channel-based completion uses the same rollback boundary`() {
        every { games.getGameByPlatformId(123uL) } answers { storedGame(1) }
        every { records.checkAndUpdateRecordsForGame(any()) } throws IllegalStateException("records unavailable")
        assertFailsWith<IllegalStateException> { finalization.endSingleGameByChannelId(123uL) }
        assertEquals(GameStatus.IN_PROGRESS, storedGame(1).gameStatus)
        assertEquals(listOf(1), notifications)
    }

    @Test
    fun `forfeit rollback includes adjusted scores`() {
        every { records.checkAndUpdateRecordsForGame(any()) } throws IllegalStateException("records unavailable")
        assertFailsWith<IllegalStateException> { finalization.endDOGOutGame(storedGame(1), 3 to 0) }
        assertEquals(14, storedGame(1).awayScore)
        assertEquals(GameStatus.IN_PROGRESS, storedGame(1).gameStatus)
        assertEquals(listOf(1), notifications)
    }

    @Test
    fun `batch failure preserves previously committed games`() {
        jdbc.update("INSERT INTO game_state VALUES (2, 'IN_PROGRESS', 21, 14)")
        jdbc.update("INSERT INTO schedule_state VALUES (2, FALSE, 0, 0)")
        jdbc.update("INSERT INTO stats_state VALUES (2, 'Home 2', 'IN_PROGRESS'), (2, 'Away 2', 'IN_PROGRESS')")
        every { records.checkAndUpdateRecordsForGame(match { it.gameId == 2 }) } throws IllegalStateException("second game failed")
        assertFailsWith<IllegalStateException> { finalization.endAllGames() }
        assertEquals(GameStatus.FINAL, storedGame(1).gameStatus)
        assertEquals(1, effects(1).count { it == "team records" })
        assertEquals(GameStatus.IN_PROGRESS, storedGame(2).gameStatus)
        assertEquals(emptyList(), effects(2))
        assertEquals(listOf(2), notifications)
    }

    @Test
    fun `successful commit does not report rollback`() {
        finalization.endSingleGameByGameId(1)
        assertEquals(GameStatus.FINAL, storedGame(1).gameStatus)
        assertEquals(true, jdbc.queryForObject("SELECT finished FROM schedule_state WHERE game_id = 1", Boolean::class.java))
        assertEquals(listOf("FINAL", "FINAL"), jdbc.queryForList("SELECT status FROM stats_state WHERE game_id = 1", String::class.java))
        verify(exactly = 0) { discord.notifyFinalizationRollback(any()) }
    }

    @Test
    fun `notification failure does not replace finalization error`() {
        val failure = IllegalStateException("records unavailable")
        every { records.checkAndUpdateRecordsForGame(any()) } throws failure
        every { discord.notifyFinalizationRollback(any()) } throws IllegalStateException("bot unavailable")
        assertSame(failure, assertFailsWith<IllegalStateException> { finalization.endSingleGameByGameId(1) })
        assertEquals(GameStatus.IN_PROGRESS, storedGame(1).gameStatus)
        assertEquals(emptyList(), effects(1))
    }
}
