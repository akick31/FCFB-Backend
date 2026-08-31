package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.UpdateUserRequest
import com.fcfb.arceus.dto.request.UserValidationRequest
import com.fcfb.arceus.dto.response.ApiKeyResponse
import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.service.fcfb.UserService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/user")
class UserController(
    private var userService: UserService,
) {
    @Operation(summary = "Get user by ID")
    @GetMapping(params = ["userId"])
    fun getUserById(
        @RequestParam userId: Long,
    ) = userService.getUserDTOById(userId)

    @Operation(summary = "Get user by Discord ID")
    @GetMapping("/discord")
    fun getUserDTOByDiscordId(
        @RequestParam discordId: String,
    ) = userService.getUserDTOByDiscordId(discordId)

    @Operation(summary = "Get user by team")
    @GetMapping("/team")
    fun getUserByTeam(
        @RequestParam team: String,
    ) = userService.getUserByTeam(team)

    @Operation(summary = "List users")
    @GetMapping("/all")
    fun getAllUsers() = userService.getAllUsers()

    @Operation(summary = "List free agents")
    @GetMapping("/free_agents")
    fun getFreeAgents() = userService.getFreeAgents()

    @Operation(summary = "Get user by name")
    @GetMapping("/name")
    fun getUserDTOByName(
        @RequestParam name: String,
    ) = userService.getUserDTOByName(name)

    @Operation(summary = "Update email address")
    @PutMapping("/update/email")
    fun updateUserEmail(
        @RequestParam id: Long,
        @RequestParam newEmail: String,
    ): UserDTO = userService.updateEmail(id, newEmail)

    @Operation(summary = "Update username")
    @PutMapping("/update/username")
    fun updateUsername(
        @RequestParam id: Long,
        @RequestParam newUsername: String,
    ): UserDTO = userService.updateUsername(id, newUsername)

    @Operation(summary = "Get a user's past usernames")
    @GetMapping("/username-history")
    fun getUsernameHistory(
        @RequestParam userId: Long,
    ): List<String> = userService.getUsernameHistory(userId)

    @Operation(summary = "Record a past username for a user (backfill for renames that predate history tracking)")
    @PostMapping("/username-history")
    fun addHistoricalUsername(
        @RequestParam userId: Long,
        @RequestParam username: String,
    ) = userService.addHistoricalUsername(userId, username)

    @Operation(summary = "Update password")
    @PutMapping("/update/password")
    fun updateUserPassword(
        @RequestParam id: Long,
        @RequestParam(required = false) currentPassword: String?,
        @RequestParam newPassword: String,
    ): UserDTO = userService.changePassword(id, currentPassword, newPassword)

    @Operation(summary = "Update user role")
    @PutMapping("/update")
    fun updateUserRole(
        @RequestBody request: UpdateUserRequest,
    ): UserDTO = userService.updateUserAsRequester(request.toUserDTO())

    @Operation(summary = "Hash user emails")
    @PostMapping("/hash_emails")
    fun encryptEmails() = userService.hashEmails()

    @Operation(summary = "Validate credentials")
    @PostMapping("/validate")
    fun validateUser(
        @RequestBody userValidationRequest: UserValidationRequest,
    ) = userService.validateUser(userValidationRequest)

    @Operation(summary = "Generate API key")
    @PostMapping("/api-key")
    fun generateApiKey(): ApiKeyResponse = userService.generateApiKeyForCurrentUser()

    @Operation(summary = "Revoke API key")
    @PostMapping("/api-key/revoke")
    fun revokeApiKey() = userService.revokeApiKeyForCurrentUser()

    @Operation(summary = "Generate API key for user")
    @PostMapping("/api-key/for-user")
    fun generateApiKeyForUser(
        @RequestParam userId: Long,
    ): ApiKeyResponse = userService.generateApiKeyForUser(userId)

    @Operation(summary = "Delete user")
    @DeleteMapping("")
    fun deleteUser(
        @RequestParam userId: Long,
    ) = userService.deleteUser(userId)
}
