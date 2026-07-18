package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NewSignupDTOTest {
    @Test
    fun `NewSignupDTO should be a data class`() {
        val newSignupDTO = createTestNewSignupDTO()

        // Data classes automatically implement equals, hashCode, toString, and copy
        assertNotNull(newSignupDTO.toString())
        assertTrue(newSignupDTO.toString().contains("NewSignupDTO"))
    }

    @Test
    fun `NewSignupDTO should create instance with all properties`() {
        val newSignupDTO =
            NewSignupDTO(
                id = 1L,
                username = "newsignup",
                coachName = "New Coach",
                discordTag = "newsignup#9999",
                discordId = "987654321",
                position = CoachPosition.HEAD_COACH,
                teamChoiceOne = "Alabama",
                teamChoiceTwo = "Georgia",
                teamChoiceThree = "Texas",
                offensivePlaybook = OffensivePlaybook.SPREAD,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                approved = true,
            )

        assertEquals(1L, newSignupDTO.id)
        assertEquals("newsignup", newSignupDTO.username)
        assertEquals("New Coach", newSignupDTO.coachName)
        assertEquals("newsignup#9999", newSignupDTO.discordTag)
        assertEquals("987654321", newSignupDTO.discordId)
        assertEquals(CoachPosition.HEAD_COACH, newSignupDTO.position)
        assertEquals("Alabama", newSignupDTO.teamChoiceOne)
        assertEquals("Georgia", newSignupDTO.teamChoiceTwo)
        assertEquals("Texas", newSignupDTO.teamChoiceThree)
        assertEquals(OffensivePlaybook.SPREAD, newSignupDTO.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, newSignupDTO.defensivePlaybook)
        assertTrue(newSignupDTO.approved)
    }

    @Test
    fun `NewSignupDTO should handle null values correctly`() {
        val newSignupDTO =
            NewSignupDTO(
                id = 2L,
                username = "nullsignup",
                coachName = "Null Coach",
                discordTag = "nullsignup#5678",
                discordId = null,
                position = CoachPosition.OFFENSIVE_COORDINATOR,
                teamChoiceOne = null,
                teamChoiceTwo = null,
                teamChoiceThree = null,
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.THREE_FOUR,
                approved = false,
            )

        assertEquals(null, newSignupDTO.discordId)
        assertEquals(null, newSignupDTO.teamChoiceOne)
        assertEquals(null, newSignupDTO.teamChoiceTwo)
        assertEquals(null, newSignupDTO.teamChoiceThree)
        assertFalse(newSignupDTO.approved)
    }

    @Test
    fun `NewSignupDTO should handle different enum values`() {
        val newSignupDTO = createTestNewSignupDTO()

        // Test different CoachPosition values
        newSignupDTO.position = CoachPosition.DEFENSIVE_COORDINATOR
        assertEquals(CoachPosition.DEFENSIVE_COORDINATOR, newSignupDTO.position)

        newSignupDTO.position = CoachPosition.RETIRED
        assertEquals(CoachPosition.RETIRED, newSignupDTO.position)

        // Test different playbook values
        newSignupDTO.offensivePlaybook = OffensivePlaybook.FLEXBONE
        assertEquals(OffensivePlaybook.FLEXBONE, newSignupDTO.offensivePlaybook)

        newSignupDTO.defensivePlaybook = DefensivePlaybook.FIVE_TWO
        assertEquals(DefensivePlaybook.FIVE_TWO, newSignupDTO.defensivePlaybook)
    }

    @Test
    fun `NewSignupDTO properties should be mutable`() {
        val newSignupDTO = createTestNewSignupDTO()

        newSignupDTO.username = "updateduser"
        newSignupDTO.coachName = "Updated Coach"
        newSignupDTO.teamChoiceOne = "Michigan"
        newSignupDTO.approved = false

        assertEquals("updateduser", newSignupDTO.username)
        assertEquals("Updated Coach", newSignupDTO.coachName)
        assertEquals("Michigan", newSignupDTO.teamChoiceOne)
        assertFalse(newSignupDTO.approved)
    }

    @Test
    fun `NewSignupDTO should handle team choices correctly`() {
        val newSignupDTO = createTestNewSignupDTO()

        newSignupDTO.teamChoiceOne = "Ohio State"
        newSignupDTO.teamChoiceTwo = "Michigan"
        newSignupDTO.teamChoiceThree = "Penn State"

        assertEquals("Ohio State", newSignupDTO.teamChoiceOne)
        assertEquals("Michigan", newSignupDTO.teamChoiceTwo)
        assertEquals("Penn State", newSignupDTO.teamChoiceThree)
    }

    @Test
    fun `NewSignupDTO should handle approval status`() {
        val newSignupDTO = createTestNewSignupDTO()

        newSignupDTO.approved = true
        assertTrue(newSignupDTO.approved)

        newSignupDTO.approved = false
        assertFalse(newSignupDTO.approved)
    }

    @Test
    fun `NewSignupDTO data class should support copy functionality`() {
        val original = createTestNewSignupDTO()
        val copied = original.copy(username = "copieduser", approved = false)

        assertEquals("copieduser", copied.username)
        assertFalse(copied.approved)
        assertEquals(original.id, copied.id)
        assertEquals(original.coachName, copied.coachName)
        assertEquals(original.teamChoiceOne, copied.teamChoiceOne)
    }

    @Test
    fun `NewSignupDTO data class should support equality comparison`() {
        val newSignupDTO1 = createTestNewSignupDTO()
        val newSignupDTO2 = createTestNewSignupDTO()
        val newSignupDTO3 = newSignupDTO1.copy(username = "different")

        assertEquals(newSignupDTO1, newSignupDTO2)
        assertTrue(newSignupDTO1 != newSignupDTO3)
    }

    @Test
    fun `NewSignupDTO should handle special characters in string fields`() {
        val newSignupDTO = createTestNewSignupDTO()

        newSignupDTO.username = "user@domain.com"
        newSignupDTO.coachName = "Coach O'Brien-Smith"
        newSignupDTO.discordTag = "user#1234"
        newSignupDTO.teamChoiceOne = "Texas A&M"
        newSignupDTO.teamChoiceTwo = "Ole Miss"
        newSignupDTO.teamChoiceThree = "Miami (FL)"

        assertEquals("user@domain.com", newSignupDTO.username)
        assertEquals("Coach O'Brien-Smith", newSignupDTO.coachName)
        assertEquals("user#1234", newSignupDTO.discordTag)
        assertEquals("Texas A&M", newSignupDTO.teamChoiceOne)
        assertEquals("Ole Miss", newSignupDTO.teamChoiceTwo)
        assertEquals("Miami (FL)", newSignupDTO.teamChoiceThree)
    }

    @Test
    fun `NewSignupDTO should handle long team names`() {
        val newSignupDTO = createTestNewSignupDTO()

        val longTeamName = "A".repeat(100)
        newSignupDTO.teamChoiceOne = longTeamName
        newSignupDTO.teamChoiceTwo = longTeamName
        newSignupDTO.teamChoiceThree = longTeamName

        assertEquals(longTeamName, newSignupDTO.teamChoiceOne)
        assertEquals(longTeamName, newSignupDTO.teamChoiceTwo)
        assertEquals(longTeamName, newSignupDTO.teamChoiceThree)
    }

    @Test
    fun `NewSignupDTO should handle all playbook combinations`() {
        val newSignupDTO = createTestNewSignupDTO()

        // Test all offensive playbooks
        val offensivePlaybooks =
            listOf(
                OffensivePlaybook.AIR_RAID,
                OffensivePlaybook.SPREAD,
                OffensivePlaybook.PRO,
                OffensivePlaybook.FLEXBONE,
                OffensivePlaybook.WEST_COAST,
            )

        // Test all defensive playbooks
        val defensivePlaybooks =
            listOf(
                DefensivePlaybook.FOUR_THREE,
                DefensivePlaybook.THREE_FOUR,
                DefensivePlaybook.FIVE_TWO,
            )

        offensivePlaybooks.forEach { offensive ->
            newSignupDTO.offensivePlaybook = offensive
            assertEquals(offensive, newSignupDTO.offensivePlaybook)
        }

        defensivePlaybooks.forEach { defensive ->
            newSignupDTO.defensivePlaybook = defensive
            assertEquals(defensive, newSignupDTO.defensivePlaybook)
        }
    }

    @Test
    fun `NewSignupDTO should handle all coach positions`() {
        val newSignupDTO = createTestNewSignupDTO()

        val positions =
            listOf(
                CoachPosition.HEAD_COACH,
                CoachPosition.OFFENSIVE_COORDINATOR,
                CoachPosition.DEFENSIVE_COORDINATOR,
                CoachPosition.RETIRED,
            )

        positions.forEach { position ->
            newSignupDTO.position = position
            assertEquals(position, newSignupDTO.position)
        }
    }

    @Test
    fun `NewSignupDTO should handle mixed null and non-null team choices`() {
        val newSignupDTO = createTestNewSignupDTO()

        // Test with only first choice
        newSignupDTO.teamChoiceOne = "Alabama"
        newSignupDTO.teamChoiceTwo = null
        newSignupDTO.teamChoiceThree = null

        assertEquals("Alabama", newSignupDTO.teamChoiceOne)
        assertEquals(null, newSignupDTO.teamChoiceTwo)
        assertEquals(null, newSignupDTO.teamChoiceThree)

        // Test with first and second choice
        newSignupDTO.teamChoiceTwo = "Georgia"

        assertEquals("Alabama", newSignupDTO.teamChoiceOne)
        assertEquals("Georgia", newSignupDTO.teamChoiceTwo)
        assertEquals(null, newSignupDTO.teamChoiceThree)

        // Test with all choices
        newSignupDTO.teamChoiceThree = "Texas"

        assertEquals("Alabama", newSignupDTO.teamChoiceOne)
        assertEquals("Georgia", newSignupDTO.teamChoiceTwo)
        assertEquals("Texas", newSignupDTO.teamChoiceThree)
    }

    private fun createTestNewSignupDTO(): NewSignupDTO {
        return NewSignupDTO(
            id = 1L,
            username = "testsignup",
            coachName = "Test Coach",
            discordTag = "testsignup#1234",
            discordId = "123456789",
            position = CoachPosition.HEAD_COACH,
            teamChoiceOne = "Alabama",
            teamChoiceTwo = "Georgia",
            teamChoiceThree = "Texas",
            offensivePlaybook = OffensivePlaybook.SPREAD,
            defensivePlaybook = DefensivePlaybook.FOUR_THREE,
            approved = true,
        )
    }
}
