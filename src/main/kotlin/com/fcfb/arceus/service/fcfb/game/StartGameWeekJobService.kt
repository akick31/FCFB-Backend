package com.fcfb.arceus.service.fcfb.game

import com.fcfb.arceus.dto.request.StartRequest
import com.fcfb.arceus.dto.response.FailedGameDetail
import com.fcfb.arceus.dto.response.GameStartLog
import com.fcfb.arceus.dto.response.GameWeekJob
import com.fcfb.arceus.dto.response.GameWeekJobResponse
import com.fcfb.arceus.dto.response.GameWeekJobStatus
import com.fcfb.arceus.enums.ranking.PollType
import com.fcfb.arceus.enums.system.Platform.DISCORD
import com.fcfb.arceus.exception.game.GameWeekJobNotFoundException
import com.fcfb.arceus.exception.game.NoGameFoundException
import com.fcfb.arceus.exception.season.RankingsNotUploadedException
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.repositories.ranking.RankingRepository
import com.fcfb.arceus.service.fcfb.schedule.ScheduleService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.job.AsyncJobTracker
import kotlinx.coroutines.CancellationException
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.annotation.PreDestroy

@Service
class StartGameWeekJobService(
    private val gameCreationService: GameCreationService,
    private val scheduleService: ScheduleService,
    private val rankingRepository: RankingRepository,
) {
    private val jobTracker = AsyncJobTracker<GameWeekJob>("Week-job workers are shutting down")

    companion object {
        const val DELAY_BETWEEN_GAMES_MS = 3000L
        const val BATCH_SIZE = 25
        const val BATCH_COOLDOWN_MS = 60000L
    }

    fun startWeekAsync(
        season: Int,
        week: Int,
    ): GameWeekJobResponse {
        if (!rankingRepository.existsForWeek(season, week, PollType.COACHES_POLL.name).let { it > 0 }) {
            throw RankingsNotUploadedException(season, week)
        }

        val jobId = UUID.randomUUID().toString()
        val now = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val gamesToStart =
            scheduleService.getGamesToStartBySeasonAndWeek(season, week) ?: run {
                Logger().error("No games found for season $season week $week")
                throw NoGameFoundException()
            }

        val job =
            GameWeekJob(
                jobId = jobId,
                season = season,
                week = week,
                status = GameWeekJobStatus.PENDING,
                totalGames = gamesToStart.size,
                startedAt = now,
            )

        Logger().info("=== STARTING GAME WEEK (async) ===")
        Logger().info("Job ID: $jobId, Season: $season, Week: $week, Games: ${gamesToStart.size}")

        jobTracker.launch(job, onCancelled = { completeJob(it) }) {
            processGameWeek(jobId, week, gamesToStart)
        }

        return GameWeekJobResponse(
            jobId = jobId,
            message =
                "Started processing ${gamesToStart.size} games for Season $season, Week $week. " +
                    "Poll /start-game-week-job/status?jobId=$jobId for progress.",
        )
    }

    private suspend fun processGameWeek(
        jobId: String,
        week: Int,
        gamesToStart: List<Schedule>,
    ) {
        if (!jobTracker.contains(jobId)) return
        var job = jobTracker.update(jobId) { it.copy(status = GameWeekJobStatus.IN_PROGRESS) }

        var batchCount = 0

        for ((index, game) in gamesToStart.withIndex()) {
            if (jobTracker.isShuttingDown) break
            val timestamp = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            jobTracker.update(jobId) { it.copy(currentIndex = index + 1) }

            try {
                Logger().info("[${index + 1}/${gamesToStart.size}] Starting game: ${game.homeTeam} vs ${game.awayTeam}")
                val startedGame =
                    gameCreationService.startNormalGame(
                        StartRequest(
                            DISCORD,
                            DISCORD,
                            game.subdivision,
                            game.homeTeam,
                            game.awayTeam,
                            game.tvChannel,
                            game.gameType,
                            game.postseasonGameLogo,
                            game.postseasonGameName,
                            game.neutralSite,
                            game.venue,
                        ),
                        week,
                    )
                game.gameId = startedGame.gameId
                scheduleService.markGameAsStarted(game)
                batchCount++
                val startLog =
                    GameStartLog(
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        status = "SUCCESS",
                        message = "Game started (ID: ${startedGame.gameId})",
                        timestamp = timestamp,
                        index = index + 1,
                    )
                job =
                    jobTracker.update(jobId) { current ->
                        current.copy(
                            startedGames = current.startedGames + 1,
                            logs = (current.logs + startLog).toMutableList(),
                        )
                    }
                Logger().info(
                    "[${index + 1}/${gamesToStart.size}] Successfully started: " +
                        "${game.homeTeam} vs ${game.awayTeam} (Game ID: ${startedGame.gameId})",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                batchCount++
                val errorMsg = e.message ?: "Unknown error"
                val startLog =
                    GameStartLog(
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        status = "FAILED",
                        message = errorMsg,
                        timestamp = timestamp,
                        index = index + 1,
                    )
                val failedDetail =
                    FailedGameDetail(
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        subdivision = game.subdivision,
                        tvChannel = game.tvChannel,
                        gameType = game.gameType,
                        postseasonGameLogo = game.postseasonGameLogo,
                        postseasonGameName = game.postseasonGameName,
                        neutralSite = game.neutralSite,
                        venue = game.venue,
                        error = errorMsg,
                    )
                job =
                    jobTracker.update(jobId) { current ->
                        current.copy(
                            failedGames = current.failedGames + 1,
                            logs = (current.logs + startLog).toMutableList(),
                            failedGameDetails = (current.failedGameDetails + failedDetail).toMutableList(),
                        )
                    }
                Logger().error(
                    "[${index + 1}/${gamesToStart.size}] FAILED to start game: " +
                        "${game.homeTeam} vs ${game.awayTeam} — $errorMsg",
                )
            }

            if (batchCount >= BATCH_SIZE && index < gamesToStart.size - 1) {
                val batchNum = (index + 1) / BATCH_SIZE
                Logger().info("=== Batch $batchNum complete ($BATCH_SIZE games). Cooling down ${BATCH_COOLDOWN_MS / 1000}s ===")
                Logger().info("Progress: ${job.startedGames}/${job.totalGames} started, ${job.failedGames} failed")
                pauseBetweenGames(BATCH_COOLDOWN_MS)
                batchCount = 0
            } else if (index < gamesToStart.size - 1) {
                pauseBetweenGames(DELAY_BETWEEN_GAMES_MS)
            }
        }

        job = completeJob(jobId)

        Logger().info("=== GAME WEEK START COMPLETE ===")
        Logger().info("Job: $jobId — Total: ${job.totalGames}, Started: ${job.startedGames}, Failed: ${job.failedGames}")
    }

    @PreDestroy
    internal fun stopWorkers() {
        jobTracker.stop()
    }

    private suspend fun pauseBetweenGames(duration: Long) {
        jobTracker.awaitShutdownOrTimeout(duration)
    }

    private fun completeJob(jobId: String): GameWeekJob =
        jobTracker.update(jobId) {
            it.copy(
                completedAt = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                status =
                    if (it.failedGames > 0 || it.startedGames + it.failedGames < it.totalGames) {
                        GameWeekJobStatus.FAILED
                    } else {
                        GameWeekJobStatus.COMPLETED
                    },
            )
        }

    private fun snapshot(job: GameWeekJob): GameWeekJob =
        job.copy(logs = job.logs.toMutableList(), failedGameDetails = job.failedGameDetails.toMutableList())

    fun getGameWeekJobStatus(jobId: String): GameWeekJob =
        jobTracker.get(jobId)?.let(::snapshot) ?: throw GameWeekJobNotFoundException(jobId)

    fun getAllGameWeekJobs(): List<GameWeekJob> = jobTracker.all().map(::snapshot).sortedByDescending { it.startedAt }

    fun retryFailedGames(originalJobId: String): GameWeekJobResponse {
        val originalJob =
            jobTracker.get(originalJobId)
                ?: throw NoGameFoundException()

        val failedDetails = originalJob.failedGameDetails.toList()
        if (failedDetails.isEmpty()) {
            return GameWeekJobResponse(
                jobId = originalJobId,
                message = "No failed games to retry.",
            )
        }

        val newJobId = UUID.randomUUID().toString()
        val now = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val newJob =
            GameWeekJob(
                jobId = newJobId,
                season = originalJob.season,
                week = originalJob.week,
                status = GameWeekJobStatus.PENDING,
                totalGames = failedDetails.size,
                startedAt = now,
            )

        Logger().info("=== RETRYING FAILED GAMES ===")
        Logger().info("New Job: $newJobId, Original Job: $originalJobId, Retrying: ${failedDetails.size} games")

        jobTracker.launch(newJob, onCancelled = { completeJob(it) }) {
            retryFailedGamesProcess(newJobId, originalJob.week, failedDetails)
        }

        return GameWeekJobResponse(
            jobId = newJobId,
            message =
                "Retrying ${failedDetails.size} failed games from job $originalJobId. " +
                    "Poll /start-game-week-job/status?jobId=$newJobId for progress.",
        )
    }

    private suspend fun retryFailedGamesProcess(
        jobId: String,
        week: Int,
        failedGames: List<FailedGameDetail>,
    ) {
        if (!jobTracker.contains(jobId)) return
        var job = jobTracker.update(jobId) { it.copy(status = GameWeekJobStatus.IN_PROGRESS) }

        var batchCount = 0

        for ((index, failed) in failedGames.withIndex()) {
            if (jobTracker.isShuttingDown) break
            val timestamp = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            jobTracker.update(jobId) { it.copy(currentIndex = index + 1) }

            try {
                Logger().info("[Retry ${index + 1}/${failedGames.size}] Starting game: ${failed.homeTeam} vs ${failed.awayTeam}")
                val startedGame =
                    gameCreationService.startNormalGame(
                        StartRequest(
                            DISCORD,
                            DISCORD,
                            failed.subdivision,
                            failed.homeTeam,
                            failed.awayTeam,
                            failed.tvChannel,
                            failed.gameType,
                            failed.postseasonGameLogo,
                            failed.postseasonGameName,
                            failed.neutralSite,
                            failed.venue,
                        ),
                        week,
                    )
                scheduleService.markManuallyStartedGameAsStarted(startedGame)
                batchCount++
                val startLog =
                    GameStartLog(
                        homeTeam = failed.homeTeam,
                        awayTeam = failed.awayTeam,
                        status = "SUCCESS",
                        message = "Retry successful (ID: ${startedGame.gameId})",
                        timestamp = timestamp,
                        index = index + 1,
                    )
                job =
                    jobTracker.update(jobId) { current ->
                        current.copy(
                            startedGames = current.startedGames + 1,
                            logs = (current.logs + startLog).toMutableList(),
                        )
                    }
                Logger().info("[Retry ${index + 1}/${failedGames.size}] Success: ${failed.homeTeam} vs ${failed.awayTeam}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                batchCount++
                val errorMsg = e.message ?: "Unknown error"
                val startLog =
                    GameStartLog(
                        homeTeam = failed.homeTeam,
                        awayTeam = failed.awayTeam,
                        status = "FAILED",
                        message = "Retry failed: $errorMsg",
                        timestamp = timestamp,
                        index = index + 1,
                    )
                val failedDetail =
                    FailedGameDetail(
                        homeTeam = failed.homeTeam,
                        awayTeam = failed.awayTeam,
                        subdivision = failed.subdivision,
                        tvChannel = failed.tvChannel,
                        gameType = failed.gameType,
                        postseasonGameLogo = failed.postseasonGameLogo,
                        postseasonGameName = failed.postseasonGameName,
                        neutralSite = failed.neutralSite,
                        venue = failed.venue,
                        error = errorMsg,
                    )
                job =
                    jobTracker.update(jobId) { current ->
                        current.copy(
                            failedGames = current.failedGames + 1,
                            logs = (current.logs + startLog).toMutableList(),
                            failedGameDetails = (current.failedGameDetails + failedDetail).toMutableList(),
                        )
                    }
                Logger().error(
                    "[Retry ${index + 1}/${failedGames.size}] FAILED again: ${failed.homeTeam} vs ${failed.awayTeam} — $errorMsg",
                )
            }

            if (batchCount >= BATCH_SIZE && index < failedGames.size - 1) {
                Logger().info("=== Retry batch complete. Cooling down ${BATCH_COOLDOWN_MS / 1000}s ===")
                pauseBetweenGames(BATCH_COOLDOWN_MS)
                batchCount = 0
            } else if (index < failedGames.size - 1) {
                pauseBetweenGames(DELAY_BETWEEN_GAMES_MS)
            }
        }

        job = completeJob(jobId)

        Logger().info("=== RETRY COMPLETE ===")
        Logger().info("Job: $jobId — Retried: ${failedGames.size}, Started: ${job.startedGames}, Still Failed: ${job.failedGames}")
    }
}
