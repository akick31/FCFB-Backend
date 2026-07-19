package com.fcfb.arceus.service.auth

import com.fcfb.arceus.dto.response.LoginResponse
import com.fcfb.arceus.dto.standard.SignupInfo
import com.fcfb.arceus.model.NewSignup
import com.fcfb.arceus.service.discord.DiscordService
import com.fcfb.arceus.service.email.EmailService
import com.fcfb.arceus.service.fcfb.NewSignupService
import com.fcfb.arceus.service.fcfb.UserService
import com.fcfb.arceus.util.Logger
import com.fcfb.arceus.util.UserUnauthorizedException
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class AuthService(
    private val discordService: DiscordService,
    private val emailService: EmailService,
    private val userService: UserService,
    private val newSignupService: NewSignupService,
    private val sessionService: SessionService,
    private val passwordEncoder: PasswordEncoder,
) {
    fun createNewSignup(newSignup: NewSignup): NewSignup {
        try {
            val signup = newSignupService.createNewSignup(newSignup)
            emailService.sendVerificationEmail(signup.email, signup.id, signup.verificationToken)
            val signupInfo =
                SignupInfo(
                    signup.discordTag,
                    signup.discordId ?: "",
                    signup.teamChoiceOne,
                    signup.teamChoiceTwo,
                    signup.teamChoiceThree,
                )
            discordService.sendRegistrationNotice(signupInfo)
            Logger.info("User ${signup.username} registered successfully. Verification email sent.")
            return signup
        } catch (e: Exception) {
            Logger.error("Error creating new sign up: ", e.message)
            throw e
        }
    }

    fun login(
        usernameOrEmail: String,
        password: String,
    ): LoginResponse {
        val user = userService.getUserByUsernameOrEmail(usernameOrEmail)
        if (!passwordEncoder.matches(password, user.password)) {
            throw UserUnauthorizedException()
        }
        val token = sessionService.generateToken(user.id, user.role)
        return LoginResponse(token, user.id, user.role)
    }

    fun logout(token: String): String {
        sessionService.blacklistUserSession(token)
        return "User logged out successfully"
    }

    fun verifyEmail(token: String): Boolean {
        val newSignup = newSignupService.getByVerificationToken(token) ?: return false
        if (newSignup.verificationTokenExpiration?.isBefore(LocalDateTime.now()) == true) {
            return false
        }
        return newSignupService.approveNewSignup(newSignup)
    }

    fun resetVerificationToken(id: Long): NewSignup {
        val newSignup =
            newSignupService.getNewSignupById(id)
                ?: throw IllegalArgumentException("NewSignup with id $id not found")
        val verificationToken = UUID.randomUUID().toString()
        newSignup.verificationToken = verificationToken
        newSignup.verificationTokenExpiration = LocalDateTime.now().plusHours(24)
        newSignupService.saveNewSignup(newSignup)
        emailService.sendVerificationEmail(newSignup.email, newSignup.id, verificationToken)
        return newSignup
    }

    fun forgotPassword(email: String): ResponseEntity<String> {
        val user = userService.updateResetToken(email)

        emailService.sendPasswordResetEmail(user.email, user.id, user.resetToken ?: "")
        return ResponseEntity.ok("Reset email sent")
    }

    fun resetPassword(
        token: String,
        newPassword: String,
    ): ResponseEntity<String> {
        val user =
            userService.getUserByResetToken(token)
                ?: return ResponseEntity.badRequest().body("Invalid or expired token")

        if (user.resetTokenExpiration?.let { LocalDateTime.parse(it).isBefore(LocalDateTime.now()) } ?: false) {
            return ResponseEntity.badRequest().body("Invalid or expired token")
        }

        userService.updateUserPassword(user.id, newPassword)
        emailService.sendPasswordResetConfirmation(user.email, user.username)
        return ResponseEntity.ok("Password updated successfully")
    }
}
