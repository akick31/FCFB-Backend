package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.ForgotPasswordRequest
import com.fcfb.arceus.dto.LoginRequest
import com.fcfb.arceus.dto.ResetPasswordRequest
import com.fcfb.arceus.model.NewSignup
import com.fcfb.arceus.service.auth.AuthService
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
    @PostMapping("/register")
    fun registerUser(
        @RequestBody newSignup: NewSignup,
    ): ResponseEntity<NewSignup> = ResponseEntity.ok(authService.createNewSignup(newSignup))


    
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(authService.login(request.usernameOrEmail, request.password))

    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<String> {
        val token = authHeader.removePrefix("Bearer ").trim()
        return ResponseEntity.ok(authService.logout(token))
    }

    @GetMapping("/verify-email")
    fun verifyEmail(
        @RequestParam("token") token: String,
    ): ResponseEntity<Boolean> = ResponseEntity.ok(authService.verifyEmail(token))

    @PostMapping("/verification-email/resend")
    fun resetVerificationToken(
        @RequestParam("newSignupId") newSignupId: Long,
    ): ResponseEntity<NewSignup> = ResponseEntity.ok(authService.resetVerificationToken(newSignupId))

    @PostMapping("/forgot-password")
    fun forgotPassword(
        @RequestBody request: ForgotPasswordRequest,
    ): ResponseEntity<String> = authService.forgotPassword(request.email)

    @PostMapping("/reset-password")
    fun resetPassword(
        @RequestBody request: ResetPasswordRequest,
    ): ResponseEntity<String> = authService.resetPassword(request.token, request.newPassword)
}
