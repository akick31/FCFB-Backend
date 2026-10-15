package com.fcfb.arceus.controllers.game

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.system.Platform
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.service.fcfb.game.GameCoinTossService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GameCoinTossTestConfiguration::class, StartGameWeekJobTestConfiguration::class)
class GameCoinTossControllerHttpTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var coinToss: GameCoinTossService

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
                coinTossWinner = TeamSide.HOME
                coinTossChoice = CoinTossChoice.RECEIVE
                overtimeCoinTossWinner = TeamSide.AWAY
                overtimeCoinTossChoice = OvertimeCoinTossChoice.DEFENSE
                gameStatus = GameStatus.OVERTIME
            }
        every { coinToss.runCoinToss(7, CoinTossCall.HEADS) } returns game
        every { coinToss.makeCoinTossChoice(7, CoinTossChoice.RECEIVE) } returns game
        every { coinToss.makeOvertimeCoinTossChoice(7, OvertimeCoinTossChoice.DEFENSE) } returns game
        UserRole.entries.forEach { role ->
            every { sessions.validateToken(role.name) } returns true
            every { sessions.isSessionBlacklisted(role.name) } returns false
            every { sessions.extractUserIdFromToken(role.name) } returns 1L
            every { sessions.extractRoleFromToken(role.name) } returns role
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["call", "choice", "overtime-choice"])
    fun `new routes retain privileged access and snake case game responses`(route: String) {
        mvc.perform(request(route)).andExpect(status().isUnauthorized)
        mvc.perform(request(route).header("X-Service-Key", "wrong-key")).andExpect(status().isUnauthorized)
        mvc.perform(request(route).header("X-Service-Key", websiteKey)).andExpect(status().isForbidden)
        mvc.perform(request(route).header("Authorization", "Bearer USER")).andExpect(status().isForbidden)
        listOf("ADMIN", "CONFERENCE_COMMISSIONER").forEach { role ->
            mvc.perform(request(route).header("Authorization", "Bearer $role")).andExpect(status().isOk)
        }
        mvc.perform(request(route).header("X-Service-Key", botKey))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.game_id").value(7))
            .andExpect(jsonPath("$.coin_toss_winner").value("HOME"))
            .andExpect(jsonPath("$.coin_toss_choice").value("RECEIVE"))
            .andExpect(jsonPath("$.overtime_coin_toss_winner").value("AWAY"))
            .andExpect(jsonPath("$.overtime_coin_toss_choice").value("DEFENSE"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["call", "choice", "overtime-choice"])
    fun `new routes preserve enum binding failure mapping`(route: String) {
        val parameter = if (route == "call") "coinTossCall" else "coinTossChoice"
        mvc.perform(
            put("${ApiConstants.FULL_PATH}/game-coin-toss/$route").param("gameId", "7")
                .param(parameter, "invalid").header("X-Service-Key", botKey),
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("An unexpected error occurred"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["coin-toss", "coin-toss-choice", "overtime-coin-toss-choice"])
    fun `legacy routes have no aliases`(route: String) {
        mvc.perform(put("${ApiConstants.FULL_PATH}/game/$route").header("X-Service-Key", botKey))
            .andExpect(status().isNotFound)
    }

    private fun request(route: String): MockHttpServletRequestBuilder =
        put("${ApiConstants.FULL_PATH}/game-coin-toss/$route").param("gameId", "7").apply {
            when (route) {
                "call" -> param("coinTossCall", "HEADS")
                "choice" -> param("coinTossChoice", "RECEIVE")
                else -> param("coinTossChoice", "DEFENSE")
            }
        }
}
