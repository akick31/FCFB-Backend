package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.dto.request.ConferenceScheduleRequest
import com.fcfb.arceus.dto.response.ScheduleGenJobStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Conference
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.service.fcfb.season.SeasonService
import com.fcfb.arceus.service.fcfb.team.TeamService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class ConferenceScheduleGenerationServiceTest {
    private val seasonService = mockk<SeasonService>()
    private val repository = mockk<ScheduleRepository>()
    private val teamService = mockk<TeamService>()
    private val conferenceRepository = mockk<ConferenceRepository>()
    private val rulesService = mockk<ConferenceRulesService>()
    private val dataSource = DriverManagerDataSource("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1", "sa", "")
    private val jdbc = JdbcTemplate(dataSource)
    private val service =
        ConferenceScheduleGenerationService(
            seasonService,
            repository,
            teamService,
            DataSourceTransactionManager(dataSource),
        )

    private val jobService = ConferenceScheduleGenerationJobService(service, teamService, conferenceRepository, rulesService)

    @AfterEach
    fun closeDatabase() {
        jobService.stopWorkers()
        jdbc.execute("SHUTDOWN")
    }

    private fun storedGames(): List<String> = jdbc.queryForList("SELECT marker FROM schedule ORDER BY home_team", String::class.java)

    private fun persistGenerated(games: List<Schedule>): List<Schedule> {
        games.forEach { jdbc.update("INSERT INTO schedule VALUES (?, ?, ?)", it.homeTeam, it.awayTeam, "generated") }
        return games
    }

    private val previousGame =
        Schedule().apply {
            season = 11
            homeTeam = "A"
            awayTeam = "B"
            gameType = GameType.CONFERENCE_GAME
        }
    private val currentGame =
        Schedule().apply {
            season = 12
            homeTeam = "A"
            awayTeam = "B"
            gameType = GameType.CONFERENCE_GAME
        }

    private fun stubGeneration(names: List<String>) {
        val teams =
            names.mapIndexed { index, name ->
                Team().apply {
                    this.name = name
                    division = if (index < 2) "EAST" else "WEST"
                }
            }
        every { teamService.getTeamsInConference("TEST") } returns teams
        every { seasonService.isScheduleLocked(12) } returns false
        every { repository.getScheduleBySeason(any()) } returns emptyList()
        every { repository.getScheduleBySeason(12) } returns listOf(currentGame)
        every { repository.getScheduleBySeasonAndTeam(12, any()) } returns emptyList()
        jdbc.execute("CREATE TABLE schedule (home_team VARCHAR(50), away_team VARCHAR(50), marker VARCHAR(50))")
        jdbc.update("INSERT INTO schedule VALUES ('A', 'B', 'original')")
        every { repository.delete(any()) } answers {
            val game = firstArg<Schedule>()
            jdbc.update("DELETE FROM schedule WHERE home_team = ? AND away_team = ?", game.homeTeam, game.awayTeam)
            Unit
        }
        every { repository.saveAll(any<List<Schedule>>()) } answers { persistGenerated(firstArg()) }
    }

    private fun request(divisions: List<String> = emptyList()) =
        ConferenceScheduleRequest(
            season = 12,
            conference = "TEST",
            subdivision = Subdivision.FCFB,
            numConferenceGames = if (divisions.isEmpty()) 1 else 2,
            divisions = divisions,
        )

    @Test
    fun `successful history read alternates the previous home team`() {
        stubGeneration(listOf("A", "B"))
        every { repository.getScheduleBySeason(11) } returns listOf(previousGame)
        val generated = service.generateConferenceSchedule(request())
        assertEquals(1, generated.size)
        assertEquals("B", generated.single().homeTeam)
        assertEquals("A", generated.single().awayTeam)
        assertEquals(listOf("generated"), storedGames())
    }

    @Test
    fun `absent history retains normal home-away balancing`() {
        stubGeneration(listOf("A", "B"))
        every { repository.getScheduleBySeason(11) } returns null
        val generated = service.generateConferenceSchedule(request())
        assertEquals("A", generated.single().homeTeam)
        assertEquals("B", generated.single().awayTeam)
    }

    @Test
    fun `failed home-away history read propagates and restores existing schedule`() {
        stubGeneration(listOf("A", "B"))
        val failure = DataAccessResourceFailureException("history unavailable")
        every { repository.getScheduleBySeason(11) } throws failure
        assertSame(failure, assertThrows(DataAccessResourceFailureException::class.java) { service.generateConferenceSchedule(request()) })
        assertEquals(listOf("original"), storedGames())
        verifyOrder {
            repository.delete(currentGame)
            repository.getScheduleBySeason(11)
        }
        verify(exactly = 0) { repository.saveAll(any<List<Schedule>>()) }
    }

    @Test
    fun `failed opponent history read propagates and restores existing schedule`() {
        stubGeneration(listOf("A", "B", "C", "D"))
        val failure = DataAccessResourceFailureException("history unavailable")
        every { repository.getScheduleBySeason(11) } throws failure
        assertSame(
            failure,
            assertThrows(DataAccessResourceFailureException::class.java) {
                service.generateConferenceSchedule(request(listOf("EAST", "WEST")))
            },
        )
        assertEquals(listOf("original"), storedGames())
        verifyOrder {
            repository.delete(currentGame)
            repository.getScheduleBySeason(11)
        }
        verify(exactly = 0) { repository.saveAll(any<List<Schedule>>()) }
    }

    @Test
    fun `save failure rolls back deletion and partially written replacement`() {
        stubGeneration(listOf("A", "B"))
        val failure = DataAccessResourceFailureException("save unavailable")
        every { repository.saveAll(any<List<Schedule>>()) } answers {
            persistGenerated(firstArg())
            throw failure
        }
        assertSame(failure, assertThrows(DataAccessResourceFailureException::class.java) { service.generateConferenceSchedule(request()) })
        assertEquals(listOf("original"), storedGames())
    }

    @Test
    fun `background generation rolls back failed conference and commits next conference`() {
        stubGeneration(listOf("A", "B"))
        val otherGame =
            Schedule().apply {
                season = 12
                homeTeam = "C"
                awayTeam = "D"
                gameType = GameType.CONFERENCE_GAME
            }
        jdbc.update("INSERT INTO schedule VALUES ('C', 'D', 'other original')")
        every { repository.getScheduleBySeason(12) } returns listOf(currentGame, otherGame)
        every { teamService.getTeamsInConference("OTHER") } returns listOf(Team().apply { name = "C" }, Team().apply { name = "D" })
        every { conferenceRepository.findAllByActiveTrueOrderByLabelAsc() } returns
            listOf(
                Conference().apply {
                    code = "TEST"
                    label = "Test"
                },
                Conference().apply {
                    code = "OTHER"
                    label = "Other"
                },
            )
        every { rulesService.getConferenceRules(any()) } returns null
        val historyReads = AtomicInteger()
        every { repository.getScheduleBySeason(11) } answers {
            if (historyReads.incrementAndGet() == 1) throw DataAccessResourceFailureException("history unavailable")
            emptyList()
        }
        val response = jobService.startAllConferenceGenerationAsync(12)
        runBlocking {
            withTimeout(10_000) {
                while (jobService.getScheduleGenJobStatus(response.jobId)!!.status != ScheduleGenJobStatus.COMPLETED) delay(10)
            }
        }
        val job = jobService.getScheduleGenJobStatus(response.jobId)!!
        assertEquals(ScheduleGenJobStatus.COMPLETED, job.status)
        assertEquals(1, job.failedConferences)
        assertEquals(1, job.completedConferences)
        assertEquals(1, job.totalGamesGenerated)
        assertEquals(listOf("TEST", "OTHER"), job.logs.map { it.conference })
        assertEquals(listOf("FAILED", "SUCCESS"), job.logs.map { it.status })
        assertEquals(listOf("original", "generated"), storedGames())
    }

    @Test
    fun `status response is detached from registry state`() {
        stubGeneration(listOf("A", "B"))
        every { conferenceRepository.findAllByActiveTrueOrderByLabelAsc() } returns
            listOf(
                Conference().apply {
                    code = "TEST"
                    label = "Test"
                },
            )
        every { rulesService.getConferenceRules(any()) } returns null
        val response = jobService.startAllConferenceGenerationAsync(12)
        runBlocking {
            withTimeout(5000) {
                while (jobService.getScheduleGenJobStatus(response.jobId)!!.status != ScheduleGenJobStatus.COMPLETED) delay(10)
            }
        }
        val captured = jobService.getScheduleGenJobStatus(response.jobId)!!
        assertNotSame(captured, jobService.getScheduleGenJobStatus(response.jobId))
        captured.logs.clear()
        captured.completedConferences = 99
        val fresh = jobService.getScheduleGenJobStatus(response.jobId)!!
        assertEquals(1, fresh.completedConferences)
        assertEquals(1, fresh.logs.size)
    }

    @Test
    fun `context close drains current transaction and leaves remaining conference untouched`() {
        stubGeneration(listOf("A", "B"))
        val otherGame =
            Schedule().apply {
                season = 12
                homeTeam = "C"
                awayTeam = "D"
                gameType = GameType.CONFERENCE_GAME
            }
        jdbc.update("INSERT INTO schedule VALUES ('C', 'D', 'other original')")
        every { repository.getScheduleBySeason(12) } returns listOf(currentGame, otherGame)
        every { teamService.getTeamsInConference("OTHER") } returns listOf(Team().apply { name = "C" }, Team().apply { name = "D" })
        every { conferenceRepository.findAllByActiveTrueOrderByLabelAsc() } returns
            listOf(
                Conference().apply {
                    code = "TEST"
                    label = "Test"
                },
                Conference().apply {
                    code = "OTHER"
                    label = "Other"
                },
            )
        every { rulesService.getConferenceRules(any()) } returns null
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        every { repository.getScheduleBySeason(11) } answers {
            entered.countDown()
            check(release.await(10, TimeUnit.SECONDS))
            emptyList()
        }
        val context =
            AnnotationConfigApplicationContext().apply {
                registerBean("conferenceJobs", ConferenceScheduleGenerationJobService::class.java, Supplier { jobService })
                refresh()
            }
        val response = jobService.startAllConferenceGenerationAsync(12)
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        val captured = jobService.getScheduleGenJobStatus(response.jobId)!!
        val closing = CompletableFuture.runAsync { context.close() }
        try {
            val shutdown =
                jobService.javaClass.getDeclaredField("shutdownSignal").apply { isAccessible = true }
                    .get(jobService) as CompletableDeferred<*>
            runBlocking { withTimeout(5000) { shutdown.await() } }
            assertFalse(closing.isDone)
            assertThrows(IllegalStateException::class.java) { jobService.startAllConferenceGenerationAsync(12) }
        } finally {
            release.countDown()
            closing.get(10, TimeUnit.SECONDS)
        }
        val finished = jobService.getScheduleGenJobStatus(response.jobId)!!
        assertEquals(ScheduleGenJobStatus.FAILED, finished.status)
        assertEquals(1, finished.completedConferences)
        assertEquals(0, finished.failedConferences)
        assertEquals(1, finished.totalGamesGenerated)
        assertEquals(listOf("TEST"), finished.logs.map { it.conference })
        assertTrue(finished.completedAt != null)
        assertEquals(ScheduleGenJobStatus.IN_PROGRESS, captured.status)
        assertEquals(0, captured.completedConferences)
        assertTrue(captured.logs.isEmpty())
        assertEquals(listOf("generated", "other original"), storedGames())
        verify(exactly = 0) { rulesService.getConferenceRules("OTHER") }
    }

    @Test
    fun `cancellation rolls back transaction without recording a conference failure`() {
        stubGeneration(listOf("A", "B"))
        every { conferenceRepository.findAllByActiveTrueOrderByLabelAsc() } returns
            listOf(
                Conference().apply {
                    code = "TEST"
                    label = "Test"
                },
            )
        every { rulesService.getConferenceRules(any()) } returns null
        every { repository.getScheduleBySeason(11) } throws CancellationException("cancelled")
        val response = jobService.startAllConferenceGenerationAsync(12)
        runBlocking {
            withTimeout(5000) {
                while (jobService.getScheduleGenJobStatus(response.jobId)!!.status != ScheduleGenJobStatus.FAILED) delay(10)
            }
        }
        val job = jobService.getScheduleGenJobStatus(response.jobId)!!
        assertEquals(0, job.completedConferences)
        assertEquals(0, job.failedConferences)
        assertTrue(job.logs.isEmpty())
        assertTrue(job.completedAt != null)
        assertEquals(listOf("original"), storedGames())
    }
}
