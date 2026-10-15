package com.fcfb.arceus.service.fcfb.account

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.model.account.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserDtoServiceTest {
    private lateinit var userDtoService: UserDtoService

    @BeforeEach
    fun setup() {
        userDtoService = UserDtoService()
    }

    @Test
    fun `UserDtoService should be properly annotated`() {
        val serviceAnnotation = UserDtoService::class.annotations.find { it is org.springframework.stereotype.Service }
        assertNotNull(serviceAnnotation, "UserDtoService should be annotated with @Service")
    }

    @Test
    fun `convertToUserDTO should convert User to UserDTO correctly`() {
        val user =
            User().apply {
                id = 1L
                username = "testuser"
                coachName = "Test Coach"
                discordTag = "testuser#1234"
                discordId = "123456789"
                position = CoachPosition.HEAD_COACH
                role = UserRole.ADMIN
                team = "Test Team"
                delayOfGameInstances = 5
                wins = 10
                losses = 5
                winPercentage = 0.667
                conferenceWins = 8
                conferenceLosses = 2
                conferenceChampionshipWins = 1
                conferenceChampionshipLosses = 0
                bowlWins = 2
                bowlLosses = 1
                playoffWins = 1
                playoffLosses = 0
                nationalChampionshipWins = 1
                nationalChampionshipLosses = 0
                offensivePlaybook = OffensivePlaybook.AIR_RAID
                defensivePlaybook = DefensivePlaybook.THREE_FOUR
                averageResponseTime = 120.5
                delayOfGameWarningOptOut = false
            }

        val userDTO = userDtoService.convertToUserDTO(user)

        assertNotNull(userDTO, "UserDTO should not be null")
        assertEquals(user.id, userDTO.id)
        assertEquals(user.username, userDTO.username)
        assertEquals(user.coachName, userDTO.coachName)
        assertEquals(user.discordTag, userDTO.discordTag)
        assertEquals(user.discordId, userDTO.discordId)
        assertEquals(user.position, userDTO.position)
        assertEquals(user.role, userDTO.role)
        assertEquals(user.team, userDTO.team)
        assertEquals(user.delayOfGameInstances, userDTO.delayOfGameInstances)
        assertEquals(user.wins, userDTO.wins)
        assertEquals(user.losses, userDTO.losses)
        assertEquals(user.winPercentage, userDTO.winPercentage)
        assertEquals(user.conferenceWins, userDTO.conferenceWins)
        assertEquals(user.conferenceLosses, userDTO.conferenceLosses)
        assertEquals(user.conferenceChampionshipWins, userDTO.conferenceChampionshipWins)
        assertEquals(user.conferenceChampionshipLosses, userDTO.conferenceChampionshipLosses)
        assertEquals(user.bowlWins, userDTO.bowlWins)
        assertEquals(user.bowlLosses, userDTO.bowlLosses)
        assertEquals(user.playoffWins, userDTO.playoffWins)
        assertEquals(user.playoffLosses, userDTO.playoffLosses)
        assertEquals(user.nationalChampionshipWins, userDTO.nationalChampionshipWins)
        assertEquals(user.nationalChampionshipLosses, userDTO.nationalChampionshipLosses)
        assertEquals(user.offensivePlaybook, userDTO.offensivePlaybook)
        assertEquals(user.defensivePlaybook, userDTO.defensivePlaybook)
        assertEquals(user.averageResponseTime, userDTO.averageResponseTime)
        assertEquals(user.delayOfGameWarningOptOut, userDTO.delayOfGameWarningOptOut)
    }

    @Test
    fun `convertToUserDTO should handle null values correctly`() {
        val user =
            User().apply {
                id = 2L
                username = "testuser2"
                coachName = "Test Coach 2"
                discordTag = "testuser2#5678"
                discordId = null
                position = CoachPosition.OFFENSIVE_COORDINATOR
                role = UserRole.USER
                team = null
                delayOfGameInstances = 0
                wins = 0
                losses = 0
                winPercentage = 0.0
                conferenceWins = 0
                conferenceLosses = 0
                conferenceChampionshipWins = 0
                conferenceChampionshipLosses = 0
                bowlWins = 0
                bowlLosses = 0
                playoffWins = 0
                playoffLosses = 0
                nationalChampionshipWins = 0
                nationalChampionshipLosses = 0
                offensivePlaybook = OffensivePlaybook.AIR_RAID
                defensivePlaybook = DefensivePlaybook.THREE_FOUR
                averageResponseTime = 0.0
                delayOfGameWarningOptOut = false
            }

        val userDTO = userDtoService.convertToUserDTO(user)

        assertNotNull(userDTO, "UserDTO should not be null")
        assertEquals(user.id, userDTO.id)
        assertEquals(user.username, userDTO.username)
        assertEquals(user.coachName, userDTO.coachName)
        assertEquals(user.discordTag, userDTO.discordTag)
        assertEquals(user.discordId, userDTO.discordId)
        assertEquals(user.position, userDTO.position)
        assertEquals(user.role, userDTO.role)
        assertEquals(user.team, userDTO.team)
        assertEquals(user.delayOfGameInstances, userDTO.delayOfGameInstances)
        assertEquals(user.wins, userDTO.wins)
        assertEquals(user.losses, userDTO.losses)
        assertEquals(user.winPercentage, userDTO.winPercentage)
        assertEquals(user.offensivePlaybook, userDTO.offensivePlaybook)
        assertEquals(user.defensivePlaybook, userDTO.defensivePlaybook)
        assertEquals(user.averageResponseTime, userDTO.averageResponseTime)
        assertEquals(user.delayOfGameWarningOptOut, userDTO.delayOfGameWarningOptOut)
    }

    @Test
    fun `UserDtoService should be instantiable`() {
        val service = UserDtoService()
        assertNotNull(service, "UserDtoService should be instantiable")
    }

    @Test
    fun `convertToUserDTO should handle different enum values`() {
        val user =
            User().apply {
                id = 3L
                username = "enumtest"
                coachName = "Enum Test Coach"
                discordTag = "enumtest#0000"
                position = CoachPosition.OFFENSIVE_COORDINATOR
                role = UserRole.ADMIN
                offensivePlaybook = OffensivePlaybook.PRO
                defensivePlaybook = DefensivePlaybook.FIVE_TWO
            }

        val userDTO = userDtoService.convertToUserDTO(user)

        assertEquals(CoachPosition.OFFENSIVE_COORDINATOR, userDTO.position)
        assertEquals(UserRole.ADMIN, userDTO.role)
        assertEquals(OffensivePlaybook.PRO, userDTO.offensivePlaybook)
        assertEquals(DefensivePlaybook.FIVE_TWO, userDTO.defensivePlaybook)
    }
}
