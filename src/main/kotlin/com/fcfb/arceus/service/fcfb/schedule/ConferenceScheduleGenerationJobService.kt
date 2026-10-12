package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.dto.request.ConferenceScheduleRequest
import com.fcfb.arceus.dto.response.ScheduleGenJob
import com.fcfb.arceus.dto.response.ScheduleGenJobResponse
import com.fcfb.arceus.dto.response.ScheduleGenJobStatus
import com.fcfb.arceus.dto.response.ScheduleGenLog
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.exception.schedule.ScheduleNotFoundException
import com.fcfb.arceus.model.Conference
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PreDestroy

@Service
class ConferenceScheduleGenerationJobService(
    private val conferenceScheduleGenerationService: ConferenceScheduleGenerationService,
    private val teamService: TeamService,
    private val conferenceRepository: ConferenceRepository,
    private val conferenceRulesService: ConferenceRulesService,
) {
    private val workerJob = SupervisorJob()
    private val workerScope = CoroutineScope(Dispatchers.IO + workerJob)
    private val shutdownSignal = CompletableDeferred<Unit>()
    private val workerLock = Any()

    companion object {
        private val activeGenJobs = ConcurrentHashMap<String, ScheduleGenJob>()
    }

    fun startAllConferenceGenerationAsync(season: Int): ScheduleGenJobResponse {
        val jobId = UUID.randomUUID().toString()
        val now = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val skipConferences = setOf("FBS_INDEPENDENT", "FAKE_TEAM")
        val conferencesToGenerate = conferenceRepository.findAllByActiveTrueOrderByLabelAsc().filter { it.code !in skipConferences }
        val validConferences =
            conferencesToGenerate.filter { conf ->
                val teams = teamService.getTeamsInConference(conf.code)
                teams?.any { it.active } == true
            }

        val job =
            ScheduleGenJob(
                jobId = jobId,
                season = season,
                status = ScheduleGenJobStatus.PENDING,
                totalConferences = validConferences.size,
                startedAt = now,
            )

        Logger.info("=== STARTING ALL-CONFERENCE SCHEDULE GENERATION (async) ===")
        Logger.info("Job ID: $jobId, Season: $season, Conferences: ${validConferences.size}")

        launchWorker(job) {
            processAllConferenceGeneration(
                jobId,
                season,
                validConferences,
            )
        }

        return ScheduleGenJobResponse(
            jobId = jobId,
            message =
                "Started generating schedules for ${validConferences.size} conferences. " +
                    "Poll /conference-schedule-generation-job/status?jobId=$jobId for progress.",
        )
    }

    private suspend fun processAllConferenceGeneration(
        jobId: String,
        season: Int,
        conferences: List<Conference>,
    ) {
        updateJob(jobId) { it.copy(status = ScheduleGenJobStatus.IN_PROGRESS) }

        for ((index, conference) in conferences.withIndex()) {
            currentCoroutineContext().ensureActive()
            if (shutdownSignal.isCompleted) break
            val timestamp =
                ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            try {
                Logger.info("[${index + 1}/${conferences.size}] Generating schedule for ${conference.code}...")
                val teams = (teamService.getTeamsInConference(conference.code) ?: emptyList()).filter { it.active }
                val subdivision = teams.firstOrNull()?.subdivision ?: Subdivision.FCFB
                val rules = conferenceRulesService.getConferenceRules(conference.code)

                val request =
                    ConferenceScheduleRequest(
                        season = season,
                        conference = conference.code,
                        subdivision = subdivision,
                        numConferenceGames = rules?.numConferenceGames ?: 9,
                        protectedRivalries = rules?.protectedRivalries ?: emptyList(),
                        startWeek = 1,
                        divisions = rules?.divisions ?: emptyList(),
                    )

                val generated = conferenceScheduleGenerationService.generateConferenceScheduleForTeams(request, teams)
                val log =
                    ScheduleGenLog(
                        conference = conference.code,
                        status = "SUCCESS",
                        gamesGenerated = generated.size,
                        message = "Generated ${generated.size} games",
                        timestamp = timestamp,
                    )
                updateJob(jobId) {
                    it.copy(
                        completedConferences = it.completedConferences + 1,
                        totalGamesGenerated = it.totalGamesGenerated + generated.size,
                        logs = (it.logs + log).toMutableList(),
                    )
                }
                Logger.info("[${index + 1}/${conferences.size}] ${conference.code}: ${generated.size} games generated")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"

                val log =
                    ScheduleGenLog(
                        conference = conference.code,
                        status = "FAILED",
                        gamesGenerated = 0,
                        message = errorMsg,
                        timestamp = timestamp,
                    )
                updateJob(jobId) {
                    it.copy(failedConferences = it.failedConferences + 1, logs = (it.logs + log).toMutableList())
                }
                Logger.error("[${index + 1}/${conferences.size}] FAILED ${conference.code}: $errorMsg")
            }
        }

        val job = completeJob(jobId)

        Logger.info("=== ALL-CONFERENCE GENERATION COMPLETE ===")
        Logger.info(
            "Job: $jobId — Conferences: ${job.completedConferences}/${job.totalConferences} succeeded, " +
                "${job.failedConferences} failed, ${job.totalGamesGenerated} total games",
        )
    }

    private fun launchWorker(
        job: ScheduleGenJob,
        process: suspend () -> Unit,
    ) {
        synchronized(workerLock) {
            check(!shutdownSignal.isCompleted) { "Conference-generation workers are shutting down" }
            activeGenJobs[job.jobId] = job
            workerScope.launch {
                try {
                    process()
                } catch (e: CancellationException) {
                    completeJob(job.jobId)
                    throw e
                }
            }
        }
    }

    @PreDestroy
    internal fun stopWorkers() {
        synchronized(workerLock) {
            shutdownSignal.complete(Unit)
            workerJob.complete()
        }
        runBlocking { workerJob.join() }
    }

    private fun completeJob(jobId: String): ScheduleGenJob =
        updateJob(jobId) {
            it.copy(
                completedAt = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                status =
                    if (it.completedConferences + it.failedConferences < it.totalConferences) {
                        ScheduleGenJobStatus.FAILED
                    } else {
                        ScheduleGenJobStatus.COMPLETED
                    },
            )
        }

    private fun updateJob(
        jobId: String,
        update: (ScheduleGenJob) -> ScheduleGenJob,
    ): ScheduleGenJob = activeGenJobs.computeIfPresent(jobId) { _, job -> update(job) }!!

    fun getScheduleGenJobStatus(jobId: String): ScheduleGenJob =
        activeGenJobs[jobId]?.let { it.copy(logs = it.logs.toMutableList()) }
            ?: throw ScheduleNotFoundException("No schedule generation job found with id $jobId")
}
