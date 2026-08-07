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
import org.springframework.web.bind.annotation.PathVariable
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
    @Operation(summary = "Get a user by their ID")
    @GetMapping("{userId:[0-9]+}")
    fun getUserById(
        @PathVariable userId: Long,
    ) = userService.getUserDTOById(userId)

    @Operation(summary = "Get a user by their Discord ID")
    @GetMapping("/discord")
    fun getUserDTOByDiscordId(
        @RequestParam discordId: String,
    ) = userService.getUserDTOByDiscordId(discordId)

    @Operation(summary = "Get a user by their team")
    @GetMapping("/team")
    fun getUserByTeam(
        @RequestParam team: String,
    ) = userService.getUserByTeam(team)

    @Operation(summary = "List all users")
    @GetMapping("")
    fun getAllUsers() = userService.getAllUsers()

    @Operation(summary = "List all free agent users")
    @GetMapping("/free_agents")
    fun getFreeAgents() = userService.getFreeAgents()

    @Operation(summary = "Get a user by their name")
    @GetMapping("/name")
    fun getUserDTOByName(
        @RequestParam name: String,
    ) = userService.getUserDTOByName(name)

    @Operation(summary = "Update a user's email address")
    @PutMapping("/update/email")
    fun updateUserEmail(
        @RequestParam id: Long,
        @RequestParam newEmail: String,
    ): UserDTO = userService.updateEmail(id, newEmail)

    @Operation(summary = "Update a user's username")
    @PutMapping("/update/username")
    fun updateUsername(
        @RequestParam id: Long,
        @RequestParam newUsername: String,
    ): UserDTO = userService.updateUsername(id, newUsername)

    @Operation(summary = "Update a user's password")
    @PutMapping("/update/password")
    fun updateUserPassword(
        @RequestParam id: Long,
        @RequestParam(required = false) currentPassword: String?,
        @RequestParam newPassword: String,
    ): UserDTO = userService.changePassword(id, currentPassword, newPassword)

    @Operation(summary = "Update a user's role")
    @PutMapping("/update")
    fun updateUserRole(
        @RequestBody request: UpdateUserRequest,
    ): UserDTO = userService.updateUserAsRequester(request.toUserDTO())

    @Operation(summary = "Hash all user email addresses")
    @PostMapping("/hash_emails")
    fun encryptEmails() = userService.hashEmails()

    @Operation(summary = "Validate a user's credentials")
    @PostMapping("/validate")
    fun validateUser(
        @RequestBody userValidationRequest: UserValidationRequest,
    ) = userService.validateUser(userValidationRequest)

    @Operation(summary = "Generate an API key for the current user")
    @PostMapping("/api-key")
    fun generateApiKey(): ApiKeyResponse = userService.generateApiKeyForCurrentUser()

    @Operation(summary = "Revoke the current user's API key")
    @PostMapping("/api-key/revoke")
    fun revokeApiKey() = userService.revokeApiKeyForCurrentUser()

    @Operation(summary = "Generate an API key for a specific user")
    @PostMapping("/{userId:[0-9]+}/api-key")
    fun generateApiKeyForUser(
        @PathVariable userId: Long,
    ): ApiKeyResponse = userService.generateApiKeyForUser(userId)

    @Operation(summary = "Delete a user")
    @DeleteMapping("{teamId:[0-9]+}")
    fun deleteTeam(
        @PathVariable teamId: Long,
    ) = userService.deleteUser(teamId)
}
