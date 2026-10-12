package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.system.Platform
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.exception.GlobalExceptionHandler
import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.service.fcfb.game.GameCreationService
import com.fcfb.arceus.service.fcfb.game.GameService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class GameControllerHttpTest {
    private val creation = mockk<GameCreationService>()
    private val gameService = mockk<GameService>(relaxed = true)
    private val mockMvc =
        MockMvcBuilders.standaloneSetup(GameController(gameService), GameCreationController(creation))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
            .build()
    private val path = "${ApiConstants.FULL_PATH}/game"

    @Test
    fun `unexpected service failures return a generic JSON error`() {
        every { gameService.getGamesByIds(listOf(1)) } throws IllegalStateException("SELECT secret FROM private_accounts")

        mockMvc.perform(post("$path/batch").contentType(MediaType.APPLICATION_JSON).content("[1]"))
            .andExpect(status().isInternalServerError)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().json("{\"error\":\"An unexpected error occurred\"}"))
    }

    @Test
    fun `batch retains its JSON array response`() {
        every { gameService.getGamesByIds(listOf(1, 2)) } returns emptyList()

        mockMvc.perform(post("$path/batch").contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
            .andExpect(status().isOk)
            .andExpect(content().json("[]"))

        verify { gameService.getGamesByIds(listOf(1, 2)) }
    }

    @Test
    fun `list retains its paginated response`() {
        every { gameService.getFilteredGames(any(), any(), any(), any(), any(), any(), any(), any()) } returns PageImpl(emptyList())

        mockMvc.perform(get(path))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isEmpty)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun `start game retains created status after coroutine dispatch`() {
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
        val started =
            mockMvc.perform(
                post("${ApiConstants.FULL_PATH}/game-creation").contentType(MediaType.APPLICATION_JSON).content(
                    """{"homePlatform":"DISCORD","awayPlatform":"DISCORD","subdivision":"FCFB",
                    "homeTeam":"Texas","awayTeam":"Oklahoma","gameType":"SCRIMMAGE"}""",
                ),
            ).andExpect(request().asyncStarted()).andReturn()

        mockMvc.perform(asyncDispatch(started))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.game_id").value(7))
    }

    @Test
    fun `ping updates retain empty no-content responses`() {
        mockMvc.perform(put("$path/close-game-pinged").param("gameId", "7"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
        mockMvc.perform(put("$path/upset-alert-pinged").param("gameId", "7"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        verify { gameService.markCloseGamePinged(7) }
        verify { gameService.markUpsetAlertPinged(7) }
    }

    @Test
    fun `service rejection retains forbidden status`() {
        every { gameService.markCloseGamePinged(7) } throws UserForbiddenException()

        mockMvc.perform(put("$path/close-game-pinged").param("gameId", "7"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("User is not permitted to modify this account"))
    }
}
