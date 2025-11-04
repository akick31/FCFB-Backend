package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeamTest {
    @Test
    fun `test Team entity annotations`() {
        val team = Team()

        // Test that the class has the correct JPA annotations
        val entityAnnotation = Team::class.java.getAnnotation(javax.persistence.Entity::class.java)
        assertNotNull(entityAnnotation)

        val tableAnnotation = Team::class.java.getAnnotation(javax.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
        assertEquals("team", tableAnnotation.name)
    }

    @Test
    fun `test Team default constructor`() {
        val team = Team()

        assertEquals(0, team.id)
        assertNull(team.name)
        assertNull(team.abbreviation)
        assertNull(team.shortName)
        assertNull(team.logo)
        assertNull(team.scorebugLogo)
        assertEquals(mutableListOf<String>(), team.coachUsernames)
        assertEquals(mutableListOf<String>(), team.coachNames)
        assertEquals(mutableListOf<String>(), team.coachDiscordTags)
        assertEquals(mutableListOf<String>(), team.coachDiscordIds)
        assertNull(team.primaryColor)
        assertNull(team.secondaryColor)
        assertNull(team.coachesPollRanking)
        assertNull(team.playoffCommitteeRanking)
        assertNull(team.subdivision)
        assertNull(team.conference)
        assertEquals(0, team.currentWins)
        assertEquals(0, team.currentLosses)
        assertEquals(0, team.overallWins)
        assertEquals(0, team.overallLosses)
        assertEquals(0, team.currentConferenceWins)
        assertEquals(0, team.currentConferenceLosses)
        assertEquals(0, team.overallConferenceWins)
        assertEquals(0, team.overallConferenceLosses)
        assertEquals(0, team.conferenceChampionshipWins)
        assertEquals(0, team.conferenceChampionshipLosses)
        assertEquals(0, team.bowlWins)
        assertEquals(0, team.bowlLosses)
        assertEquals(0, team.playoffWins)
        assertEquals(0, team.playoffLosses)
        assertEquals(0, team.nationalChampionshipWins)
        assertEquals(0, team.nationalChampionshipLosses)
        assertFalse(team.isTaken)
        assertTrue(team.active)

        // Test that lateinit properties are not initialized
        assertThrows<UninitializedPropertyAccessException> {
            team.offensivePlaybook
        }
        assertThrows<UninitializedPropertyAccessException> {
            team.defensivePlaybook
        }
    }

    @Test
    fun `test Team parameterized constructor`() {
        val coachUsernames = mutableListOf("coach1", "coach2")
        val coachNames = mutableListOf("Coach One", "Coach Two")
        val coachDiscordTags = mutableListOf("coach1#1234", "coach2#5678")
        val coachDiscordIds = mutableListOf("123456789", "987654321")

        val team =
            Team(
                logo = "logo.png",
                scorebugLogo = "scorebug_logo.png",
                coachUsernames = coachUsernames,
                coachNames = coachNames,
                coachDiscordTags = coachDiscordTags,
                coachDiscordIds = coachDiscordIds,
                name = "Test University",
                shortName = "Test U",
                abbreviation = "TU",
                primaryColor = "#FF0000",
                secondaryColor = "#0000FF",
                coachesPollRanking = 5,
                playoffCommitteeRanking = 3,
                subdivision = Subdivision.FBS,
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                conference = Conference.SEC,
                currentWins = 8,
                currentLosses = 2,
                overallWins = 25,
                overallLosses = 10,
                currentConferenceWins = 6,
                currentConferenceLosses = 1,
                overallConferenceWins = 18,
                overallConferenceLosses = 7,
                conferenceChampionshipWins = 2,
                conferenceChampionshipLosses = 1,
                bowlWins = 3,
                bowlLosses = 2,
                playoffWins = 1,
                playoffLosses = 0,
                nationalChampionshipWins = 1,
                nationalChampionshipLosses = 0,
                isTaken = true,
                active = true,
                currentElo = 1500.0,
                overallElo = 1500.0,
            )

        assertEquals("logo.png", team.logo)
        assertEquals("scorebug_logo.png", team.scorebugLogo)
        assertEquals(coachUsernames, team.coachUsernames)
        assertEquals(coachNames, team.coachNames)
        assertEquals(coachDiscordTags, team.coachDiscordTags)
        assertEquals(coachDiscordIds, team.coachDiscordIds)
        assertEquals("Test University", team.name)
        assertEquals("Test U", team.shortName)
        assertEquals("TU", team.abbreviation)
        assertEquals("#FF0000", team.primaryColor)
        assertEquals("#0000FF", team.secondaryColor)
        assertEquals(5, team.coachesPollRanking)
        assertEquals(3, team.playoffCommitteeRanking)
        assertEquals(Subdivision.FBS, team.subdivision)
        assertEquals(OffensivePlaybook.AIR_RAID, team.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, team.defensivePlaybook)
        assertEquals(Conference.SEC, team.conference)
        assertEquals(8, team.currentWins)
        assertEquals(2, team.currentLosses)
        assertEquals(25, team.overallWins)
        assertEquals(10, team.overallLosses)
        assertEquals(6, team.currentConferenceWins)
        assertEquals(1, team.currentConferenceLosses)
        assertEquals(18, team.overallConferenceWins)
        assertEquals(7, team.overallConferenceLosses)
        assertEquals(2, team.conferenceChampionshipWins)
        assertEquals(1, team.conferenceChampionshipLosses)
        assertEquals(3, team.bowlWins)
        assertEquals(2, team.bowlLosses)
        assertEquals(1, team.playoffWins)
        assertEquals(0, team.playoffLosses)
        assertEquals(1, team.nationalChampionshipWins)
        assertEquals(0, team.nationalChampionshipLosses)
        assertTrue(team.isTaken)
        assertTrue(team.active)
    }

    @Test
    fun `test Team property mutability`() {
        val team = Team()

        // Test property mutability
        team.id = 1
        team.name = "New University"
        team.abbreviation = "NU"
        team.shortName = "New U"
        team.logo = "new_logo.png"
        team.scorebugLogo = "new_scorebug_logo.png"
        team.coachUsernames = mutableListOf("newcoach")
        team.coachNames = mutableListOf("New Coach")
        team.coachDiscordTags = mutableListOf("newcoach#9999")
        team.coachDiscordIds = mutableListOf("111222333")
        team.primaryColor = "#00FF00"
        team.secondaryColor = "#FFFF00"
        team.coachesPollRanking = 10
        team.playoffCommitteeRanking = 8
        team.subdivision = Subdivision.FCS
        team.offensivePlaybook = OffensivePlaybook.SPREAD
        team.defensivePlaybook = DefensivePlaybook.THREE_FOUR
        team.conference = Conference.BIG_TEN
        team.currentWins = 12
        team.currentLosses = 1
        team.overallWins = 35
        team.overallLosses = 15
        team.currentConferenceWins = 9
        team.currentConferenceLosses = 0
        team.overallConferenceWins = 25
        team.overallConferenceLosses = 10
        team.conferenceChampionshipWins = 3
        team.conferenceChampionshipLosses = 2
        team.bowlWins = 5
        team.bowlLosses = 3
        team.playoffWins = 2
        team.playoffLosses = 1
        team.nationalChampionshipWins = 2
        team.nationalChampionshipLosses = 1
        team.isTaken = false
        team.active = false

        assertEquals(1, team.id)
        assertEquals("New University", team.name)
        assertEquals("NU", team.abbreviation)
        assertEquals("New U", team.shortName)
        assertEquals("new_logo.png", team.logo)
        assertEquals("new_scorebug_logo.png", team.scorebugLogo)
        assertEquals(mutableListOf("newcoach"), team.coachUsernames)
        assertEquals(mutableListOf("New Coach"), team.coachNames)
        assertEquals(mutableListOf("newcoach#9999"), team.coachDiscordTags)
        assertEquals(mutableListOf("111222333"), team.coachDiscordIds)
        assertEquals("#00FF00", team.primaryColor)
        assertEquals("#FFFF00", team.secondaryColor)
        assertEquals(10, team.coachesPollRanking)
        assertEquals(8, team.playoffCommitteeRanking)
        assertEquals(Subdivision.FCS, team.subdivision)
        assertEquals(OffensivePlaybook.SPREAD, team.offensivePlaybook)
        assertEquals(DefensivePlaybook.THREE_FOUR, team.defensivePlaybook)
        assertEquals(Conference.BIG_TEN, team.conference)
        assertEquals(12, team.currentWins)
        assertEquals(1, team.currentLosses)
        assertEquals(35, team.overallWins)
        assertEquals(15, team.overallLosses)
        assertEquals(9, team.currentConferenceWins)
        assertEquals(0, team.currentConferenceLosses)
        assertEquals(25, team.overallConferenceWins)
        assertEquals(10, team.overallConferenceLosses)
        assertEquals(3, team.conferenceChampionshipWins)
        assertEquals(2, team.conferenceChampionshipLosses)
        assertEquals(5, team.bowlWins)
        assertEquals(3, team.bowlLosses)
        assertEquals(2, team.playoffWins)
        assertEquals(1, team.playoffLosses)
        assertEquals(2, team.nationalChampionshipWins)
        assertEquals(1, team.nationalChampionshipLosses)
        assertFalse(team.isTaken)
        assertFalse(team.active)
    }

    @Test
    fun `test Conference enum values`() {
        assertEquals("ACC", Conference.ACC.description)
        assertEquals("American", Conference.AMERICAN.description)
        assertEquals("Big 12", Conference.BIG_12.description)
        assertEquals("Big Ten", Conference.BIG_TEN.description)
        assertEquals("Fake Team", Conference.FAKE_TEAM.description)
        assertEquals("FBS Independent", Conference.FBS_INDEPENDENT.description)
        assertEquals("MAC", Conference.MAC.description)
        assertEquals("Mountain West", Conference.MOUNTAIN_WEST.description)
        assertEquals("Pac-12", Conference.PAC_12.description)
        assertEquals("SEC", Conference.SEC.description)
        assertEquals("Sun Belt", Conference.SUN_BELT.description)
        assertEquals("Missouri Valley", Conference.MISSOURI_VALLEY.description)
        assertEquals("Colonial", Conference.COLONIAL.description)
        assertEquals("NEC", Conference.NEC.description)
    }

    @Test
    fun `test Conference fromString method`() {
        assertEquals(Conference.ACC, Conference.fromString("ACC"))
        assertEquals(Conference.SEC, Conference.fromString("SEC"))
        assertEquals(Conference.BIG_TEN, Conference.fromString("Big Ten"))
        assertEquals(Conference.PAC_12, Conference.fromString("Pac-12"))
        assertEquals(Conference.FBS_INDEPENDENT, Conference.fromString("FBS Independent"))
        assertNull(Conference.fromString("Invalid Conference"))
    }

    @Test
    fun `test Team with null optional fields`() {
        val team = Team()
        team.name = null
        team.abbreviation = null
        team.shortName = null
        team.logo = null
        team.scorebugLogo = null
        team.coachUsernames = null
        team.coachNames = null
        team.coachDiscordTags = null
        team.coachDiscordIds = null
        team.primaryColor = null
        team.secondaryColor = null
        team.coachesPollRanking = null
        team.playoffCommitteeRanking = null
        team.subdivision = null
        team.conference = null

        assertNull(team.name)
        assertNull(team.abbreviation)
        assertNull(team.shortName)
        assertNull(team.logo)
        assertNull(team.scorebugLogo)
        assertNull(team.coachUsernames)
        assertNull(team.coachNames)
        assertNull(team.coachDiscordTags)
        assertNull(team.coachDiscordIds)
        assertNull(team.primaryColor)
        assertNull(team.secondaryColor)
        assertNull(team.coachesPollRanking)
        assertNull(team.playoffCommitteeRanking)
        assertNull(team.subdivision)
        assertNull(team.conference)
    }

    @Test
    fun `test Team with all conference values`() {
        val team = Team()

        // Test all Conference values
        Conference.entries.forEach { conference ->
            team.conference = conference
            assertEquals(conference, team.conference)
        }
    }

    @Test
    fun `test Team with different playbook combinations`() {
        val team = Team()

        // Test different offensive playbook combinations
        OffensivePlaybook.entries.forEach { offensivePlaybook ->
            team.offensivePlaybook = offensivePlaybook
            assertEquals(offensivePlaybook, team.offensivePlaybook)
        }

        // Test different defensive playbook combinations
        DefensivePlaybook.entries.forEach { defensivePlaybook ->
            team.defensivePlaybook = defensivePlaybook
            assertEquals(defensivePlaybook, team.defensivePlaybook)
        }
    }

    @Test
    fun `test Team with different subdivision values`() {
        val team = Team()

        // Test all Subdivision values
        Subdivision.entries.forEach { subdivision ->
            team.subdivision = subdivision
            assertEquals(subdivision, team.subdivision)
        }
    }

    @Test
    fun `test Team statistics tracking`() {
        val team = Team()

        // Set up current season statistics
        team.currentWins = 10
        team.currentLosses = 2
        team.currentConferenceWins = 8
        team.currentConferenceLosses = 1

        // Set up overall statistics
        team.overallWins = 45
        team.overallLosses = 25
        team.overallConferenceWins = 35
        team.overallConferenceLosses = 20

        // Set up championship statistics
        team.conferenceChampionshipWins = 3
        team.conferenceChampionshipLosses = 2
        team.bowlWins = 5
        team.bowlLosses = 3
        team.playoffWins = 2
        team.playoffLosses = 1
        team.nationalChampionshipWins = 1
        team.nationalChampionshipLosses = 1

        assertEquals(10, team.currentWins)
        assertEquals(2, team.currentLosses)
        assertEquals(8, team.currentConferenceWins)
        assertEquals(1, team.currentConferenceLosses)
        assertEquals(45, team.overallWins)
        assertEquals(25, team.overallLosses)
        assertEquals(35, team.overallConferenceWins)
        assertEquals(20, team.overallConferenceLosses)
        assertEquals(3, team.conferenceChampionshipWins)
        assertEquals(2, team.conferenceChampionshipLosses)
        assertEquals(5, team.bowlWins)
        assertEquals(3, team.bowlLosses)
        assertEquals(2, team.playoffWins)
        assertEquals(1, team.playoffLosses)
        assertEquals(1, team.nationalChampionshipWins)
        assertEquals(1, team.nationalChampionshipLosses)
    }

    @Test
    fun `test Team ranking tracking`() {
        val team = Team()

        team.coachesPollRanking = 1
        assertEquals(1, team.coachesPollRanking)

        team.coachesPollRanking = 25
        assertEquals(25, team.coachesPollRanking)

        team.coachesPollRanking = null
        assertNull(team.coachesPollRanking)

        team.playoffCommitteeRanking = 4
        assertEquals(4, team.playoffCommitteeRanking)

        team.playoffCommitteeRanking = 12
        assertEquals(12, team.playoffCommitteeRanking)

        team.playoffCommitteeRanking = null
        assertNull(team.playoffCommitteeRanking)
    }

    @Test
    fun `test Team coach management`() {
        val team = Team()

        val usernames = mutableListOf("coach1", "coach2", "coach3")
        val names = mutableListOf("Head Coach", "Offensive Coordinator", "Defensive Coordinator")
        val discordTags = mutableListOf("headcoach#1234", "offcoach#5678", "defcoach#9999")
        val discordIds = mutableListOf("111111111", "222222222", "333333333")

        team.coachUsernames = usernames
        team.coachNames = names
        team.coachDiscordTags = discordTags
        team.coachDiscordIds = discordIds

        assertEquals(usernames, team.coachUsernames)
        assertEquals(names, team.coachNames)
        assertEquals(discordTags, team.coachDiscordTags)
        assertEquals(discordIds, team.coachDiscordIds)

        // Test adding a coach
        team.coachUsernames?.add("coach4")
        team.coachNames?.add("Special Teams Coach")
        team.coachDiscordTags?.add("stcoach#0000")
        team.coachDiscordIds?.add("444444444")

        assertEquals(4, team.coachUsernames?.size)
        assertEquals(4, team.coachNames?.size)
        assertEquals(4, team.coachDiscordTags?.size)
        assertEquals(4, team.coachDiscordIds?.size)
    }

    @Test
    fun `test Team status flags`() {
        val team = Team()

        // Test isTaken flag
        team.isTaken = true
        assertTrue(team.isTaken)

        team.isTaken = false
        assertFalse(team.isTaken)

        // Test active flag
        team.active = false
        assertFalse(team.active)

        team.active = true
        assertTrue(team.active)
    }

    @Test
    fun `test Team color management`() {
        val team = Team()

        team.primaryColor = "#FF0000"
        team.secondaryColor = "#0000FF"

        assertEquals("#FF0000", team.primaryColor)
        assertEquals("#0000FF", team.secondaryColor)

        team.primaryColor = "#00FF00"
        team.secondaryColor = "#FFFF00"

        assertEquals("#00FF00", team.primaryColor)
        assertEquals("#FFFF00", team.secondaryColor)

        team.primaryColor = null
        team.secondaryColor = null

        assertNull(team.primaryColor)
        assertNull(team.secondaryColor)
    }
}
