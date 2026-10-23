package com.fcfb.arceus.controllers.auth

import com.fcfb.arceus.controllers.ApiConstants
import com.fcfb.arceus.dto.request.ForgotPasswordRequest
import com.fcfb.arceus.dto.request.LoginRequest
import com.fcfb.arceus.dto.request.ResetPasswordRequest
import com.fcfb.arceus.dto.response.LoginResponse
import com.fcfb.arceus.model.account.NewSignup
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
    @Operation(summary = "Register signup")
    @PostMapping("/register")
    fun registerUser(
        @RequestBody newSignup: NewSignup,
    ): NewSignup = authService.createNewSignup(newSignup)

    @Operation(summary = "Log in user")
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): LoginResponse = authService.login(request.usernameOrEmail, request.password)

    @Operation(summary = "Refresh the caller's session token")
    @PostMapping("/refresh")
    fun refreshSession(
        @RequestHeader("Authorization") authHeader: String,
    ): LoginResponse = authService.refreshSession(authHeader)

    @Operation(summary = "Log out user")
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String,
    ): String = authService.logout(authHeader)

    @Operation(summary = "Verify email address")
    @GetMapping("/verify-email")
    fun verifyEmail(
        @RequestParam("token") token: String,
    ): Boolean = authService.verifyEmail(token)

    @Operation(summary = "Resend verification email")
    @PostMapping("/verification-email/resend")
    fun resetVerificationToken(
        @RequestParam("newSignupId") newSignupId: Long,
    ): NewSignup = authService.resetVerificationToken(newSignupId)

    @Operation(summary = "Send password reset email")
    @PostMapping("/forgot-password")
    fun forgotPassword(
        @RequestBody request: ForgotPasswordRequest,
    ): ResponseEntity<String> = authService.forgotPassword(request.email)

    @Operation(summary = "Reset password")
    @PostMapping("/reset-password")
    fun resetPassword(
        @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<String> = authService.resetPassword(request.token, request.newPassword)
}
