package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.response.GameWeekJob
import com.fcfb.arceus.dto.response.GameWeekJobResponse
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.service.fcfb.game.StartGameWeekJobService
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(StartGameWeekJobTestConfiguration::class)
class StartGameWeekJobControllerHttpTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var jobs: StartGameWeekJobService

    @Autowired
    private lateinit var sessions: SessionService

    @Value("\${bot.service.key}")
    private lateinit var botKey: String

    @Value("\${website.service.key}")
    private lateinit var websiteKey: String

    private val base = "${ApiConstants.FULL_PATH}/start-game-week-job"

    @BeforeEach
    fun responses() {
        every { jobs.startWeekAsync(12, 15) } returns GameWeekJobResponse("job-123", "Started processing games")
        every { jobs.retryFailedGames("job-123") } returns GameWeekJobResponse("job-456", "Retrying games")
        every { jobs.getGameWeekJobStatus("job-123") } returns GameWeekJob("job-123", 12, 15)
        every { jobs.getAllGameWeekJobs() } returns listOf(GameWeekJob("job-123", 12, 15))
        UserRole.entries.forEach { role ->
            every { sessions.validateToken(role.name) } returns true
            every { sessions.isSessionBlacklisted(role.name) } returns false
            every { sessions.extractUserIdFromToken(role.name) } returns 1L
            every { sessions.extractRoleFromToken(role.name) } returns role
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "/status", "/jobs", "/retry"])
    fun `job routes preserve privileged access and JSON contracts`(route: String) {
        mvc.perform(request(route)).andExpect(status().isUnauthorized)
        mvc.perform(request(route).header("X-Service-Key", "wrong-key")).andExpect(status().isUnauthorized)
        mvc.perform(request(route).header("X-Service-Key", websiteKey)).andExpect(status().isForbidden)
        mvc.perform(request(route).header("Authorization", "Bearer USER")).andExpect(status().isForbidden)

        val expectedStatus = if (route == "" || route == "/retry") 202 else 200
        listOf("ADMIN", "CONFERENCE_COMMISSIONER").forEach { role ->
            mvc.perform(request(route).header("Authorization", "Bearer $role")).andExpect(status().`is`(expectedStatus))
        }
        mvc.perform(request(route).header("X-Service-Key", botKey))
            .andExpect(status().`is`(expectedStatus))
            .andExpect(jsonPath(if (route == "/jobs") "$[0].jobId" else "$.jobId").value(if (route == "/retry") "job-456" else "job-123"))
    }

    private fun request(route: String): MockHttpServletRequestBuilder =
        when (route) {
            "" -> post(base).param("season", "12").param("week", "15")
            "/retry" -> post("$base$route").param("jobId", "job-123")
            "/status" -> get("$base$route").param("jobId", "job-123")
            else -> get("$base$route")
        }
}
