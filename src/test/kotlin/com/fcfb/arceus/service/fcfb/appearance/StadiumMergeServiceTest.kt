package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.StadiumMergeRequest
import com.fcfb.arceus.enums.animation.StadiumChangeAction
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.model.appearance.Venue
import com.fcfb.arceus.repositories.appearance.VenueRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

class StadiumMergeServiceTest {
    private val venues = mutableMapOf<String, Venue>()
    private val venueRepository = mockk<VenueRepository>(relaxed = true)
    private val stadiumService = mockk<StadiumService>(relaxed = true)
    private val changeLog = mockk<StadiumChangeLogService>(relaxed = true)
    private val service = StadiumMergeService(venueRepository, VenueReferenceService(venueRepository), stadiumService, changeLog)

    init {
        venues["Mosaic Stadium, Regina Saskatchewan"] =
            Venue("Mosaic Stadium, Regina Saskatchewan", city = "Regina", state = "SK", capacity = 33350).apply { country = "Canada" }
        venues["Mosaic Stadium"] = Venue("Mosaic Stadium").apply { design.stadiumBackdrop = "LAKE" }
        every { venueRepository.findById(any()) } answers { Optional.ofNullable(venues[firstArg()]) }
        every { venueRepository.save(any()) } answers { firstArg<Venue>().also { venues[it.name] = it } }
        every { venueRepository.delete(any()) } answers { venues.remove(firstArg<Venue>().name) }
    }

    @AfterEach
    fun clearAuthentication() = SecurityContextHolder.clearContext()

    @Test
    fun `merging moves every reference to the kept stadium and keeps its design`() {
        signIn("ROLE_ADMIN")

        service.merge(StadiumMergeRequest(source = "Mosaic Stadium, Regina Saskatchewan", into = "Mosaic Stadium"))

        val duplicate = "Mosaic Stadium, Regina Saskatchewan"
        verify {
            venueRepository.renameTeamStadiums(duplicate, "Mosaic Stadium")
            venueRepository.renameGameVenues(duplicate, "Mosaic Stadium")
            venueRepository.renamePresentationVenues(duplicate, "Mosaic Stadium")
            venueRepository.renameScheduleVenues(duplicate, "Mosaic Stadium")
            venueRepository.renameChampionshipVenues(duplicate, "Mosaic Stadium")
            venueRepository.renameBowlLastVenues(duplicate, "Mosaic Stadium")
            venueRepository.renameBowlFieldVenues(duplicate, "Mosaic Stadium")
            venueRepository.renamePlayoffFieldVenues(duplicate, "Mosaic Stadium")
        }
        assertEquals(setOf("Mosaic Stadium"), venues.keys)
        val kept = venues.getValue("Mosaic Stadium")
        assertEquals("LAKE", kept.design.stadiumBackdrop)
        assertEquals("Regina", kept.city)
        assertEquals("Canada", kept.country)
        assertEquals(33350, kept.capacity)
        verify { venueRepository.renameChangeLogVenues(duplicate, "Mosaic Stadium") }
        verify { changeLog.record("Mosaic Stadium", StadiumChangeAction.MERGE, any(), any(), "Merged $duplicate into this stadium") }
    }

    @Test
    fun `a stadium cannot be merged into itself`() {
        signIn("ROLE_ADMIN")

        assertThrows<InvalidUniformException> {
            service.merge(StadiumMergeRequest(source = "Mosaic Stadium", into = "Mosaic Stadium"))
        }
    }

    @Test
    fun `only admins and commissioners can merge`() {
        signIn("ROLE_USER")

        assertThrows<UserForbiddenException> {
            service.merge(StadiumMergeRequest(source = "Mosaic Stadium, Regina Saskatchewan", into = "Mosaic Stadium"))
        }
    }

    private fun signIn(role: String) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("user", null, listOf(SimpleGrantedAuthority(role)))
    }
}
