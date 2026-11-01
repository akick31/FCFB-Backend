package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.enums.animation.FieldStyle
import com.fcfb.arceus.model.animation.FieldTheme
import com.fcfb.arceus.model.appearance.TeamField
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.rendering.animation.field.FieldBackgroundPainter
import com.fcfb.arceus.service.fcfb.animation.FieldThemeService
import com.fcfb.arceus.service.fcfb.game.GameService
import com.fcfb.arceus.service.fcfb.team.TeamService
import com.fcfb.arceus.util.appearance.AppearanceThumbnailCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class GameFieldPreviewServiceTest {
    private val gameService = mockk<GameService>()
    private val teamService = mockk<TeamService>()
    private val fieldThemeService = mockk<FieldThemeService>()
    private val service = GameFieldPreviewService(gameService, teamService, fieldThemeService, AppearanceThumbnailCache())

    private val home = Team().apply { name = "Wyoming" }
    private val away = Team().apply { name = "Idaho" }

    private fun stubGame() {
        val game =
            Game().apply {
                homeTeam = "Wyoming"
                awayTeam = "Idaho"
            }
        every { gameService.getGameById(7) } returns game
        every { teamService.getTeamByName("Wyoming") } returns home
        every { teamService.getTeamByName("Idaho") } returns away
        every { fieldThemeService.resolve(any(), game, home, away) } returns
            FieldTheme(
                style = FieldStyle.HOME_FIELD,
                homeTeam = home,
                awayTeam = away,
                centerLogoUrl = null,
                homeField = TeamField(),
            )
    }

    @Test
    fun `the field renders at the painter size and reports its geometry`() {
        stubGame()

        val response = service.gameField(7)

        val image = ImageIO.read(ByteArrayInputStream(response.body!!))
        assertEquals(FieldBackgroundPainter.WIDTH, image.width)
        assertEquals(FieldBackgroundPainter.HEIGHT, image.height)
        assertEquals("1000", response.headers.getFirst(GameFieldPreviewService.FIELD_WIDTH_HEADER))
        assertEquals("80", response.headers.getFirst(GameFieldPreviewService.FIELD_MARGIN_HEADER))
    }

    @Test
    fun `a repeat request is served from the cache`() {
        stubGame()

        service.gameField(7)
        service.gameField(7)

        verify(exactly = 1) { teamService.getTeamByName("Idaho") }
    }
}
