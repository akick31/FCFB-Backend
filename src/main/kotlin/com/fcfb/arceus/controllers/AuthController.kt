package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.ForgotPasswordRequest
import com.fcfb.arceus.dto.request.LoginRequest
import com.fcfb.arceus.dto.request.ResetPasswordRequest
import com.fcfb.arceus.model.NewSignup
import com.fcfb.arceus.service.auth.AuthService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/auth")
class AuthController(
    private val authService: AuthService,
) {
    @Operation(summary = "Register a new signup")
    @PostMapping("/register")
    fun registerUser(
        @RequestBody newSignup: NewSignup,
    ): ResponseEntity<NewSignup> = ResponseEntity.ok(authService.createNewSignup(newSignup))

    @Operation(summary = "Log in a user and issue an auth token")
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(authService.login(request.usernameOrEmail, request.password))

    @Operation(summary = "Log out a user")
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<String> = ResponseEntity.ok(authService.logout(authHeader))

    @Operation(summary = "Verify a user's email address using a verification token")
    @GetMapping("/verify-email")
    fun verifyEmail(
        @RequestParam("token") token: String,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(authService.verifyEmail(token))

    @Operation(summary = "Resend the email verification token for a new signup")
    @PostMapping("/verification-email/resend")
    fun resetVerificationToken(
        @RequestParam("newSignupId") newSignupId: Long,
    ): ResponseEntity<NewSignup> = ResponseEntity.ok(authService.resetVerificationToken(newSignupId))

    @Operation(summary = "Send a password reset email to a user")
    @PostMapping("/forgot-password")
    fun forgotPassword(
        @RequestBody request: ForgotPasswordRequest,
    ): ResponseEntity<String> = authService.forgotPassword(request.email)

    @Operation(summary = "Reset a user's password using a reset token")
    @PostMapping("/reset-password")
    fun resetPassword(
        @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<String> = authService.resetPassword(request.token, request.newPassword)
}
