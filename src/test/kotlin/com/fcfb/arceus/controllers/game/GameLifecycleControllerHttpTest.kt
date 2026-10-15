package com.fcfb.arceus.controllers.game

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.enums.system.Platform
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.service.fcfb.game.GameCreationService
import com.fcfb.arceus.service.fcfb.game.GameFinalizationService
import io.mockk.coEvery
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GameLifecycleTestConfiguration::class, StartGameWeekJobTestConfiguration::class)
class GameLifecycleControllerHttpTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var creation: GameCreationService

    @Autowired
    private lateinit var finalization: GameFinalizationService

    @Autowired
    private lateinit var sessions: SessionService

    @Value("\${bot.service.key}")
    private lateinit var botKey: String

    @Value("\${website.service.key}")
    private lateinit var websiteKey: String

    @BeforeEach
    fun responses() {
        val game =
            Game().apply {
                gameId = 7
                homeTeam = "Texas"
                awayTeam = "Oklahoma"
                homeOffensivePlaybook = OffensivePlaybook.AIR_RAID
                awayOffensivePlaybook = OffensivePlaybook.AIR_RAID
                homeDefensivePlaybook = DefensivePlaybook.FOUR_THREE
                awayDefensivePlaybook = DefensivePlaybook.FOUR_THREE
                homePlatform = Platform.DISCORD
                awayPlatform = Platform.DISCORD
            }
        coEvery { creation.startSingleGame(any(), null) } returns game
        coEvery { creation.startOvertimeGame(any()) } returns game
        coEvery { creation.restartGame("123") } returns game
        every { finalization.endSingleGameByGameId(7) } returns game
        every { finalization.endSingleGameByChannelId(123uL) } returns game
        every { finalization.endAllGames() } returns listOf(game)
        UserRole.entries.forEach { role ->
            every { sessions.validateToken(role.name) } returns true
            every { sessions.isSessionBlacklisted(role.name) } returns false
            every { sessions.extractUserIdFromToken(role.name) } returns 1L
            every { sessions.extractRoleFromToken(role.name) } returns role
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["create", "overtime", "restart", "end-id", "end-channel", "end-all"])
    fun `migrated endpoints retain privileged access status and Game JSON`(operation: String) {
        mvc.perform(request(operation)).andExpect(status().isUnauthorized)
        mvc.perform(request(operation).header("X-Service-Key", "wrong-key")).andExpect(status().isUnauthorized)
        mvc.perform(request(operation).header("X-Service-Key", websiteKey)).andExpect(status().isForbidden)
        mvc.perform(request(operation).header("Authorization", "Bearer USER")).andExpect(status().isForbidden)
        for (role in listOf("ADMIN", "CONFERENCE_COMMISSIONER")) {
            val initial = mvc.perform(request(operation).header("Authorization", "Bearer $role"))
            val sent = initial.andReturn()
            val response =
                if (sent.request.isAsyncStarted) {
                    mvc.perform(asyncDispatch(sent))
                } else {
                    initial
                }
            response.andExpect(status().`is`(if (operation in listOf("create", "overtime")) 201 else 200))
                .andExpect(jsonPath(if (operation == "end-all") "$[0].game_id" else "$.game_id").value(7))
        }
        val initial = mvc.perform(request(operation).header("X-Service-Key", botKey))
        val sent = initial.andReturn()
        val response =
            if (sent.request.isAsyncStarted) {
                mvc.perform(asyncDispatch(sent))
            } else {
                initial
            }
        response.andExpect(status().`is`(if (operation in listOf("create", "overtime")) 201 else 200))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "/overtime", "/restart", "/end", "/end-all"])
    fun `old lifecycle routes have no aliases`(suffix: String) {
        mvc.perform(post("${ApiConstants.FULL_PATH}/game$suffix").header("X-Service-Key", botKey))
            .andExpect(if (suffix.isEmpty()) status().isInternalServerError else status().isNotFound)
    }

    private fun request(operation: String): MockHttpServletRequestBuilder =
        when (operation) {
            "create", "overtime" ->
                post("${ApiConstants.FULL_PATH}/game-creation${if (operation == "overtime") "/overtime" else ""}")
                    .contentType(MediaType.APPLICATION_JSON).content(
                        """{"homePlatform":"DISCORD","awayPlatform":"DISCORD","subdivision":"FCFB",
                "homeTeam":"Texas","awayTeam":"Oklahoma","gameType":"SCRIMMAGE"}""",
                    )
            "restart" -> post("${ApiConstants.FULL_PATH}/game-creation/restart").param("channelId", "123")
            "end-id" -> post("${ApiConstants.FULL_PATH}/game-finalization").param("gameId", "7")
            "end-channel" -> post("${ApiConstants.FULL_PATH}/game-finalization").param("channelId", "123")
            else -> post("${ApiConstants.FULL_PATH}/game-finalization/all")
        }
}
