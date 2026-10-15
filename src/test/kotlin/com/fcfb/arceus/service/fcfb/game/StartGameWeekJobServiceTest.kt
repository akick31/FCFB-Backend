package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.dto.response.GameWeekJob
import com.fcfb.arceus.dto.response.GameWeekJobStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.ranking.PollType
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.exception.game.GameWeekJobNotFoundException
import com.fcfb.arceus.exception.game.NoGameFoundException
import com.fcfb.arceus.exception.season.RankingsNotUploadedException
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.repositories.ranking.RankingRepository
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.util.UUID
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class StartGameWeekJobServiceTest {
    private val games = mockk<GameCreationService>()
    private val schedules = mockk<ScheduleService>(relaxed = true)
    private val rankings = mockk<RankingRepository>()
    private val service = StartGameWeekJobService(games, schedules, rankings)

    @AfterEach
    fun stopWorkers() {
        service.stopWorkers()
    }

    private fun shutdownSignalOf(service: StartGameWeekJobService): CompletableDeferred<*> {
        val tracker = service.javaClass.getDeclaredField("jobTracker").apply { isAccessible = true }.get(service)
        return tracker.javaClass.getDeclaredField("shutdownSignal").apply { isAccessible = true }
            .get(tracker) as CompletableDeferred<*>
    }

    private val schedule =
        Schedule().apply {
            season = 12
            week = 15
            homeTeam = "Home"
            awayTeam = "Away"
            subdivision = Subdivision.FBS
            gameType = GameType.BOWL
            postseasonGameLogo = "bowl-logo"
            postseasonGameName = "Bowl"
            neutralSite = true
            venue = "Neutral Stadium"
        }

    @Test
    fun `missing rankings reject before loading the schedule`() {
        every { rankings.existsForWeek(12, 15, PollType.COACHES_POLL.name) } returns 0

        assertFailsWith<RankingsNotUploadedException> { service.startWeekAsync(12, 15) }
        verify(exactly = 0) { schedules.getGamesToStartBySeasonAndWeek(any(), any()) }
    }

    @Test
    fun `missing schedule retains its existing exception`() {
        every { rankings.existsForWeek(12, 15, PollType.COACHES_POLL.name) } returns 1
        every { schedules.getGamesToStartBySeasonAndWeek(12, 15) } returns null

        assertFailsWith<NoGameFoundException> { service.startWeekAsync(12, 15) }
    }

    @Test
    fun `unknown status and retry retain distinct exceptions`() {
        val missing = UUID.randomUUID().toString()
        assertFailsWith<GameWeekJobNotFoundException> { service.getGameWeekJobStatus(missing) }
        assertFailsWith<NoGameFoundException> { service.retryFailedGames(missing) }
    }

    @Test
    fun `starting a schedule preserves metadata and marks that schedule`() =
        runBlocking {
            readySchedule()
            val request = slot<StartRequest>()
            val started = Game().apply { gameId = 42 }
            coEvery { games.startNormalGame(capture(request), 15) } returns started

            val response = service.startWeekAsync(12, 15)
            val job = awaitFinished(response.jobId)

            assertEquals(GameWeekJobStatus.COMPLETED, job.status)
            assertEquals(1, job.totalGames)
            assertEquals(1, job.startedGames)
            assertEquals(0, job.failedGames)
            assertEquals(1, job.currentIndex)
            assertEquals(42, schedule.gameId)
            assertEquals("Game started (ID: 42)", job.logs.single().message)
            assertEquals("Bowl", request.captured.postseasonGameName)
            assertEquals("bowl-logo", request.captured.postseasonGameLogo)
            assertEquals(true, request.captured.neutralSite)
            assertEquals("Neutral Stadium", request.captured.venue)
            assertNotNull(job.completedAt)
            verify(exactly = 1) { schedules.markGameAsStarted(schedule) }
            verify(exactly = 0) { schedules.markManuallyStartedGameAsStarted(any()) }
            coVerify(exactly = 1) { games.startNormalGame(any(), 15) }

            val retry = service.retryFailedGames(response.jobId)
            assertEquals(response.jobId, retry.jobId)
            assertEquals("No failed games to retry.", retry.message)
        }

    @Test
    fun `a failed game is retried with the original metadata and separate job`() =
        runBlocking {
            readySchedule()
            coEvery { games.startNormalGame(any(), 15) } throws IllegalStateException("Bot unavailable")
            val original = awaitFinished(service.startWeekAsync(12, 15).jobId)
            assertEquals(GameWeekJobStatus.FAILED, original.status)
            assertEquals(1, original.failedGames)
            assertEquals("Bot unavailable", original.failedGameDetails.single().error)

            val request = slot<StartRequest>()
            val started = Game().apply { gameId = 43 }
            coEvery { games.startNormalGame(capture(request), 15) } returns started
            val retried = awaitFinished(service.retryFailedGames(original.jobId).jobId)

            assertEquals(GameWeekJobStatus.COMPLETED, retried.status)
            assertEquals(1, retried.startedGames)
            assertEquals(0, retried.failedGames)
            assertEquals("Retry successful (ID: 43)", retried.logs.single().message)
            assertEquals("Bowl", request.captured.postseasonGameName)
            assertEquals("Neutral Stadium", request.captured.venue)
            assertEquals(GameWeekJobStatus.FAILED, original.status)
            verify(exactly = 1) { schedules.markManuallyStartedGameAsStarted(started) }
            verify(exactly = 0) { schedules.markGameAsStarted(any()) }
        }

    @Test
    fun `a failed retry preserves the error and retryable details`() =
        runBlocking {
            readySchedule()
            coEvery { games.startNormalGame(any(), 15) } throws IllegalStateException("Bot unavailable")
            val original = awaitFinished(service.startWeekAsync(12, 15).jobId)
            val retried = awaitFinished(service.retryFailedGames(original.jobId).jobId)

            assertEquals(GameWeekJobStatus.FAILED, retried.status)
            assertEquals(0, retried.startedGames)
            assertEquals(1, retried.failedGames)
            assertEquals("Retry failed: Bot unavailable", retried.logs.single().message)
            assertEquals(original.failedGameDetails, retried.failedGameDetails)
        }

    private fun readySchedule() {
        every { rankings.existsForWeek(12, 15, PollType.COACHES_POLL.name) } returns 1
        every { schedules.getGamesToStartBySeasonAndWeek(12, 15) } returns listOf(schedule)
    }

    @Test
    fun `status response is detached from subsequent worker updates`() =
        runBlocking {
            readySchedule()
            val entered = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            coEvery { games.startNormalGame(any(), 15) } coAnswers {
                entered.complete(Unit)
                finish.await()
                Game().apply { gameId = 44 }
            }
            val response = service.startWeekAsync(12, 15)
            val captured =
                try {
                    withTimeout(5000) { entered.await() }
                    service.getGameWeekJobStatus(response.jobId).also {
                        assertEquals(GameWeekJobStatus.IN_PROGRESS, it.status)
                        assertEquals(0, it.startedGames)
                        assertNotSame(it, service.getAllGameWeekJobs().single { job -> job.jobId == response.jobId })
                    }
                } finally {
                    finish.complete(Unit)
                }
            val completed = awaitFinished(response.jobId)
            assertNotSame(captured, completed)
            assertNotSame(captured.logs, completed.logs)
            assertEquals(GameWeekJobStatus.IN_PROGRESS, captured.status)
            assertEquals(0, captured.startedGames)
            assertEquals(0, captured.logs.size)
            assertEquals(GameWeekJobStatus.COMPLETED, completed.status)
            assertEquals(1, completed.startedGames)
            assertEquals(1, completed.logs.size)
        }

    @Test
    fun `mutating returned status and jobs list cannot alter registry or retry inputs`() =
        runBlocking {
            readySchedule()
            coEvery { games.startNormalGame(any(), 15) } throws IllegalStateException("Bot unavailable")
            val response = service.startWeekAsync(12, 15)
            val original = awaitFinished(response.jobId)
            val listed = service.getAllGameWeekJobs().single { it.jobId == response.jobId }
            original.startedGames = 99
            original.failedGames = 0
            original.status = GameWeekJobStatus.COMPLETED
            original.logs.clear()
            original.failedGameDetails.clear()
            listed.logs.clear()
            listed.failedGameDetails.clear()
            val fresh = service.getGameWeekJobStatus(response.jobId)
            assertEquals(GameWeekJobStatus.FAILED, fresh.status)
            assertEquals(0, fresh.startedGames)
            assertEquals(1, fresh.failedGames)
            assertEquals(1, fresh.logs.size)
            assertEquals(1, fresh.failedGameDetails.size)
            coEvery { games.startNormalGame(any(), 15) } returns Game().apply { gameId = 45 }
            val retried = awaitFinished(service.retryFailedGames(response.jobId).jobId)
            assertEquals(1, retried.totalGames)
            assertEquals(1, retried.startedGames)
            assertEquals(GameWeekJobStatus.COMPLETED, retried.status)
        }

    private suspend fun awaitFinished(jobId: String): GameWeekJob =
        withTimeout(5000) {
            var job = service.getGameWeekJobStatus(jobId)
            assertEquals(job.startedGames + job.failedGames, job.logs.size)
            assertEquals(job.failedGames, job.failedGameDetails.size)
            while (job.status == GameWeekJobStatus.PENDING || job.status == GameWeekJobStatus.IN_PROGRESS) {
                delay(10)
                job = service.getGameWeekJobStatus(jobId)
                assertEquals(job.startedGames + job.failedGames, job.logs.size)
                assertEquals(job.failedGames, job.failedGameDetails.size)
            }
            job
        }

    @Test
    fun `cancelled worker terminates without inventing retryable failures`() =
        runBlocking {
            readySchedule()
            coEvery { games.startNormalGame(any(), 15) } coAnswers {
                currentCoroutineContext().cancel(CancellationException("Worker cancelled"))
                throw CancellationException("Worker cancelled")
            }
            val cancelled = awaitFinished(service.startWeekAsync(12, 15).jobId)
            assertEquals(GameWeekJobStatus.FAILED, cancelled.status)
            assertEquals(0, cancelled.failedGames)
            assertEquals(0, cancelled.failedGameDetails.size)
            assertEquals(0, cancelled.logs.size)
            assertNotNull(cancelled.completedAt)
            verify(exactly = 0) { schedules.markGameAsStarted(any()) }

            val retry = service.retryFailedGames(cancelled.jobId)
            assertEquals(cancelled.jobId, retry.jobId)
            assertEquals("No failed games to retry.", retry.message)
        }

    @Test
    fun `cancelled multi-game worker publishes a terminal incomplete job`() =
        runBlocking {
            readySchedule()
            every { schedules.getGamesToStartBySeasonAndWeek(12, 15) } returns listOf(schedule, schedule)
            val worker = CompletableDeferred<Job>()
            coEvery { games.startNormalGame(any(), 15) } coAnswers {
                val job = currentCoroutineContext()[Job]!!
                worker.complete(job)
                job.cancel(CancellationException("Worker cancelled"))
                throw CancellationException("Worker cancelled")
            }
            val response = service.startWeekAsync(12, 15)
            withTimeout(5000) { worker.await().join() }
            val abandoned = service.getGameWeekJobStatus(response.jobId)
            assertEquals(GameWeekJobStatus.FAILED, abandoned.status)
            assertEquals(2, abandoned.totalGames)
            assertEquals(1, abandoned.currentIndex)
            assertEquals(0, abandoned.failedGames)
            assertEquals(0, abandoned.failedGameDetails.size)
            assertNotNull(abandoned.completedAt)
            coVerify(exactly = 1) { games.startNormalGame(any(), 15) }
        }

    @Test
    fun `shutdown waits for current game and schedule update then skips remaining games`() =
        runBlocking {
            readySchedule()
            every { schedules.getGamesToStartBySeasonAndWeek(12, 15) } returns listOf(schedule, schedule)
            val entered = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            coEvery { games.startNormalGame(any(), 15) } coAnswers {
                entered.complete(Unit)
                finish.await()
                Game().apply { gameId = 46 }
            }
            val response = service.startWeekAsync(12, 15)
            withTimeout(5000) { entered.await() }
            val context =
                AnnotationConfigApplicationContext().apply {
                    registerBean("weekJobs", StartGameWeekJobService::class.java, Supplier { service })
                    refresh()
                }
            val stop = async(Dispatchers.IO) { context.close() }
            try {
                val signal = shutdownSignalOf(service)
                withTimeout(5000) { signal.await() }
                assertFalse(stop.isCompleted)
                assertFailsWith<IllegalStateException> { service.startWeekAsync(12, 15) }
            } finally {
                finish.complete(Unit)
            }
            withTimeout(5000) { stop.await() }
            val stopped = service.getGameWeekJobStatus(response.jobId)
            assertEquals(GameWeekJobStatus.FAILED, stopped.status)
            assertEquals(1, stopped.startedGames)
            assertEquals(0, stopped.failedGames)
            assertEquals(0, stopped.failedGameDetails.size)
            assertEquals(1, stopped.currentIndex)
            assertNotNull(stopped.completedAt)
            verify(exactly = 1) { schedules.markGameAsStarted(schedule) }
        }

    @Test
    fun `shutdown wakes pacing wait without starting the next game`() =
        runBlocking {
            readySchedule()
            every { schedules.getGamesToStartBySeasonAndWeek(12, 15) } returns listOf(schedule, schedule)
            coEvery { games.startNormalGame(any(), 15) } returns Game().apply { gameId = 47 }
            val response = service.startWeekAsync(12, 15)
            withTimeout(1000) {
                while (service.getGameWeekJobStatus(response.jobId).startedGames != 1) delay(1)
            }
            withTimeout(1000) { async(Dispatchers.IO) { service.stopWorkers() }.await() }
            val stopped = service.getGameWeekJobStatus(response.jobId)
            assertEquals(GameWeekJobStatus.FAILED, stopped.status)
            assertEquals(1, stopped.startedGames)
            assertEquals(0, stopped.failedGames)
            coVerify(exactly = 1) { games.startNormalGame(any(), 15) }
        }

    @Test
    fun `shutdown drains retry schedule update and rejects another retry`() =
        runBlocking {
            readySchedule()
            coEvery { games.startNormalGame(any(), 15) } throws IllegalStateException("Bot unavailable")
            val original = awaitFinished(service.startWeekAsync(12, 15).jobId)
            val entered = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            val started = Game().apply { gameId = 48 }
            coEvery { games.startNormalGame(any(), 15) } coAnswers {
                entered.complete(Unit)
                finish.await()
                started
            }
            val response = service.retryFailedGames(original.jobId)
            try {
                withTimeout(5000) { entered.await() }
                val stop = async(Dispatchers.IO) { service.stopWorkers() }
                val signal = shutdownSignalOf(service)
                withTimeout(5000) { signal.await() }
                assertFalse(stop.isCompleted)
                assertFailsWith<IllegalStateException> { service.retryFailedGames(original.jobId) }
                finish.complete(Unit)
                withTimeout(5000) { stop.await() }
                val completed = service.getGameWeekJobStatus(response.jobId)
                assertEquals(GameWeekJobStatus.COMPLETED, completed.status)
                assertEquals(1, completed.startedGames)
                assertEquals(0, completed.failedGames)
                verify(exactly = 1) { schedules.markManuallyStartedGameAsStarted(started) }
                verify(exactly = 0) { schedules.markGameAsStarted(any()) }
            } finally {
                finish.complete(Unit)
            }
        }
}
