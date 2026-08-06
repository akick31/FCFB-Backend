package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserTest {
    @Test
    fun `test User entity annotations`() {
        val user = User()

        val entityAnnotation = User::class.java.getAnnotation(javax.persistence.Entity::class.java)
        assertNotNull(entityAnnotation)

        val tableAnnotation = User::class.java.getAnnotation(javax.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
        assertEquals("user", tableAnnotation.name)
    }

    @Test
    fun `test User default constructor`() {
        val user = User()

        assertEquals(0L, user.id)
        assertEquals(0, user.delayOfGameInstances)
        assertEquals(0, user.wins)
        assertEquals(0, user.losses)
        assertEquals(0, user.conferenceWins)
        assertEquals(0, user.conferenceLosses)
        assertEquals(0, user.conferenceChampionshipWins)
        assertEquals(0, user.conferenceChampionshipLosses)
        assertEquals(0, user.bowlWins)
        assertEquals(0, user.bowlLosses)
        assertEquals(0, user.playoffWins)
        assertEquals(0, user.playoffLosses)
        assertEquals(0, user.nationalChampionshipWins)
        assertEquals(0, user.nationalChampionshipLosses)
        assertEquals(0.0, user.winPercentage)
        assertEquals(UserRole.USER, user.role)
        assertFalse(user.delayOfGameWarningOptOut)
        assertEquals(0.0, user.averageResponseTime)
        assertNull(user.discordId)
        assertNull(user.team)
        assertNull(user.resetToken)
        assertNull(user.resetTokenExpiration)
    }

    @Test
    fun `test User parameterized constructor`() {
        val user =
            User(
                username = "testuser",
                coachName = "Test Coach",
                discordTag = "test#1234",
                discordId = "123456789",
                email = "test@example.com",
                hashedEmail = "hashed@example.com",
                password = "hashedpassword",
                position = CoachPosition.HEAD_COACH,
                role = UserRole.ADMIN,
                salt = "salt123",
                team = "Test Team",
                delayOfGameInstances = 2,
                wins = 10,
                losses = 5,
                winPercentage = 66.7,
                conferenceWins = 8,
                conferenceLosses = 2,
                conferenceChampionshipWins = 1,
                conferenceChampionshipLosses = 0,
                bowlWins = 2,
                bowlLosses = 1,
                playoffWins = 1,
                playoffLosses = 0,
                nationalChampionshipWins = 1,
                nationalChampionshipLosses = 0,
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                averageResponseTime = 45.5,
                delayOfGameWarningOptOut = true,
                resetToken = "reset123",
                resetTokenExpiration = "2024-12-31",
            )

        assertEquals("testuser", user.username)
        assertEquals("Test Coach", user.coachName)
        assertEquals("test#1234", user.discordTag)
        assertEquals("123456789", user.discordId)
        assertEquals("test@example.com", user.email)
        assertEquals("hashed@example.com", user.hashedEmail)
        assertEquals("hashedpassword", user.password)
        assertEquals(CoachPosition.HEAD_COACH, user.position)
        assertEquals(UserRole.ADMIN, user.role)
        assertEquals("salt123", user.salt)
        assertEquals("Test Team", user.team)
        assertEquals(2, user.delayOfGameInstances)
        assertEquals(10, user.wins)
        assertEquals(5, user.losses)
        assertEquals(66.7, user.winPercentage)
        assertEquals(8, user.conferenceWins)
        assertEquals(2, user.conferenceLosses)
        assertEquals(1, user.conferenceChampionshipWins)
        assertEquals(0, user.conferenceChampionshipLosses)
        assertEquals(2, user.bowlWins)
        assertEquals(1, user.bowlLosses)
        assertEquals(1, user.playoffWins)
        assertEquals(0, user.playoffLosses)
        assertEquals(1, user.nationalChampionshipWins)
        assertEquals(0, user.nationalChampionshipLosses)
        assertEquals(OffensivePlaybook.AIR_RAID, user.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, user.defensivePlaybook)
        assertEquals(45.5, user.averageResponseTime)
        assertTrue(user.delayOfGameWarningOptOut)
        assertEquals("reset123", user.resetToken)
        assertEquals("2024-12-31", user.resetTokenExpiration)
    }

    @Test
    fun `test User property mutability`() {
        val user = User()

        user.id = 1L
        user.username = "newuser"
        user.coachName = "New Coach"
        user.discordTag = "new#5678"
        user.discordId = "987654321"
        user.email = "new@example.com"
        user.hashedEmail = "newhashed@example.com"
        user.password = "newpassword"
        user.position = CoachPosition.OFFENSIVE_COORDINATOR
        user.role = UserRole.CONFERENCE_COMMISSIONER
        user.salt = "newsalt"
        user.team = "New Team"
        user.delayOfGameInstances = 3
        user.wins = 15
        user.losses = 8
        user.conferenceWins = 12
        user.conferenceLosses = 3
        user.conferenceChampionshipWins = 2
        user.conferenceChampionshipLosses = 1
        user.bowlWins = 3
        user.bowlLosses = 2
        user.playoffWins = 2
        user.playoffLosses = 1
        user.nationalChampionshipWins = 2
        user.nationalChampionshipLosses = 1
        user.winPercentage = 75.0
        user.offensivePlaybook = OffensivePlaybook.SPREAD
        user.defensivePlaybook = DefensivePlaybook.THREE_FOUR
        user.delayOfGameWarningOptOut = true
        user.averageResponseTime = 60.0
        user.resetToken = "newreset"
        user.resetTokenExpiration = "2025-01-01"

        assertEquals(1L, user.id)
        assertEquals("newuser", user.username)
        assertEquals("New Coach", user.coachName)
        assertEquals("new#5678", user.discordTag)
        assertEquals("987654321", user.discordId)
        assertEquals("new@example.com", user.email)
        assertEquals("newhashed@example.com", user.hashedEmail)
        assertEquals("newpassword", user.password)
        assertEquals(CoachPosition.OFFENSIVE_COORDINATOR, user.position)
        assertEquals(UserRole.CONFERENCE_COMMISSIONER, user.role)
        assertEquals("newsalt", user.salt)
        assertEquals("New Team", user.team)
        assertEquals(3, user.delayOfGameInstances)
        assertEquals(15, user.wins)
        assertEquals(8, user.losses)
        assertEquals(12, user.conferenceWins)
        assertEquals(3, user.conferenceLosses)
        assertEquals(2, user.conferenceChampionshipWins)
        assertEquals(1, user.conferenceChampionshipLosses)
        assertEquals(3, user.bowlWins)
        assertEquals(2, user.bowlLosses)
        assertEquals(2, user.playoffWins)
        assertEquals(1, user.playoffLosses)
        assertEquals(2, user.nationalChampionshipWins)
        assertEquals(1, user.nationalChampionshipLosses)
        assertEquals(75.0, user.winPercentage)
        assertEquals(OffensivePlaybook.SPREAD, user.offensivePlaybook)
        assertEquals(DefensivePlaybook.THREE_FOUR, user.defensivePlaybook)
        assertTrue(user.delayOfGameWarningOptOut)
        assertEquals(60.0, user.averageResponseTime)
        assertEquals("newreset", user.resetToken)
        assertEquals("2025-01-01", user.resetTokenExpiration)
    }

    @Test
    fun `test CoachPosition enum values`() {
        assertEquals("Head Coach", CoachPosition.HEAD_COACH.description)
        assertEquals("Offensive Coordinator", CoachPosition.OFFENSIVE_COORDINATOR.description)
        assertEquals("Defensive Coordinator", CoachPosition.DEFENSIVE_COORDINATOR.description)
        assertEquals("Retired", CoachPosition.RETIRED.description)
    }

    @Test
    fun `test CoachPosition fromString method`() {
        assertEquals(CoachPosition.HEAD_COACH, CoachPosition.fromString("Head Coach"))
        assertEquals(CoachPosition.OFFENSIVE_COORDINATOR, CoachPosition.fromString("Offensive Coordinator"))
        assertEquals(CoachPosition.DEFENSIVE_COORDINATOR, CoachPosition.fromString("Defensive Coordinator"))
        assertEquals(CoachPosition.RETIRED, CoachPosition.fromString("Retired"))
        assertNull(CoachPosition.fromString("Invalid Position"))
    }

    @Test
    fun `test Role enum values`() {
        assertEquals("User", UserRole.USER.description)
        assertEquals("Conference Commissioner", UserRole.CONFERENCE_COMMISSIONER.description)
        assertEquals("Admin", UserRole.ADMIN.description)
    }

    @Test
    fun `test Role fromString method`() {
        assertEquals(UserRole.USER, UserRole.fromString("User"))
        assertEquals(UserRole.CONFERENCE_COMMISSIONER, UserRole.fromString("Conference Commissioner"))
        assertEquals(UserRole.ADMIN, UserRole.fromString("Admin"))
        assertNull(UserRole.fromString("Invalid Role"))
    }

    @Test
    fun `test User with null optional fields`() {
        val user = User()
        user.discordId = null
        user.team = null
        user.resetToken = null
        user.resetTokenExpiration = null

        assertNull(user.discordId)
        assertNull(user.team)
        assertNull(user.resetToken)
        assertNull(user.resetTokenExpiration)
    }

    @Test
    fun `test User with all enum values`() {
        val user = User()

        CoachPosition.entries.forEach { position ->
            user.position = position
            assertEquals(position, user.position)
        }

        UserRole.entries.forEach { role ->
            user.role = role
            assertEquals(role, user.role)
        }
    }

    @Test
    fun `test User with different playbook combinations`() {
        val user = User()

        OffensivePlaybook.entries.forEach { offensivePlaybook ->
            user.offensivePlaybook = offensivePlaybook
            assertEquals(offensivePlaybook, user.offensivePlaybook)
        }

        DefensivePlaybook.entries.forEach { defensivePlaybook ->
            user.defensivePlaybook = defensivePlaybook
            assertEquals(defensivePlaybook, user.defensivePlaybook)
        }
    }

    @Test
    fun `test User statistics calculations`() {
        val user = User()

        user.wins = 10
        user.losses = 5
        user.conferenceWins = 8
        user.conferenceLosses = 2
        user.conferenceChampionshipWins = 1
        user.conferenceChampionshipLosses = 0
        user.bowlWins = 2
        user.bowlLosses = 1
        user.playoffWins = 1
        user.playoffLosses = 0
        user.nationalChampionshipWins = 1
        user.nationalChampionshipLosses = 0
        user.winPercentage = 66.7

        assertEquals(10, user.wins)
        assertEquals(5, user.losses)
        assertEquals(8, user.conferenceWins)
        assertEquals(2, user.conferenceLosses)
        assertEquals(1, user.conferenceChampionshipWins)
        assertEquals(0, user.conferenceChampionshipLosses)
        assertEquals(2, user.bowlWins)
        assertEquals(1, user.bowlLosses)
        assertEquals(1, user.playoffWins)
        assertEquals(0, user.playoffLosses)
        assertEquals(1, user.nationalChampionshipWins)
        assertEquals(0, user.nationalChampionshipLosses)
        assertEquals(66.7, user.winPercentage)
    }

    @Test
    fun `test User delay of game tracking`() {
        val user = User()

        user.delayOfGameInstances = 0
        assertEquals(0, user.delayOfGameInstances)

        user.delayOfGameInstances = 5
        assertEquals(5, user.delayOfGameInstances)

        user.delayOfGameWarningOptOut = true
        assertTrue(user.delayOfGameWarningOptOut)

        user.delayOfGameWarningOptOut = false
        assertFalse(user.delayOfGameWarningOptOut)
    }

    @Test
    fun `test User response time tracking`() {
        val user = User()

        user.averageResponseTime = 0.0
        assertEquals(0.0, user.averageResponseTime)

        user.averageResponseTime = 45.5
        assertEquals(45.5, user.averageResponseTime)

        user.averageResponseTime = 120.0
        assertEquals(120.0, user.averageResponseTime)
    }
}
