package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.UserDTO
import com.fcfb.arceus.dto.UserValidationRequest
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
    @GetMapping("{userId}")
    fun getUserById(
        @PathVariable userId: Long,
    ) = userService.getUserById(userId)

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
    ) = userService.updateEmail(id, newEmail)

    @PutMapping("/update")
    fun updateUserRole(
        @RequestBody user: UserDTO,
    ) = userService.updateUser(user)

    @PostMapping("/hash_emails")
    fun encryptEmails() = userService.hashEmails()

    @PostMapping("/validate")
    fun validateUser(
        @RequestBody userValidationRequest: UserValidationRequest,
    ) = userService.validateUser(userValidationRequest)

    @DeleteMapping("{teamId}")
    fun deleteTeam(
        @PathVariable teamId: Long,
    ) = userService.deleteUser(teamId)
}
