package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserDTOTest {
    @Test
    fun `UserDTO should be a data class`() {
        val userDTO = createTestUserDTO()

        // Data classes automatically implement equals, hashCode, toString, and copy
        assertNotNull(userDTO.toString())
        assertTrue(userDTO.toString().contains("UserDTO"))
    }

    @Test
    fun `UserDTO should create instance with all properties`() {
        val userDTO =
            UserDTO(
                id = 1L,
                username = "testuser",
                coachName = "Test Coach",
                discordTag = "testuser#1234",
                discordId = "123456789",
                position = CoachPosition.HEAD_COACH,
                role = UserRole.USER,
                team = "Alabama",
                delayOfGameInstances = 2,
                wins = 10,
                losses = 5,
                winPercentage = 0.667,
                conferenceWins = 8,
                conferenceLosses = 2,
                conferenceChampionshipWins = 1,
                conferenceChampionshipLosses = 0,
                bowlWins = 3,
                bowlLosses = 1,
                playoffWins = 2,
                playoffLosses = 0,
                nationalChampionshipWins = 1,
                nationalChampionshipLosses = 0,
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                averageResponseTime = 120.5,
                delayOfGameWarningOptOut = false,
            )

        assertEquals(1L, userDTO.id)
        assertEquals("testuser", userDTO.username)
        assertEquals("Test Coach", userDTO.coachName)
        assertEquals("testuser#1234", userDTO.discordTag)
        assertEquals("123456789", userDTO.discordId)
        assertEquals(CoachPosition.HEAD_COACH, userDTO.position)
        assertEquals(UserRole.USER, userDTO.role)
        assertEquals("Alabama", userDTO.team)
        assertEquals(2, userDTO.delayOfGameInstances)
        assertEquals(10, userDTO.wins)
        assertEquals(5, userDTO.losses)
        assertEquals(0.667, userDTO.winPercentage)
        assertEquals(8, userDTO.conferenceWins)
        assertEquals(2, userDTO.conferenceLosses)
        assertEquals(1, userDTO.conferenceChampionshipWins)
        assertEquals(0, userDTO.conferenceChampionshipLosses)
        assertEquals(3, userDTO.bowlWins)
        assertEquals(1, userDTO.bowlLosses)
        assertEquals(2, userDTO.playoffWins)
        assertEquals(0, userDTO.playoffLosses)
        assertEquals(1, userDTO.nationalChampionshipWins)
        assertEquals(0, userDTO.nationalChampionshipLosses)
        assertEquals(OffensivePlaybook.AIR_RAID, userDTO.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, userDTO.defensivePlaybook)
        assertEquals(120.5, userDTO.averageResponseTime)
        assertFalse(userDTO.delayOfGameWarningOptOut)
    }

    @Test
    fun `UserDTO should handle null values correctly`() {
        val userDTO =
            UserDTO(
                id = 2L,
                username = "nulluser",
                coachName = "Null Coach",
                discordTag = "nulluser#5678",
                discordId = null,
                position = CoachPosition.OFFENSIVE_COORDINATOR,
                role = UserRole.ADMIN,
                team = null,
                delayOfGameInstances = 0,
                wins = 0,
                losses = 0,
                winPercentage = 0.0,
                conferenceWins = 0,
                conferenceLosses = 0,
                conferenceChampionshipWins = 0,
                conferenceChampionshipLosses = 0,
                bowlWins = 0,
                bowlLosses = 0,
                playoffWins = 0,
                playoffLosses = 0,
                nationalChampionshipWins = 0,
                nationalChampionshipLosses = 0,
                offensivePlaybook = OffensivePlaybook.SPREAD,
                defensivePlaybook = DefensivePlaybook.THREE_FOUR,
                averageResponseTime = 0.0,
                delayOfGameWarningOptOut = true,
            )

        assertEquals(null, userDTO.discordId)
        assertEquals(null, userDTO.team)
        assertTrue(userDTO.delayOfGameWarningOptOut)
    }

    @Test
    fun `UserDTO should handle different enum values`() {
        val userDTO = createTestUserDTO()

        // Test different CoachPosition values
        userDTO.position = CoachPosition.DEFENSIVE_COORDINATOR
        assertEquals(CoachPosition.DEFENSIVE_COORDINATOR, userDTO.position)

        userDTO.position = CoachPosition.RETIRED
        assertEquals(CoachPosition.RETIRED, userDTO.position)

        // Test different Role values
        userDTO.role = UserRole.CONFERENCE_COMMISSIONER
        assertEquals(UserRole.CONFERENCE_COMMISSIONER, userDTO.role)

        userDTO.role = UserRole.ADMIN
        assertEquals(UserRole.ADMIN, userDTO.role)

        // Test different playbook values
        userDTO.offensivePlaybook = OffensivePlaybook.FLEXBONE
        assertEquals(OffensivePlaybook.FLEXBONE, userDTO.offensivePlaybook)

        userDTO.defensivePlaybook = DefensivePlaybook.FIVE_TWO
        assertEquals(DefensivePlaybook.FIVE_TWO, userDTO.defensivePlaybook)
    }

    @Test
    fun `UserDTO properties should be mutable`() {
        val userDTO = createTestUserDTO()

        userDTO.username = "updateduser"
        userDTO.coachName = "Updated Coach"
        userDTO.wins = 15
        userDTO.losses = 3
        userDTO.winPercentage = 0.833

        assertEquals("updateduser", userDTO.username)
        assertEquals("Updated Coach", userDTO.coachName)
        assertEquals(15, userDTO.wins)
        assertEquals(3, userDTO.losses)
        assertEquals(0.833, userDTO.winPercentage)
    }

    @Test
    fun `UserDTO should handle statistics correctly`() {
        val userDTO = createTestUserDTO()

        // Test win/loss statistics
        userDTO.wins = 12
        userDTO.losses = 3
        userDTO.winPercentage = 0.8

        assertEquals(12, userDTO.wins)
        assertEquals(3, userDTO.losses)
        assertEquals(0.8, userDTO.winPercentage)

        // Test conference statistics
        userDTO.conferenceWins = 9
        userDTO.conferenceLosses = 1

        assertEquals(9, userDTO.conferenceWins)
        assertEquals(1, userDTO.conferenceLosses)

        // Test championship statistics
        userDTO.conferenceChampionshipWins = 2
        userDTO.nationalChampionshipWins = 1

        assertEquals(2, userDTO.conferenceChampionshipWins)
        assertEquals(1, userDTO.nationalChampionshipWins)
    }

    @Test
    fun `UserDTO should handle response time statistics`() {
        val userDTO = createTestUserDTO()

        userDTO.averageResponseTime = 95.5
        userDTO.delayOfGameInstances = 1
        userDTO.delayOfGameWarningOptOut = true

        assertEquals(95.5, userDTO.averageResponseTime)
        assertEquals(1, userDTO.delayOfGameInstances)
        assertTrue(userDTO.delayOfGameWarningOptOut)
    }

    @Test
    fun `UserDTO data class should support copy functionality`() {
        val original = createTestUserDTO()
        val copied = original.copy(username = "copieduser", wins = 20)

        assertEquals("copieduser", copied.username)
        assertEquals(20, copied.wins)
        assertEquals(original.id, copied.id)
        assertEquals(original.coachName, copied.coachName)
        assertEquals(original.losses, copied.losses)
    }

    @Test
    fun `UserDTO data class should support equality comparison`() {
        val userDTO1 = createTestUserDTO()
        val userDTO2 = createTestUserDTO()
        val userDTO3 = userDTO1.copy(username = "different")

        assertEquals(userDTO1, userDTO2)
        assertTrue(userDTO1 != userDTO3)
    }

    @Test
    fun `UserDTO should handle large statistical values`() {
        val userDTO = createTestUserDTO()

        userDTO.wins = 999
        userDTO.losses = 1
        userDTO.delayOfGameInstances = 100
        userDTO.averageResponseTime = 9999.99

        assertEquals(999, userDTO.wins)
        assertEquals(1, userDTO.losses)
        assertEquals(100, userDTO.delayOfGameInstances)
        assertEquals(9999.99, userDTO.averageResponseTime)
    }

    @Test
    fun `UserDTO should handle special characters in string fields`() {
        val userDTO = createTestUserDTO()

        userDTO.username = "user@domain.com"
        userDTO.coachName = "Coach O'Brien-Smith"
        userDTO.discordTag = "user#1234"
        userDTO.team = "Texas A&M"

        assertEquals("user@domain.com", userDTO.username)
        assertEquals("Coach O'Brien-Smith", userDTO.coachName)
        assertEquals("user#1234", userDTO.discordTag)
        assertEquals("Texas A&M", userDTO.team)
    }

    private fun createTestUserDTO(): UserDTO {
        return UserDTO(
            id = 1L,
            username = "testuser",
            coachName = "Test Coach",
            discordTag = "testuser#1234",
            discordId = "123456789",
            position = CoachPosition.HEAD_COACH,
            role = UserRole.USER,
            team = "Alabama",
            delayOfGameInstances = 2,
            wins = 10,
            losses = 5,
            winPercentage = 0.667,
            conferenceWins = 8,
            conferenceLosses = 2,
            conferenceChampionshipWins = 1,
            conferenceChampionshipLosses = 0,
            bowlWins = 3,
            bowlLosses = 1,
            playoffWins = 2,
            playoffLosses = 0,
            nationalChampionshipWins = 1,
            nationalChampionshipLosses = 0,
            offensivePlaybook = OffensivePlaybook.AIR_RAID,
            defensivePlaybook = DefensivePlaybook.FOUR_THREE,
            averageResponseTime = 120.5,
            delayOfGameWarningOptOut = false,
        )
    }
}
