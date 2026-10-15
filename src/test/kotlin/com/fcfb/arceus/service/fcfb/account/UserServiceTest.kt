package com.fcfb.arceus.service.fcfb.account

import com.fcfb.arceus.dto.request.UpdateUserRequest
import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.exception.user.UserForbiddenException
import com.fcfb.arceus.model.account.User
import com.fcfb.arceus.repositories.account.UserCredentialRepository
import com.fcfb.arceus.repositories.account.UserPreferencesRepository
import com.fcfb.arceus.repositories.account.UserRepository
import com.fcfb.arceus.service.fcfb.coach.CoachStintService
import com.fcfb.arceus.service.log.UsernameHistoryService
import com.fcfb.arceus.util.EncryptionUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder

class UserServiceTest {
    private val userRepository: UserRepository = mockk()
    private val encryptionUtils: EncryptionUtils = mockk()
    private val userDtoService: UserDtoService = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val usernameHistoryService: UsernameHistoryService = mockk()
    private val coachStintService: CoachStintService = mockk()
    private val userCredentialRepository: UserCredentialRepository = mockk(relaxed = true)
    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private lateinit var userService: UserService

    private val ownerUser =
        User().apply {
            id = 1L
            username = "owner"
            password = "hashed-password"
            email = "owner@example.com"
            hashedEmail = "hashed-owner-email"
            salt = "salt"
        }

    private val ownerUserDTO =
        UserDTO(
            id = 1L,
            username = "owner",
            coachName = "Owner Coach",
            discordTag = "owner#1234",
            discordId = "111",
            position = CoachPosition.HEAD_COACH,
            role = UserRole.USER,
            team = "Test Team",
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
            offensivePlaybook = OffensivePlaybook.AIR_RAID,
            defensivePlaybook = DefensivePlaybook.FOUR_THREE,
            averageResponseTime = 0.0,
            delayOfGameWarningOptOut = false,
            scoutingReportHiddenColumns = null,
        )

    @BeforeEach
    fun setup() {
        userService =
            UserService(
                userRepository,
                encryptionUtils,
                userDtoService,
                passwordEncoder,
                usernameHistoryService,
                coachStintService,
                userCredentialRepository,
                userPreferencesRepository,
            )
        every { userRepository.getById(1L) } returns ownerUser
        every { userRepository.save(any()) } returns ownerUser
        every { userCredentialRepository.save(any()) } answers { firstArg() }
        every { userPreferencesRepository.save(any()) } answers { firstArg() }
        every { userDtoService.convertToUserDTO(any()) } returns ownerUserDTO
        every { usernameHistoryService.recordUsernameChange(any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun authenticateAs(
        userId: Long,
        role: String = "USER",
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId.toString(), null, listOf(SimpleGrantedAuthority("ROLE_$role")))
    }

    @Test
    fun `updateUsername succeeds for the owning user`() {
        authenticateAs(1L)
        userService.updateUsername(1L, "newname")
    }

    @Test
    fun `updateUsername succeeds for an admin acting on another user`() {
        authenticateAs(999L, role = "ADMIN")
        userService.updateUsername(1L, "newname")
    }

    @Test
    fun `updateUsername rejects a non-owner, non-admin caller`() {
        authenticateAs(2L)
        assertThrows(UserForbiddenException::class.java) {
            userService.updateUsername(1L, "newname")
        }
    }

    @Test
    fun `updateEmail rejects a non-owner, non-admin caller`() {
        authenticateAs(2L)
        assertThrows(UserForbiddenException::class.java) {
            userService.updateEmail(1L, "new@example.com")
        }
    }

    @Test
    fun `changePassword succeeds for the owning user with a correct current password`() {
        authenticateAs(1L)
        every { passwordEncoder.matches("oldpass", "hashed-password") } returns true
        userService.changePassword(1L, "oldpass", "newpass")
    }

    @Test
    fun `changePassword allows an admin to reset another user's password without the current password`() {
        authenticateAs(999L, role = "ADMIN")
        userService.changePassword(1L, null, "newpass")
    }

    @Test
    fun `changePassword rejects a non-owner, non-admin caller`() {
        authenticateAs(2L)
        assertThrows(UserForbiddenException::class.java) {
            userService.changePassword(1L, "oldpass", "newpass")
        }
    }

    @Test
    fun `updateUserAsRequester rejects a non-owner, non-admin caller`() {
        authenticateAs(2L)
        assertThrows(UserForbiddenException::class.java) {
            userService.updateUserAsRequester(
                UpdateUserRequest(
                    id = ownerUserDTO.id,
                    username = ownerUserDTO.username,
                    coachName = ownerUserDTO.coachName,
                    discordTag = ownerUserDTO.discordTag,
                    discordId = ownerUserDTO.discordId,
                    position = ownerUserDTO.position,
                    role = ownerUserDTO.role,
                    team = ownerUserDTO.team,
                    wins = ownerUserDTO.wins,
                    losses = ownerUserDTO.losses,
                    conferenceWins = ownerUserDTO.conferenceWins,
                    conferenceLosses = ownerUserDTO.conferenceLosses,
                    conferenceChampionshipWins = ownerUserDTO.conferenceChampionshipWins,
                    conferenceChampionshipLosses = ownerUserDTO.conferenceChampionshipLosses,
                    bowlWins = ownerUserDTO.bowlWins,
                    bowlLosses = ownerUserDTO.bowlLosses,
                    playoffWins = ownerUserDTO.playoffWins,
                    playoffLosses = ownerUserDTO.playoffLosses,
                    nationalChampionshipWins = ownerUserDTO.nationalChampionshipWins,
                    nationalChampionshipLosses = ownerUserDTO.nationalChampionshipLosses,
                    offensivePlaybook = ownerUserDTO.offensivePlaybook,
                    defensivePlaybook = ownerUserDTO.defensivePlaybook,
                ),
            )
        }
    }
}
