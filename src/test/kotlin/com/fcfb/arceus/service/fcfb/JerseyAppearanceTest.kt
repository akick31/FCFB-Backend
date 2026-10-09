package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.request.AppearancePreviewRequest
import com.fcfb.arceus.dto.request.TeamUniformRequest
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.model.TeamField
import com.fcfb.arceus.model.TeamUniformCurrent
import com.fcfb.arceus.model.TeamUniformHistory
import com.fcfb.arceus.repositories.TeamUniformCurrentRepository
import com.fcfb.arceus.repositories.TeamUniformHistoryRepository
import com.fcfb.arceus.service.fcfb.animation.FieldStyle
import com.fcfb.arceus.service.fcfb.animation.FieldTheme
import com.fcfb.arceus.service.fcfb.animation.FieldThemeResolver
import com.fcfb.arceus.service.fcfb.animation.Uniforms
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

class JerseyAppearanceTest {
    private val team =
        Team().apply {
            name = "Test"
            primaryColor = "#800000"
            secondaryColor = "#FFFF00"
        }
    private val current =
        TeamUniformCurrent().apply {
            team = "Test"
            helmetColor = "#800000"
            secondaryHelmetColor = "#FFFFFF"
        }
    private val currentRepository = mockk<TeamUniformCurrentRepository>()
    private val teamService = mockk<TeamService>()
    private val cache = AppearanceThumbnailCache()
    private val appearanceService =
        TeamAppearanceService(
            currentRepository,
            mockk(),
            teamService,
            mockk(),
            FieldAppearanceApplier(),
            mockk(),
            cache,
        )

    init {
        every { currentRepository.findById("Test") } returns Optional.of(current)
        every { currentRepository.save(any()) } answers { firstArg() }
        every { teamService.getTeamByName("Test") } returns team
    }

    @AfterEach
    fun clearAuthentication() = SecurityContextHolder.clearContext()

    @Test
    fun `saved jerseys retain independent home and away text in game snapshots`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("1", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        appearanceService.updateUniform(
            "Test",
            TeamUniformRequest(
                numberTopText = "HOME",
                awayNumberTopText = "AWAY",
            ),
        )
        val historyRepository = mockk<TeamUniformHistoryRepository>()
        val captured = slot<TeamUniformHistory>()
        every { historyRepository.findByTeamAndSeasonNumberAndWeek(any(), any(), any()) } returns null
        every { historyRepository.save(capture(captured)) } answers { firstArg() }
        TeamUniformService(historyRepository, currentRepository, teamService).snapshot(
            Game().apply {
                homeTeam = "Test"
                awayTeam = "Test"
                season = 1
                week = 1
            },
        )
        val (home, away) = Uniforms.forMatchup(team, team, captured.captured, captured.captured)
        assertEquals("HOME", home.numberTopText)
        assertEquals("AWAY", away.numberTopText)

        appearanceService.updateUniform(
            "Test",
            TeamUniformRequest(awayNumberTopText = ""),
        )
        TeamUniformService(historyRepository, currentRepository, teamService).snapshot(
            Game().apply {
                homeTeam = "Test"
                awayTeam = "Test"
                season = 1
                week = 2
            },
        )
        val cleared = Uniforms.forMatchup(team, team, captured.captured, captured.captured)
        assertNull(cleared.first.numberTopText)
        assertEquals("", cleared.second.numberTopText)
    }

    @Test
    fun `draft preview permits clearing stored text without saving`() {
        current.numberTopText = "STORED"
        val appearance = mockk<TeamAppearanceService>()
        every { appearance.getUniform("Test") } returns current
        every { appearance.getField("Test") } returns TeamField().apply { team = "Test" }
        val resolver = mockk<FieldThemeResolver>()
        val captured = slot<TeamUniformHistory>()
        every { resolver.resolve(any(), any(), any(), any(), capture(captured), any(), any(), any(), any()) } answers {
            FieldTheme(FieldStyle.HOME_FIELD, team, team, null, homeUniform = captured.captured)
        }
        val preview =
            AppearancePreviewService(
                teamService, appearance, mockk(), mockk(), resolver, mockk(), mockk(), mockk(), FieldAppearanceApplier(),
                PostseasonFieldUpdater(), cache,
            )
        preview.preview(AppearancePreviewRequest(team = "Test", view = "JERSEY"))
        assertEquals("STORED", captured.captured.numberTopText)
        preview.preview(
            AppearancePreviewRequest(
                team = "Test",
                view = "AWAY_JERSEY",
                uniform =
                    TeamUniformRequest(
                        awayNumberTopText = "",
                    ),
            ),
        )
        assertNull(captured.captured.numberTopText)
        assertEquals("", captured.captured.awayNumberTopText)
        assertEquals("STORED", current.numberTopText)
    }
}
