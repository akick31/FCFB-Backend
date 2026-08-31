package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.response.ScheduleValidationResult
import com.fcfb.arceus.dto.response.TeamScheduleGap
import com.fcfb.arceus.model.Season
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.repositories.SeasonRepository
import com.fcfb.arceus.service.fcfb.OffseasonService
import com.fcfb.arceus.service.fcfb.SeasonService
import com.fcfb.arceus.service.fcfb.TeamSeasonConferenceService
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.service.fcfb.UserService
import com.fcfb.arceus.service.fcfb.schedule.ScheduleValidationService
import com.fcfb.arceus.util.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SeasonControllerTest {
    private lateinit var mockMvc: MockMvc
    private val seasonRepository: SeasonRepository = mockk()
    private val offseasonService: OffseasonService = mockk()
    private val teamService: TeamService = mockk()
    private val userService: UserService = mockk()
    private val scheduleRepository: ScheduleRepository = mockk()
    private val teamSeasonConferenceService: TeamSeasonConferenceService = mockk()
    private val scheduleValidationService: ScheduleValidationService = mockk()
    private lateinit var seasonService: SeasonService
    private lateinit var seasonController: SeasonController

    @BeforeEach
    fun setup() {
        seasonService =
            SeasonService(
                seasonRepository,
                offseasonService,
                teamService,
                userService,
                scheduleRepository,
                teamSeasonConferenceService,
                scheduleValidationService,
            )
        seasonController = SeasonController(seasonService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(seasonController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `should start a locked, validated pending season successfully`() {
        val pendingSeason =
            Season(
                seasonNumber = 12,
                startDate = null,
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = false,
                scheduleLocked = true,
            )

        every { seasonRepository.getPendingSeason() } returns pendingSeason
        every { scheduleValidationService.validateSchedule(12) } returns
            ScheduleValidationResult(valid = true, incompleteTeams = emptyList())
        every { teamService.resetWinsAndLosses() } returns Unit
        every { teamService.resetRankings() } returns Unit
        every { userService.resetAllDelayOfGameInstances() } returns Unit
        every { seasonRepository.save(any()) } returns pendingSeason
        every { offseasonService.endOffseason(any()) } returns Unit
        every { teamSeasonConferenceService.snapshotSeason(any()) } returns Unit

        mockMvc.perform(post("/api/v1/arceus/season").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seasonNumber").value(12))
            .andExpect(jsonPath("$.currentSeason").value(true))
            .andExpect(jsonPath("$.startDate").isNotEmpty)

        verify { seasonRepository.save(pendingSeason) }
    }

    @Test
    fun `should reject starting a season when there is no pending season`() {
        every { seasonRepository.getPendingSeason() } returns null

        mockMvc.perform(post("/api/v1/arceus/season").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("No pending season to start. Create a season for scheduling first."))
    }

    @Test
    fun `should reject starting a season when the schedule is not locked`() {
        val pendingSeason =
            Season(
                seasonNumber = 12,
                startDate = null,
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = false,
                scheduleLocked = false,
            )
        every { seasonRepository.getPendingSeason() } returns pendingSeason

        mockMvc.perform(post("/api/v1/arceus/season").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Cannot start Season 12: the schedule must be locked before starting."))
    }

    @Test
    fun `should reject starting a season when the schedule has gaps`() {
        val pendingSeason =
            Season(
                seasonNumber = 12,
                startDate = null,
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = false,
                scheduleLocked = true,
            )
        every { seasonRepository.getPendingSeason() } returns pendingSeason
        every { scheduleValidationService.validateSchedule(12) } returns
            ScheduleValidationResult(valid = false, incompleteTeams = listOf(TeamScheduleGap("Duke", listOf(3, 4))))

        mockMvc.perform(post("/api/v1/arceus/season").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Cannot start Season 12: schedule is incomplete. Duke (missing weeks 3, 4)"))
    }

    @Test
    fun `should get current season successfully`() {
        val season =
            Season(
                seasonNumber = 1,
                startDate = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = true,
            )
        every { seasonRepository.getCurrentSeason() } returns season

        mockMvc.perform(get("/api/v1/arceus/season/current").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seasonNumber").value(season.seasonNumber))
            .andExpect(jsonPath("$.startDate").value(season.startDate))
            .andExpect(jsonPath("$.endDate").isEmpty)
            .andExpect(jsonPath("$.nationalChampionshipWinningTeam").isEmpty)
            .andExpect(jsonPath("$.nationalChampionshipLosingTeam").isEmpty)
            .andExpect(jsonPath("$.nationalChampionshipWinningCoach").isEmpty)
            .andExpect(jsonPath("$.nationalChampionshipLosingCoach").isEmpty)
            .andExpect(jsonPath("$.currentWeek").value(season.currentWeek))
            .andExpect(jsonPath("$.currentSeason").value(season.currentSeason))
    }

    @Test
    fun `should get upcoming season successfully`() {
        val upcomingSeason =
            Season(
                seasonNumber = 12,
                startDate = null,
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = false,
            )
        every { seasonRepository.getPendingSeason() } returns upcomingSeason

        mockMvc.perform(get("/api/v1/arceus/season/upcoming").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seasonNumber").value(upcomingSeason.seasonNumber))
            .andExpect(jsonPath("$.startDate").isEmpty)
    }

    @Test
    fun `should get latest completed season successfully`() {
        val latestCompletedSeason =
            Season(
                seasonNumber = 11,
                startDate = "08/01/2025 00:29:12",
                endDate = "07/08/2026 23:56:43",
                nationalChampionshipWinningTeam = "Wyoming",
                nationalChampionshipLosingTeam = "Duke",
                nationalChampionshipWinningCoach = "flying_porygon",
                nationalChampionshipLosingCoach = "Dan",
                currentWeek = 20,
                currentSeason = false,
            )
        every { seasonRepository.getMostRecentlyCompletedSeason() } returns latestCompletedSeason

        mockMvc.perform(get("/api/v1/arceus/season/latest-completed").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.seasonNumber").value(latestCompletedSeason.seasonNumber))
            .andExpect(jsonPath("$.endDate").value(latestCompletedSeason.endDate))
    }

    @Test
    fun `should get current week successfully`() {
        val currentWeek = 5
        val season =
            Season(
                seasonNumber = 1,
                startDate = "01/01/2023 00:00:00",
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = currentWeek,
                currentSeason = true,
            )
        every { seasonRepository.getCurrentSeason() } returns season

        mockMvc.perform(get("/api/v1/arceus/season/current/week").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").value(currentWeek))
    }

    @Test
    fun `should handle error when starting season`() {
        val pendingSeason =
            Season(
                seasonNumber = 12,
                startDate = null,
                endDate = null,
                nationalChampionshipWinningTeam = null,
                nationalChampionshipLosingTeam = null,
                nationalChampionshipWinningCoach = null,
                nationalChampionshipLosingCoach = null,
                currentWeek = 1,
                currentSeason = false,
                scheduleLocked = true,
            )
        every { seasonRepository.getPendingSeason() } returns pendingSeason
        every { scheduleValidationService.validateSchedule(12) } returns
            ScheduleValidationResult(valid = true, incompleteTeams = emptyList())
        every { teamService.resetWinsAndLosses() } throws RuntimeException("Failed to start season")

        mockMvc.perform(post("/api/v1/arceus/season").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("Failed to start season"))
    }

    @Test
    fun `returns 404 when there is no current season`() {
        every { seasonRepository.getCurrentSeason() } returns null

        mockMvc.perform(get("/api/v1/arceus/season/current").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Current season not found"))
    }

    @Test
    fun `returns 404 when there is no current week`() {
        every { seasonRepository.getCurrentSeason() } returns null

        mockMvc.perform(get("/api/v1/arceus/season/current/week").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Current week not found"))
    }
}
