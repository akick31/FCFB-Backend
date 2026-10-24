package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.GameStadiumRequest
import com.fcfb.arceus.dto.request.StadiumCreateRequest
import com.fcfb.arceus.dto.request.StadiumDesignRequest
import com.fcfb.arceus.dto.request.StadiumUpdateRequest
import com.fcfb.arceus.dto.request.TeamStadiumRequest
import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.model.appearance.Venue
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.appearance.BowlFieldRepository
import com.fcfb.arceus.repositories.appearance.PlayoffFieldRepository
import com.fcfb.arceus.repositories.appearance.VenueRepository
import com.fcfb.arceus.repositories.conference.ConferenceRepository
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.schedule.ScheduleRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.game.GameSplitTableSyncService
import com.fcfb.arceus.service.fcfb.team.TeamService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

class StadiumServiceTest {
    private val venues = mutableMapOf<String, Venue>()
    private val venueRepository = mockk<VenueRepository>(relaxed = true)
    private val teamRepository = mockk<TeamRepository>(relaxed = true)
    private val teamService = mockk<TeamService>()
    private val gameRepository = mockk<GameRepository>(relaxed = true)
    private val scheduleRepository = mockk<ScheduleRepository>(relaxed = true)
    private val splitSync = mockk<GameSplitTableSyncService>(relaxed = true)
    private val wyoming =
        Team().apply {
            name = "Wyoming"
            stadium = "War Memorial Stadium"
        }

    private val service =
        StadiumService(
            venueRepository,
            teamRepository,
            teamService,
            mockk<BowlFieldRepository>(relaxed = true),
            mockk<PlayoffFieldRepository>(relaxed = true),
            mockk<ConferenceRepository>(relaxed = true),
            gameRepository,
            scheduleRepository,
            splitSync,
            TeamEditPermissionService(teamService, mockk()),
            mockk(),
            mockk(),
            mockk(),
            mockk(relaxed = true),
        )

    init {
        venues["War Memorial Stadium"] = Venue("War Memorial Stadium").apply { design.stadiumBackdrop = "MOUNTAINS" }
        venues["Mercedes-Benz Stadium"] = Venue("Mercedes-Benz Stadium")
        every { venueRepository.findById(any()) } answers { Optional.ofNullable(venues[firstArg()]) }
        every { venueRepository.existsById(any()) } answers { venues.containsKey(firstArg()) }
        every { venueRepository.save(any()) } answers { firstArg<Venue>().also { venues[it.name] = it } }
        every { teamService.getTeamByName("Wyoming") } returns wyoming
        every { teamRepository.save(any()) } answers { firstArg() }
        every { gameRepository.save(any()) } answers { firstArg() }
        every { teamRepository.findAllByStadium(any()) } answers { listOf(wyoming).filter { it.stadium == firstArg() } }
    }

    @AfterEach
    fun clearAuthentication() = SecurityContextHolder.clearContext()

    @Test
    fun `renaming a stadium carries every team, game and event reference with it`() {
        signIn("ROLE_ADMIN")
        every { venueRepository.renameVenue("War Memorial Stadium", "Jonah Field") } answers {
            venues["Jonah Field"] = venues.remove("War Memorial Stadium")!!.also { it.name = "Jonah Field" }
        }

        service.update("War Memorial Stadium", StadiumUpdateRequest(name = "Jonah Field"))

        verify {
            venueRepository.renameVenue("War Memorial Stadium", "Jonah Field")
            venueRepository.renameTeamStadiums("War Memorial Stadium", "Jonah Field")
            venueRepository.renameGameVenues("War Memorial Stadium", "Jonah Field")
            venueRepository.renamePresentationVenues("War Memorial Stadium", "Jonah Field")
            venueRepository.renameScheduleVenues("War Memorial Stadium", "Jonah Field")
            venueRepository.renameChampionshipVenues("War Memorial Stadium", "Jonah Field")
            venueRepository.renameBowlFieldVenues("War Memorial Stadium", "Jonah Field")
            venueRepository.renamePlayoffFieldVenues("War Memorial Stadium", "Jonah Field")
        }
        assertEquals("MOUNTAINS", venues["Jonah Field"]!!.design.stadiumBackdrop)
    }

    @Test
    fun `building a new stadium leaves the old one intact so the team can move back`() {
        signIn("ROLE_ADMIN")

        service.assignTeam(TeamStadiumRequest(team = "Wyoming", venue = "New War Memorial", buildNew = true))

        assertEquals("New War Memorial", wyoming.stadium)
        assertEquals("CROWD", venues["New War Memorial"]!!.design.stadiumBackdrop ?: "CROWD")
        assertEquals("MOUNTAINS", venues["War Memorial Stadium"]!!.design.stadiumBackdrop)

        service.assignTeam(TeamStadiumRequest(team = "Wyoming", venue = "War Memorial Stadium"))

        assertEquals("War Memorial Stadium", wyoming.stadium)
    }

    @Test
    fun `retiring a stadium keeps its design for old games`() {
        signIn("ROLE_ADMIN")

        service.setActive("War Memorial Stadium", false)

        assertFalse(venues["War Memorial Stadium"]!!.active)
        assertEquals("MOUNTAINS", venues["War Memorial Stadium"]!!.design.stadiumBackdrop)
        verify(exactly = 0) { venueRepository.delete(any()) }
    }

    @Test
    fun `a new stadium can start as a copy of another`() {
        signIn("ROLE_ADMIN")

        service.create(StadiumCreateRequest(name = "Allegiant Stadium", copyFrom = "War Memorial Stadium"))

        assertEquals("MOUNTAINS", venues["Allegiant Stadium"]!!.design.stadiumBackdrop)
    }

    @Test
    fun `only admins can edit a neutral site`() {
        signIn("ROLE_USER")

        assertThrows<UserForbiddenException> {
            service.update("Mercedes-Benz Stadium", StadiumUpdateRequest(design = StadiumDesignRequest(stadiumBackdrop = "BUILDING")))
        }
    }

    @Test
    fun `picking a game's stadium updates the game, its presentation copy and its schedule entry`() {
        signIn("ROLE_ADMIN")
        val game = Game().apply { gameId = 7 }
        val schedule = Schedule().apply { gameId = 7 }
        every { gameRepository.getGameById(7) } returns game
        every { scheduleRepository.findByGameId(7) } returns schedule
        val saved = slot<Schedule>()
        every { scheduleRepository.save(capture(saved)) } answers { firstArg() }

        service.assignGame(GameStadiumRequest(gameId = 7, venue = "Mercedes-Benz Stadium"))

        assertEquals("Mercedes-Benz Stadium", game.venue)
        assertEquals("Mercedes-Benz Stadium", saved.captured.venue)
        verify { splitSync.sync(game) }
    }

    private fun signIn(role: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority(role)))
    }
}
