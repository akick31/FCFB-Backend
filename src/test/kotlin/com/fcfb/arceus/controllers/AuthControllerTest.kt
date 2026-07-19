package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.response.LoginResponse
import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.model.NewSignup
import com.fcfb.arceus.model.User
import com.fcfb.arceus.service.auth.AuthService
import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.email.EmailService
import com.fcfb.arceus.service.fcfb.NewSignupService
import com.fcfb.arceus.service.fcfb.UserService
import com.fcfb.arceus.util.UserUnauthorizedException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.UUID

class AuthControllerTest {
    private lateinit var authService: AuthService
    private val discordService: DiscordService = mockk()
    private val emailService: EmailService = mockk()
    private val userService: UserService = mockk()
    private val newSignupService: NewSignupService = mockk()
    private val sessionService: SessionService = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()

    @BeforeEach
    fun setup() {
        authService =
            AuthService(
                discordService,
                emailService,
                userService,
                newSignupService,
                sessionService,
                passwordEncoder,
            )
    }

    @Test
    fun `should create new signup successfully`() {
        val newSignup =
            NewSignup(
                username = "username",
                coachName = "coachName",
                discordTag = "discordTag",
                discordId = "discordId",
                email = "email@example.com",
                hashedEmail = "hashedEmail",
                password = "password",
                position = CoachPosition.HEAD_COACH,
                salt = "salt",
                teamChoiceOne = "team1",
                teamChoiceTwo = "team2",
                teamChoiceThree = "team3",
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                approved = false,
                verificationToken = UUID.randomUUID().toString(),
                verificationTokenExpiration = LocalDateTime.now().plusHours(24),
            )

        every { newSignupService.createNewSignup(newSignup) } returns newSignup
        every { emailService.sendVerificationEmail(newSignup.email, newSignup.id, newSignup.verificationToken) } just Runs
        every { discordService.sendRegistrationNotice(any()) } just Runs

        val result = authService.createNewSignup(newSignup)

        assertEquals(newSignup, result)
        verify { emailService.sendVerificationEmail(newSignup.email, newSignup.id, newSignup.verificationToken) }
        verify { discordService.sendRegistrationNotice(any()) }
    }

    @Test
    fun `should reset verification token successfully`() {
        val id = 1L
        val fixedToken = "fixed-verification-token"
        val newSignup =
            NewSignup(
                username = "username",
                coachName = "coachName",
                discordTag = "discordTag",
                discordId = "discordId",
                email = "email@example.com",
                hashedEmail = "hashedEmail",
                password = "password",
                position = CoachPosition.HEAD_COACH,
                salt = "salt",
                teamChoiceOne = "team1",
                teamChoiceTwo = "team2",
                teamChoiceThree = "team3",
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                approved = false,
                verificationToken = "verificationToken",
                verificationTokenExpiration = LocalDateTime.now().plusHours(24),
            )

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns fixedToken

        every { newSignupService.getNewSignupById(id) } returns newSignup
        every { newSignupService.saveNewSignup(newSignup) } returns newSignup
        every { emailService.sendVerificationEmail(newSignup.email, newSignup.id, fixedToken) } just Runs

        val result = authService.resetVerificationToken(id)

        assertEquals(fixedToken, result.verificationToken)
        verify { emailService.sendVerificationEmail(newSignup.email, newSignup.id, fixedToken) }
    }

    @Test
    fun `should login successfully`() {
        val usernameOrEmail = "username"
        val rawPassword = "password"
        val encodedPassword = "encodedPassword"
        val token = "abc123"

        val user =
            User(
                username = usernameOrEmail,
                coachName = "Coach X",
                discordTag = "User#1234",
                discordId = "123456",
                email = "user@example.com",
                hashedEmail = "hashedEmail",
                password = encodedPassword,
                position = CoachPosition.HEAD_COACH,
                role = UserRole.USER,
                salt = "somesalt",
                team = "FakeU",
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
                resetToken = null,
                resetTokenExpiration = null,
            ).apply { id = 1L }

        every { userService.getUserByUsernameOrEmail(usernameOrEmail) } returns user
        every { passwordEncoder.matches(rawPassword, encodedPassword) } returns true
        every { sessionService.generateToken(1L, UserRole.USER) } returns token

        val result = authService.login(usernameOrEmail, rawPassword)

        assertEquals(LoginResponse(token, 1L, UserRole.USER), result)
    }

    @Test
    fun `should throw exception when login fails due to incorrect password`() {
        val usernameOrEmail = "username"
        val password = "wrongPassword"
        val user =
            User(
                username = usernameOrEmail,
                coachName = "Coach X",
                discordTag = "User#1234",
                discordId = "123456",
                email = "user@example.com",
                hashedEmail = "hashedEmail",
                password = "encodedPassword",
                position = CoachPosition.HEAD_COACH,
                role = UserRole.USER,
                salt = "somesalt",
                team = "FakeU",
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
                resetToken = null,
                resetTokenExpiration = null,
            ).apply { id = 1L }

        every { userService.getUserByUsernameOrEmail(usernameOrEmail) } returns user
        every { passwordEncoder.matches(password, user.password) } returns false

        assertThrows<UserUnauthorizedException> {
            authService.login(usernameOrEmail, password)
        }
    }

    @Test
    fun `should logout successfully`() {
        val token = "abc123"

        every { sessionService.blacklistUserSession(token) } just Runs

        val result = authService.logout(token)

        assertEquals("User logged out successfully", result)
        verify { sessionService.blacklistUserSession(token) }
    }

    @Test
    fun `should verify email successfully`() {
        val token = "verificationToken"
        val newSignup = mockk<NewSignup>()

        every { newSignupService.getByVerificationToken(token) } returns newSignup
        every { newSignup.verificationTokenExpiration } returns LocalDateTime.now().plusHours(1)
        every { newSignupService.approveNewSignup(newSignup) } returns true

        val result = authService.verifyEmail(token)

        assertTrue(result)
    }

    @Test
    fun `should reject verification with an expired token`() {
        val token = "verificationToken"
        val newSignup = mockk<NewSignup>()

        every { newSignupService.getByVerificationToken(token) } returns newSignup
        every { newSignup.verificationTokenExpiration } returns LocalDateTime.now().minusHours(1)

        val result = authService.verifyEmail(token)

        assertFalse(result)
    }

    @Test
    fun `should send password reset email successfully`() {
        val email = "email@example.com"
        val user =
            User(
                username = "username",
                coachName = "Coach X",
                discordTag = "User#1234",
                discordId = "123456",
                email = "user@example.com",
                hashedEmail = "hashedEmail",
                password = "password",
                position = CoachPosition.HEAD_COACH,
                role = UserRole.USER,
                salt = "somesalt",
                team = "FakeU",
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
                resetToken = "resettoken",
                resetTokenExpiration = null,
            ).apply { id = 1L }

        every { userService.updateResetToken(email) } returns user
        every { emailService.sendPasswordResetEmail(user.email, user.id, user.resetToken!!) } just Runs

        val result = authService.forgotPassword(email)

        assertEquals(ResponseEntity.ok("Reset email sent"), result)
        verify { emailService.sendPasswordResetEmail(user.email, user.id, user.resetToken!!) }
    }

    @Test
    fun `should reset password successfully`() {
        val token = "resetToken"
        val newPassword = "newPassword"
        val userId = 1L

        val userEmail = "email@example.com"
        val userUsername = "username"
        val user =
            mockk<User> {
                every { id } returns userId
                every { email } returns userEmail
                every { username } returns userUsername
                every { resetToken } returns token
                every { resetTokenExpiration } returns LocalDateTime.now().plusDays(1).toString()
            }

        every { userService.getUserByResetToken(token) } returns user
        every { userService.updateUserPassword(userId, newPassword) } returns mockk<UserDTO>()
        every { emailService.sendPasswordResetConfirmation(userEmail, userUsername) } just Runs

        val result = authService.resetPassword(token, newPassword)

        assertEquals(ResponseEntity.ok("Password updated successfully"), result)
        verify { emailService.sendPasswordResetConfirmation(userEmail, userUsername) }
    }

    @Test
    fun `should return bad request when reset token is invalid or expired`() {
        val token = "invalidToken"
        val newPassword = "newPassword"

        val user =
            mockk<User> {
                every { resetToken } returns "validToken"
                every { resetTokenExpiration } returns LocalDateTime.now().minusDays(1).toString()
            }

        every { userService.getUserByResetToken(token) } returns user

        val result = authService.resetPassword(token, newPassword)

        assertEquals(ResponseEntity.badRequest().body("Invalid or expired token"), result)
    }
}
