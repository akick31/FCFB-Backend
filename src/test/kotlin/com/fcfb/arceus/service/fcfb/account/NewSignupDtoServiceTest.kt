package com.fcfb.arceus.service.fcfb.account

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.model.account.NewSignup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NewSignupDtoServiceTest {
    private lateinit var newSignupDtoService: NewSignupDtoService

    @BeforeEach
    fun setup() {
        newSignupDtoService = NewSignupDtoService()
    }

    @Test
    fun `NewSignupDtoService should be properly annotated`() {
        val serviceAnnotation = NewSignupDtoService::class.annotations.find { it is org.springframework.stereotype.Service }
        assertNotNull(serviceAnnotation, "NewSignupDtoService should be annotated with @Service")
    }

    @Test
    fun `convertToNewSignupDTO should convert NewSignup to NewSignupDTO correctly`() {
        val newSignup =
            NewSignup().apply {
                id = 1L
                username = "newsignup"
                coachName = "New Coach"
                discordTag = "newsignup#9999"
                discordId = "987654321"
                email = "newsignup@example.com"
                hashedEmail = "hashed_email_1"
                password = "password123"
                salt = "salt123"
                position = CoachPosition.HEAD_COACH
                teamChoiceOne = "Team A"
                teamChoiceTwo = "Team B"
                teamChoiceThree = "Team C"
                offensivePlaybook = OffensivePlaybook.SPREAD
                defensivePlaybook = DefensivePlaybook.FOUR_THREE
                approved = true
            }

        val newSignupDTO = newSignupDtoService.convertToNewSignupDTO(newSignup)

        assertNotNull(newSignupDTO, "NewSignupDTO should not be null")
        assertEquals(newSignup.id, newSignupDTO.id)
        assertEquals(newSignup.username, newSignupDTO.username)
        assertEquals(newSignup.coachName, newSignupDTO.coachName)
        assertEquals(newSignup.discordTag, newSignupDTO.discordTag)
        assertEquals(newSignup.discordId, newSignupDTO.discordId)
        assertEquals(newSignup.position, newSignupDTO.position)
        assertEquals(newSignup.teamChoiceOne, newSignupDTO.teamChoiceOne)
        assertEquals(newSignup.teamChoiceTwo, newSignupDTO.teamChoiceTwo)
        assertEquals(newSignup.teamChoiceThree, newSignupDTO.teamChoiceThree)
        assertEquals(newSignup.offensivePlaybook, newSignupDTO.offensivePlaybook)
        assertEquals(newSignup.defensivePlaybook, newSignupDTO.defensivePlaybook)
        assertEquals(newSignup.approved, newSignupDTO.approved)
    }

    @Test
    fun `convertToNewSignupDTO should handle null values correctly`() {
        val newSignup =
            NewSignup().apply {
                id = 2L
                username = "newsignup2"
                coachName = "New Coach 2"
                discordTag = "newsignup2#8888"
                discordId = null
                email = "newsignup2@example.com"
                hashedEmail = "hashed_email_2"
                password = "password456"
                salt = "salt456"
                position = CoachPosition.DEFENSIVE_COORDINATOR
                teamChoiceOne = "Default Team 1"
                teamChoiceTwo = "Default Team 2"
                teamChoiceThree = "Default Team 3"
                offensivePlaybook = OffensivePlaybook.SPREAD
                defensivePlaybook = DefensivePlaybook.FOUR_THREE
                approved = false
            }

        val newSignupDTO = newSignupDtoService.convertToNewSignupDTO(newSignup)

        assertNotNull(newSignupDTO, "NewSignupDTO should not be null")
        assertEquals(newSignup.id, newSignupDTO.id)
        assertEquals(newSignup.username, newSignupDTO.username)
        assertEquals(newSignup.coachName, newSignupDTO.coachName)
        assertEquals(newSignup.discordTag, newSignupDTO.discordTag)
        assertEquals(newSignup.discordId, newSignupDTO.discordId)
        assertEquals(newSignup.position, newSignupDTO.position)
        assertEquals(newSignup.teamChoiceOne, newSignupDTO.teamChoiceOne)
        assertEquals(newSignup.teamChoiceTwo, newSignupDTO.teamChoiceTwo)
        assertEquals(newSignup.teamChoiceThree, newSignupDTO.teamChoiceThree)
        assertEquals(newSignup.offensivePlaybook, newSignupDTO.offensivePlaybook)
        assertEquals(newSignup.defensivePlaybook, newSignupDTO.defensivePlaybook)
        assertEquals(newSignup.approved, newSignupDTO.approved)
    }

    @Test
    fun `NewSignupDtoService should be instantiable`() {
        val service = NewSignupDtoService()
        assertNotNull(service, "NewSignupDtoService should be instantiable")
    }

    @Test
    fun `convertToNewSignupDTO should handle different enum values`() {
        val newSignup =
            NewSignup().apply {
                id = 3L
                username = "enumtest2"
                coachName = "Enum Test Coach 2"
                discordTag = "enumtest2#1111"
                email = "test@example.com"
                hashedEmail = "hashed_email"
                password = "password"
                salt = "salt"
                position = CoachPosition.DEFENSIVE_COORDINATOR
                teamChoiceOne = "Team A"
                teamChoiceTwo = "Team B"
                teamChoiceThree = "Team C"
                offensivePlaybook = OffensivePlaybook.FLEXBONE
                defensivePlaybook = DefensivePlaybook.FIVE_TWO
            }

        val newSignupDTO = newSignupDtoService.convertToNewSignupDTO(newSignup)

        assertEquals(CoachPosition.DEFENSIVE_COORDINATOR, newSignupDTO.position)
        assertEquals(OffensivePlaybook.FLEXBONE, newSignupDTO.offensivePlaybook)
        assertEquals(DefensivePlaybook.FIVE_TWO, newSignupDTO.defensivePlaybook)
    }
}
