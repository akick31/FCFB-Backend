package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.UpdateUserRequest
import com.fcfb.arceus.dto.request.UserValidationRequest
import com.fcfb.arceus.dto.response.ApiKeyResponse
import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.service.fcfb.UserService
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
    @GetMapping("{userId:[0-9]+}")
    fun getUserById(
        @PathVariable userId: Long,
    ) = userService.getUserDTOById(userId)

    @GetMapping("/discord")
    fun getUserDTOByDiscordId(
        @RequestParam discordId: String,
    ) = userService.getUserDTOByDiscordId(discordId)

    @GetMapping("/team")
    fun getUserByTeam(
        @RequestParam team: String,
    ) = userService.getUserByTeam(team)

    @GetMapping("")
    fun getAllUsers() = userService.getAllUsers()

    @GetMapping("/free_agents")
    fun getFreeAgents() = userService.getFreeAgents()

    @GetMapping("/name")
    fun getUserDTOByName(
        @RequestParam name: String,
    ) = userService.getUserDTOByName(name)

    @PutMapping("/update/email")
    fun updateUserEmail(
        @RequestParam id: Long,
        @RequestParam newEmail: String,
    ): UserDTO = userService.updateEmail(id, newEmail)

    @PutMapping("/update/username")
    fun updateUsername(
        @RequestParam id: Long,
        @RequestParam newUsername: String,
    ): UserDTO = userService.updateUsername(id, newUsername)

    @PutMapping("/update/password")
    fun updateUserPassword(
        @RequestParam id: Long,
        @RequestParam(required = false) currentPassword: String?,
        @RequestParam newPassword: String,
    ): UserDTO = userService.changePassword(id, currentPassword, newPassword)

    @PutMapping("/update")
    fun updateUserRole(
        @RequestBody request: UpdateUserRequest,
    ): UserDTO = userService.updateUserAsRequester(request.toUserDTO())

    @PostMapping("/hash_emails")
    fun encryptEmails() = userService.hashEmails()

    @PostMapping("/validate")
    fun validateUser(
        @RequestBody userValidationRequest: UserValidationRequest,
    ) = userService.validateUser(userValidationRequest)

    @PostMapping("/api-key")
    fun generateApiKey(): ApiKeyResponse = userService.generateApiKeyForCurrentUser()

    @PostMapping("/api-key/revoke")
    fun revokeApiKey() = userService.revokeApiKeyForCurrentUser()

    @PostMapping("/{userId:[0-9]+}/api-key")
    fun generateApiKeyForUser(
        @PathVariable userId: Long,
    ): ApiKeyResponse = userService.generateApiKeyForUser(userId)

    @DeleteMapping("{teamId:[0-9]+}")
    fun deleteTeam(
        @PathVariable teamId: Long,
    ) = userService.deleteUser(teamId)
}
