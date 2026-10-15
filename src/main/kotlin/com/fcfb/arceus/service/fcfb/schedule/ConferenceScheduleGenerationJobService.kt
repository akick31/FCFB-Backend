package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.dto.request.ConferenceScheduleRequest
import com.fcfb.arceus.dto.response.ScheduleGenJob
import com.fcfb.arceus.dto.response.ScheduleGenJobResponse
import com.fcfb.arceus.dto.response.ScheduleGenJobStatus
import com.fcfb.arceus.dto.response.ScheduleGenLog
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.exception.schedule.ScheduleNotFoundException
import com.fcfb.arceus.model.conference.Conference
import com.fcfb.arceus.repositories.conference.ConferenceRepository
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.job.AsyncJobTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.annotation.PreDestroy

@Service
class ConferenceScheduleGenerationJobService(
    private val conferenceScheduleGenerationService: ConferenceScheduleGenerationService,
    private val teamService: TeamService,
    private val conferenceRepository: ConferenceRepository,
    private val conferenceRulesService: ConferenceRulesService,
) {
    private val jobTracker = AsyncJobTracker<ScheduleGenJob>("Conference-generation workers are shutting down")

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

        Logger().info("=== STARTING ALL-CONFERENCE SCHEDULE GENERATION (async) ===")
        Logger().info("Job ID: $jobId, Season: $season, Conferences: ${validConferences.size}")

        jobTracker.launch(job, onCancelled = { completeJob(it) }) {
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
        jobTracker.update(jobId) { it.copy(status = ScheduleGenJobStatus.IN_PROGRESS) }

        for ((index, conference) in conferences.withIndex()) {
            currentCoroutineContext().ensureActive()
            if (jobTracker.isShuttingDown) break
            val timestamp =
                ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            try {
                Logger().info("[${index + 1}/${conferences.size}] Generating schedule for ${conference.code}...")
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
                jobTracker.update(jobId) {
                    it.copy(
                        completedConferences = it.completedConferences + 1,
                        totalGamesGenerated = it.totalGamesGenerated + generated.size,
                        logs = (it.logs + log).toMutableList(),
                    )
                }
                Logger().info("[${index + 1}/${conferences.size}] ${conference.code}: ${generated.size} games generated")
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
                jobTracker.update(jobId) {
                    it.copy(failedConferences = it.failedConferences + 1, logs = (it.logs + log).toMutableList())
                }
                Logger().error("[${index + 1}/${conferences.size}] FAILED ${conference.code}: $errorMsg")
            }
        }

        val job = completeJob(jobId)

        Logger().info("=== ALL-CONFERENCE GENERATION COMPLETE ===")
        Logger().info(
            "Job: $jobId — Conferences: ${job.completedConferences}/${job.totalConferences} succeeded, " +
                "${job.failedConferences} failed, ${job.totalGamesGenerated} total games",
        )
    }

    @PreDestroy
    internal fun stopWorkers() {
        jobTracker.stop()
    }

    private fun completeJob(jobId: String): ScheduleGenJob =
        jobTracker.update(jobId) {
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

    fun getScheduleGenJobStatus(jobId: String): ScheduleGenJob =
        jobTracker.get(jobId)?.let { it.copy(logs = it.logs.toMutableList()) }
            ?: throw ScheduleNotFoundException("No schedule generation job found with id $jobId")
}
