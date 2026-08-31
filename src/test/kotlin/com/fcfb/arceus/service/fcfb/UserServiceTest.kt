package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.service.log.UsernameHistoryService
import com.fcfb.arceus.util.DTOConverter
import com.fcfb.arceus.util.EncryptionUtils
import com.fcfb.arceus.util.UserForbiddenException
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
    private val dtoConverter: DTOConverter = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val usernameHistoryService: UsernameHistoryService = mockk()
    private lateinit var userService: UserService

    private val ownerUser =
        User().apply {
            id = 1L
            username = "owner"
            password = "hashed-password"
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
        )

    @BeforeEach
    fun setup() {
        userService = UserService(userRepository, encryptionUtils, dtoConverter, passwordEncoder, usernameHistoryService)
        every { userRepository.getById(1L) } returns ownerUser
        every { userRepository.save(any()) } returns ownerUser
        every { dtoConverter.convertToUserDTO(any()) } returns ownerUserDTO
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
            userService.updateUserAsRequester(ownerUserDTO)
        }
    }
}
