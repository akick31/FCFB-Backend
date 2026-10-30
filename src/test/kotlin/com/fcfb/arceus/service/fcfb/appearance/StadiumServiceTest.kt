package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.GameStadiumRequest
import com.fcfb.arceus.dto.request.StadiumCreateRequest
import com.fcfb.arceus.dto.request.StadiumDesignRequest
import com.fcfb.arceus.dto.request.StadiumUpdateRequest
import com.fcfb.arceus.dto.request.TeamStadiumRequest
import com.fcfb.arceus.enums.animation.StadiumChangeAction
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.model.account.User
import com.fcfb.arceus.model.appearance.StadiumChangeLog
import com.fcfb.arceus.model.appearance.Venue
import com.fcfb.arceus.model.game.Game
import com.fcfb.arceus.model.schedule.Schedule
import com.fcfb.arceus.model.team.Team
import com.fcfb.arceus.repositories.appearance.BowlFieldRepository
import com.fcfb.arceus.repositories.appearance.PlayoffFieldRepository
import com.fcfb.arceus.repositories.appearance.StadiumChangeLogRepository
import com.fcfb.arceus.repositories.appearance.VenueRepository
import com.fcfb.arceus.repositories.conference.ConferenceRepository
import com.fcfb.arceus.repositories.game.GameRepository
import com.fcfb.arceus.repositories.schedule.ScheduleRepository
import com.fcfb.arceus.repositories.team.TeamRepository
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.service.fcfb.game.GameSplitTableSyncService
import com.fcfb.arceus.service.fcfb.team.TeamService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
    private val userService = mockk<UserService>()
    private val changes = mutableListOf<StadiumChangeLog>()
    private val changeLogRepository = mockk<StadiumChangeLogRepository>()
    private val changeLog = StadiumChangeLogService(changeLogRepository, userService)
    private val wyoming =
        Team().apply {
            name = "Wyoming"
            stadium = "War Memorial Stadium"
            coachUsernames = mutableListOf("cowboy")
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
            TeamEditPermissionService(teamService, userService),
            mockk(),
            mockk(),
            mockk(),
            mockk(relaxed = true),
            VenueReferenceService(venueRepository),
            changeLog,
        )
    private val history = StadiumHistoryService(changeLog, service)

    init {
        venues["War Memorial Stadium"] = Venue("War Memorial Stadium").apply { design.stadiumBackdrop = "MOUNTAINS" }
        venues["Mercedes-Benz Stadium"] = Venue("Mercedes-Benz Stadium")
        every { venueRepository.findById(any()) } answers { Optional.ofNullable(venues[firstArg()]) }
        every { venueRepository.existsById(any()) } answers { venues.containsKey(firstArg()) }
        every { venueRepository.save(any()) } answers { firstArg<Venue>().also { venues[it.name] = it } }
        every { teamService.getTeamByName("Wyoming") } returns wyoming
        every { userService.getUserById(42) } returns User().apply { username = "cowboy" }
        every { teamRepository.save(any()) } answers { firstArg() }
        every { gameRepository.save(any()) } answers { firstArg() }
        every { teamRepository.findAllByStadium(any()) } answers { listOf(wyoming).filter { it.stadium == firstArg() } }
        every { changeLogRepository.save(any()) } answers {
            firstArg<StadiumChangeLog>().also {
                it.id = changes.size + 1L
                changes += it
            }
        }
        every { changeLogRepository.findById(any()) } answers { Optional.ofNullable(changes.find { it.id == firstArg<Long>() }) }
        every { changeLogRepository.findAllByVenueOrderByChangedAtDescIdDesc(any()) } answers {
            changes.filter { it.venue == firstArg() }.reversed()
        }
    }

    @Test
    fun `each edit is logged with who made it and what changed, and saving without changes logs nothing`() {
        signIn("ROLE_USER", principal = "42")

        service.update("War Memorial Stadium", StadiumUpdateRequest(design = StadiumDesignRequest(stadiumBackdrop = "LAKE")))
        service.update("War Memorial Stadium", StadiumUpdateRequest(design = StadiumDesignRequest(stadiumBackdrop = "LAKE")))

        val logged = history.history("War Memorial Stadium")
        assertEquals(1, logged.size)
        assertEquals(StadiumChangeAction.UPDATE, logged.single().action)
        assertEquals("cowboy", logged.single().changedBy)
        assertEquals("Left end, stadium backdrop: MOUNTAINS → LAKE", logged.single().summary)
        assertEquals("MOUNTAINS", logged.single().before!!.design.stadiumBackdrop)
    }

    @Test
    fun `setting the roof from the stadium details keeps the rest of the design`() {
        signIn("ROLE_USER", principal = "42")
        service.update("War Memorial Stadium", StadiumUpdateRequest(design = StadiumDesignRequest(rightStadiumBackdrop = "LAKE")))

        service.update("War Memorial Stadium", StadiumUpdateRequest(domeStyle = "TRUSS"))

        val design = venueRepository.findById("War Memorial Stadium").get().design
        assertEquals("TRUSS", design.domeStyle)
        assertEquals("LAKE", design.rightStadiumBackdrop)
    }

    @Test
    fun `reverting restores the earlier design and is itself logged`() {
        signIn("ROLE_USER", principal = "42")
        service.update("War Memorial Stadium", StadiumUpdateRequest(design = StadiumDesignRequest(stadiumBackdrop = "LAKE")))

        history.revert(history.history("War Memorial Stadium").single().id)

        assertEquals("MOUNTAINS", venues["War Memorial Stadium"]!!.design.stadiumBackdrop)
        assertEquals(StadiumChangeAction.REVERT, history.history("War Memorial Stadium").first().action)
    }

    @Test
    fun `a coach reverting a design never undoes an admin's location change`() {
        signIn("ROLE_ADMIN")
        service.update("War Memorial Stadium", StadiumUpdateRequest(design = StadiumDesignRequest(stadiumBackdrop = "LAKE")))
        service.update("War Memorial Stadium", StadiumUpdateRequest(city = "Laramie", state = "WY"))
        signIn("ROLE_USER", principal = "42")

        history.revert(history.history("War Memorial Stadium").last().id)

        assertEquals("MOUNTAINS", venues["War Memorial Stadium"]!!.design.stadiumBackdrop)
        assertEquals("Laramie", venues["War Memorial Stadium"]!!.city)
    }

    @Test
    fun `retiring and creating are logged too`() {
        signIn("ROLE_ADMIN")

        service.create(StadiumCreateRequest(name = "Allegiant Stadium", copyFrom = "War Memorial Stadium"))
        service.setActive("Allegiant Stadium", false)

        val actions = history.history("Allegiant Stadium").map { it.action }
        assertEquals(listOf(StadiumChangeAction.RETIRE, StadiumChangeAction.CREATE), actions)
        assertEquals("Copied from War Memorial Stadium", history.history("Allegiant Stadium").last().summary)
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
    fun `a coach can rename and restyle their stadium but cannot move it`() {
        signIn("ROLE_USER", principal = "42")
        venues["War Memorial Stadium"]!!.apply {
            city = "Laramie"
            state = "WY"
        }

        service.update(
            "War Memorial Stadium",
            StadiumUpdateRequest(city = "Laramie", state = "WY", design = StadiumDesignRequest(stadiumBackdrop = "LAKE")),
        )

        assertEquals("LAKE", venues["War Memorial Stadium"]!!.design.stadiumBackdrop)
        assertThrows<UserForbiddenException> {
            service.update("War Memorial Stadium", StadiumUpdateRequest(city = "Cheyenne"))
        }
        assertThrows<UserForbiddenException> {
            service.update("War Memorial Stadium", StadiumUpdateRequest(state = "CO"))
        }
        assertThrows<UserForbiddenException> {
            service.update("War Memorial Stadium", StadiumUpdateRequest(country = "Canada"))
        }
        assertEquals("Laramie", venues["War Memorial Stadium"]!!.city)
    }

    @Test
    fun `a stadium can be found by its id so links survive renames`() {
        venues.getValue("War Memorial Stadium").venueId = 17
        every { venueRepository.findByVenueId(any()) } answers { venues.values.find { it.venueId == firstArg<Long>() } }

        val found = service.getById(17)

        assertEquals("War Memorial Stadium", found.name)
        assertEquals(17L, found.id)
        assertThrows<InvalidUniformException> { service.getById(99) }
    }

    @Test
    fun `a stadium can only be placed in a known country`() {
        signIn("ROLE_ADMIN")

        assertThrows<InvalidUniformException> {
            service.update("Mercedes-Benz Stadium", StadiumUpdateRequest(country = "Atlantis"))
        }
        service.update("Mercedes-Benz Stadium", StadiumUpdateRequest(country = "Türkiye"))

        assertEquals("Türkiye", venues["Mercedes-Benz Stadium"]!!.country)
        assertTrue(service.countries().containsAll(listOf("United States", "Canada", "Togo", "United Arab Emirates")))
        assertEquals("United States", service.countries().first())
    }

    @Test
    fun `admins can move a stadium`() {
        signIn("ROLE_ADMIN")

        service.update("Mercedes-Benz Stadium", StadiumUpdateRequest(city = "Regina", state = "SK", country = "Canada"))

        assertEquals("Regina", venues["Mercedes-Benz Stadium"]!!.city)
        assertEquals("Canada", venues["Mercedes-Benz Stadium"]!!.country)
        assertEquals(Venue.DEFAULT_COUNTRY, venues["War Memorial Stadium"]!!.country)
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

    private fun signIn(
        role: String,
        principal: String = "user",
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, listOf(SimpleGrantedAuthority(role)))
    }
}
