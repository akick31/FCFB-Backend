package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.animation.StadiumBackdrop
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.team.TeamSide
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.Venue
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.play.Play
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.appearance.BowlFieldRepository
import com.fcfb.arceus.repositories.appearance.ConferenceChampionshipFieldRepository
import com.fcfb.arceus.repositories.appearance.GameFieldRepository
import com.fcfb.arceus.repositories.appearance.PlayoffFieldRepository
import com.fcfb.arceus.repositories.appearance.TeamFieldRepository
import com.fcfb.arceus.repositories.appearance.VenueRepository
import com.fcfb.arceus.repositories.conference.ConferenceRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Optional

class FieldThemeServiceStadiumTest {
    private val venueRepository = mockk<VenueRepository>()
    private val bowlFieldRepository = mockk<BowlFieldRepository>()
    private val ccgFieldRepository = mockk<ConferenceChampionshipFieldRepository>()
    private val conferenceRepository = mockk<ConferenceRepository>()
    private val playoffFieldRepository = mockk<PlayoffFieldRepository>()
    private val gameFieldRepository = mockk<GameFieldRepository>()
    private val teamFieldRepository = mockk<TeamFieldRepository>()
    private val service =
        FieldThemeService(
            bowlFieldRepository,
            ccgFieldRepository,
            conferenceRepository,
            playoffFieldRepository,
            mockk(relaxed = true),
            gameFieldRepository,
            teamFieldRepository,
            venueRepository,
        )
    private val home =
        Team().apply {
            name = "Wyoming"
            stadium = "War Memorial Stadium"
        }
    private val away = Team().apply { name = "Boise State" }
    private val play =
        Play().apply {
            possession = TeamSide.HOME
            quarter = 1
        }

    init {
        val venues =
            mapOf(
                "War Memorial Stadium" to venue("War Memorial Stadium", "MOUNTAINS"),
                "Mercedes-Benz Stadium" to venue("Mercedes-Benz Stadium", "BUILDING"),
                "Rose Bowl" to venue("Rose Bowl", "FOREST"),
            )
        every { venueRepository.findById(any()) } answers { Optional.ofNullable(venues[firstArg()]) }
        every { bowlFieldRepository.findById(any()) } returns Optional.empty()
        every { ccgFieldRepository.findById(any()) } returns Optional.empty()
        every { conferenceRepository.findById(any()) } returns Optional.empty()
        every { playoffFieldRepository.findById(any()) } returns Optional.empty()
        every { gameFieldRepository.findById(any()) } returns Optional.empty()
        every { teamFieldRepository.findById(any()) } returns Optional.empty()
    }

    @Test
    fun `a game played at a neutral site uses that site instead of the home team's stadium`() {
        val theme = service.resolve(play, game(GameType.OUT_OF_CONFERENCE, venue = "Mercedes-Benz Stadium"), home, away)

        assertEquals(StadiumBackdrop.BUILDING, theme.stadiumBackdrop(theme.leftSide()))
    }

    @Test
    fun `a home game with no venue falls back to the home team's stadium`() {
        val theme = service.resolve(play, game(GameType.OUT_OF_CONFERENCE, venue = null), home, away)

        assertEquals(StadiumBackdrop.MOUNTAINS, theme.stadiumBackdrop(theme.leftSide()))
    }

    @Test
    fun `a bowl uses the game's venue first and the bowl's stadium otherwise`() {
        val bowl =
            BowlField().apply {
                this.bowl = "Rose Bowl Game"
                venue = "Rose Bowl"
            }

        val atVenue = service.resolve(play, game(GameType.BOWL, venue = "Mercedes-Benz Stadium"), home, away, bowlFieldOverride = bowl)
        val noVenue = service.resolve(play, game(GameType.BOWL, venue = null), home, away, bowlFieldOverride = bowl)

        assertEquals(StadiumBackdrop.BUILDING, atVenue.stadiumBackdrop(atVenue.leftSide()))
        assertEquals(StadiumBackdrop.FOREST, noVenue.stadiumBackdrop(noVenue.leftSide()))
    }

    private fun venue(
        name: String,
        backdrop: String,
    ) = Venue(name).apply { design.stadiumBackdrop = backdrop }

    private fun game(
        type: GameType,
        venue: String?,
    ) = Game().apply {
        homeTeam = "Wyoming"
        awayTeam = "Boise State"
        gameType = type
        this.venue = venue
    }
}
