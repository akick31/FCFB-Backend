package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.animation.FieldSources
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.GameLook
import com.fcfb.arceus.model.appearance.PlayoffField
import com.fcfb.arceus.model.appearance.StadiumDesign
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.appearance.GameLookRepository
import com.fcfb.arceus.service.fcfb.animation.FieldSourceService
import com.fcfb.arceus.service.fcfb.team.TeamService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

class GameLookServiceTest {
    private val stored = mutableMapOf<Int, GameLook>()
    private val repository = mockk<GameLookRepository>()
    private val fieldSourceService = mockk<FieldSourceService>()
    private val teamService = mockk<TeamService>()
    private val service = GameLookService(repository, fieldSourceService, teamService)
    private val home = Team().apply { name = "Wyoming" }

    init {
        every { repository.existsById(any()) } answers { stored.containsKey(firstArg()) }
        every { repository.findById(any()) } answers { Optional.ofNullable(stored[firstArg()]) }
        every { repository.save(any()) } answers { firstArg<GameLook>().also { stored[it.gameId] = it } }
        every { teamService.getTeamByName("Wyoming") } returns home
    }

    @Test
    fun `a captured bowl keeps its field and stadium exactly as they were`() {
        val bowl =
            game(GameType.BOWL).also { game ->
                val field =
                    BowlField().apply {
                        this.bowl = "Rose Bowl Game"
                        venue = "Rose Bowl"
                        wallText = "THE GRANDDADDY"
                    }
                every { fieldSourceService.liveSources(game, home) } returns
                    FieldSources(field, null, StadiumDesign().apply { stadiumBackdrop = "FOREST" })
            }

        service.capture(bowl)
        val replay = service.sourcesFor(bowl)!!

        assertEquals("THE GRANDDADDY", replay.bowlField!!.wallText)
        assertEquals("Rose Bowl", replay.bowlField!!.venue)
        assertEquals("FOREST", replay.stadium!!.stadiumBackdrop)
    }

    @Test
    fun `a playoff field comes back as a playoff field and a standard stadium stays standard`() {
        val playoff =
            game(GameType.PLAYOFFS).also { game ->
                every { fieldSourceService.liveSources(game, home) } returns
                    FieldSources(null, PlayoffField().apply { round = "Quarterfinal" }, null)
            }

        service.capture(playoff)
        val replay = service.sourcesFor(playoff)!!

        assertTrue(replay.postseasonField is PlayoffField)
        assertEquals("Quarterfinal", (replay.postseasonField as PlayoffField).round)
        assertNull(replay.stadium)
    }

    @Test
    fun `a game is captured once and only finished games use the snapshot`() {
        val finished = game(GameType.OUT_OF_CONFERENCE)
        every { fieldSourceService.liveSources(finished, home) } returns FieldSources(null, null, StadiumDesign())

        service.capture(finished)
        service.capture(finished)
        finished.gameStatus = GameStatus.IN_PROGRESS

        verify(exactly = 1) { repository.save(any()) }
        assertNull(service.sourcesFor(finished))
    }

    private fun game(type: GameType) =
        Game().apply {
            gameId = stored.size + 10
            homeTeam = "Wyoming"
            awayTeam = "Boise State"
            gameType = type
            gameStatus = GameStatus.FINAL
        }
}
