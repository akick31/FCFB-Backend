package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Team
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeamRepositoryTest {
    private lateinit var teamRepository: TeamRepository

    @BeforeEach
    fun setUp() {
        teamRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and findById`() {
        val team =
            createTestTeam(
                id = 1,
                name = "Alabama",
                abbreviation = "ALA",
                shortName = "Alabama",
                active = true,
                isTaken = false,
                conference = "SEC",
                subdivision = Subdivision.FBS,
            )

        every { teamRepository.save(any()) } returns team
        every { teamRepository.findById(1) } returns java.util.Optional.of(team)

        val savedTeam = teamRepository.save(team)
        val foundTeam = teamRepository.findById(savedTeam.id).get()

        assertNotNull(foundTeam)
        assertEquals(1, foundTeam.id)
        assertEquals("Alabama", foundTeam.name)
        assertEquals("ALA", foundTeam.abbreviation)
        assertEquals("Alabama", foundTeam.shortName)
        assertTrue(foundTeam.active)
        assertFalse(foundTeam.isTaken)
        assertEquals("SEC", foundTeam.conference)
        assertEquals(Subdivision.FBS, foundTeam.subdivision)
    }

    @Test
    fun `test getAllActiveTeams`() {
        val activeTeam1 =
            createTestTeam(
                id = 1,
                name = "Alabama",
                active = true,
            )
        val activeTeam2 =
            createTestTeam(
                id = 2,
                name = "Auburn",
                active = true,
            )
        val inactiveTeam =
            createTestTeam(
                id = 3,
                name = "Inactive Team",
                active = false,
            )

        val activeTeams = listOf(activeTeam1, activeTeam2)
        every { teamRepository.getAllActiveTeams() } returns activeTeams

        val result = teamRepository.getAllActiveTeams()

        assertTrue(result.size >= 2)
        assertTrue(result.any { it.name == "Alabama" })
        assertTrue(result.any { it.name == "Auburn" })
        assertFalse(result.any { it.name == "Inactive Team" })
    }

    @Test
    fun `test getTeamsInConference`() {
        val secTeam1 =
            createTestTeam(
                id = 1,
                name = "Alabama",
                conference = "SEC",
            )
        val secTeam2 =
            createTestTeam(
                id = 2,
                name = "Auburn",
                conference = "SEC",
            )
        val big12Team =
            createTestTeam(
                id = 3,
                name = "Texas",
                conference = "BIG_12",
            )

        val secTeams = listOf(secTeam1, secTeam2)
        every { teamRepository.getTeamsInConference("SEC") } returns secTeams

        val result = teamRepository.getTeamsInConference("SEC")

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertTrue(result.any { it.name == "Alabama" })
        assertTrue(result.any { it.name == "Auburn" })
        assertFalse(result.any { it.name == "Texas" })
    }

    @Test
    fun `test getOpenTeams`() {
        val openTeam1 =
            createTestTeam(
                id = 1,
                name = "Open Team 1",
                isTaken = false,
            )
        val openTeam2 =
            createTestTeam(
                id = 2,
                name = "Open Team 2",
                isTaken = false,
            )
        val takenTeam =
            createTestTeam(
                id = 3,
                name = "Taken Team",
                isTaken = true,
            )

        val openTeams = listOf("Open Team 1", "Open Team 2")
        every { teamRepository.getOpenTeams() } returns openTeams

        val result = teamRepository.getOpenTeams()

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertTrue(result.any { it == "Open Team 1" })
        assertTrue(result.any { it == "Open Team 2" })
        assertFalse(result.any { it == "Taken Team" })
    }

    @Test
    fun `test getTeamByName returns null when not found`() {
        every { teamRepository.getTeamByName("Nonexistent Team") } returns null

        val result = teamRepository.getTeamByName("Nonexistent Team")

        assertNull(result)
    }

    @Test
    fun `test getCoachesPollRankingById returns null when no ranking`() {
        val team =
            createTestTeam(
                id = 1,
                name = "Unranked Team",
                coachesPollRanking = null,
            )

        every { teamRepository.getCoachesPollRankingById(1) } returns null

        val result = teamRepository.getCoachesPollRankingById(1)

        assertNull(result)
    }

    @Test
    fun `test getCoachesPollRankingById`() {
        val team =
            createTestTeam(
                id = 1,
                name = "Ranked Team",
                coachesPollRanking = 5,
            )

        every { teamRepository.getCoachesPollRankingById(1) } returns 5

        val result = teamRepository.getCoachesPollRankingById(1)

        assertEquals(5, result)
    }

    @Test
    fun `test count`() {
        every { teamRepository.count() } returns 10L

        val result = teamRepository.count()

        assertEquals(10L, result)
    }

    @Test
    fun `test findAll`() {
        val teams =
            listOf(
                createTestTeam(id = 1, name = "Team 1"),
                createTestTeam(id = 2, name = "Team 2"),
                createTestTeam(id = 3, name = "Team 3"),
            )

        every { teamRepository.findAll() } returns teams

        val result = teamRepository.findAll()

        assertEquals(3, result.count())
        assertTrue(result.any { it.name == "Team 1" })
        assertTrue(result.any { it.name == "Team 2" })
        assertTrue(result.any { it.name == "Team 3" })
    }

    @Test
    fun `test resetWinsAndLosses`() {
        every { teamRepository.resetWinsAndLosses() } returns Unit

        teamRepository.resetWinsAndLosses()

        verify { teamRepository.resetWinsAndLosses() }
    }

    private fun createTestTeam(
        id: Int = 1,
        name: String = "Test Team",
        abbreviation: String = "TEST",
        shortName: String = "Test",
        logo: String = "logo.png",
        scorebugLogo: String = "scorebug.png",
        coachUsernames: MutableList<String> = mutableListOf("coach1"),
        coachNames: MutableList<String> = mutableListOf("Coach One"),
        coachDiscordTags: MutableList<String> = mutableListOf("coach1#1234"),
        coachDiscordIds: MutableList<String> = mutableListOf("123456789"),
        primaryColor: String = "#FF0000",
        secondaryColor: String = "#0000FF",
        coachesPollRanking: Int? = null,
        playoffCommitteeRanking: Int? = null,
        subdivision: Subdivision = Subdivision.FBS,
        offensivePlaybook: OffensivePlaybook = OffensivePlaybook.PRO,
        defensivePlaybook: DefensivePlaybook = DefensivePlaybook.THREE_FOUR,
        conference: String = "SEC",
        currentWins: Int = 0,
        currentLosses: Int = 0,
        currentConferenceWins: Int = 0,
        currentConferenceLosses: Int = 0,
        overallWins: Int = 0,
        overallLosses: Int = 0,
        overallConferenceWins: Int = 0,
        overallConferenceLosses: Int = 0,
        conferenceChampionshipWins: Int = 0,
        conferenceChampionshipLosses: Int = 0,
        bowlWins: Int = 0,
        bowlLosses: Int = 0,
        playoffWins: Int = 0,
        playoffLosses: Int = 0,
        nationalChampionshipWins: Int = 0,
        nationalChampionshipLosses: Int = 0,
        active: Boolean = true,
        isTaken: Boolean = false,
    ): Team {
        return Team().apply {
            this.id = id
            this.name = name
            this.abbreviation = abbreviation
            this.shortName = shortName
            this.logo = logo
            this.scorebugLogo = scorebugLogo
            this.coachUsernames = coachUsernames
            this.coachNames = coachNames
            this.coachDiscordTags = coachDiscordTags
            this.coachDiscordIds = coachDiscordIds
            this.primaryColor = primaryColor
            this.secondaryColor = secondaryColor
            this.coachesPollRanking = coachesPollRanking
            this.playoffCommitteeRanking = playoffCommitteeRanking
            this.subdivision = subdivision
            this.offensivePlaybook = offensivePlaybook
            this.defensivePlaybook = defensivePlaybook
            this.conference = conference
            this.currentWins = currentWins
            this.currentLosses = currentLosses
            this.currentConferenceWins = currentConferenceWins
            this.currentConferenceLosses = currentConferenceLosses
            this.overallWins = overallWins
            this.overallLosses = overallLosses
            this.overallConferenceWins = overallConferenceWins
            this.overallConferenceLosses = overallConferenceLosses
            this.conferenceChampionshipWins = conferenceChampionshipWins
            this.conferenceChampionshipLosses = conferenceChampionshipLosses
            this.bowlWins = bowlWins
            this.bowlLosses = bowlLosses
            this.playoffWins = playoffWins
            this.playoffLosses = playoffLosses
            this.nationalChampionshipWins = nationalChampionshipWins
            this.nationalChampionshipLosses = nationalChampionshipLosses
            this.active = active
            this.isTaken = isTaken
        }
    }
}
