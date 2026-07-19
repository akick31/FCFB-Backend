package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.UserValidationRequest
import com.fcfb.arceus.dto.response.UserDTO
import com.fcfb.arceus.dto.response.UserValidationResponse
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.service.fcfb.UserService
import com.fcfb.arceus.util.GlobalExceptionHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class UserControllerTest {
    private lateinit var mockMvc: MockMvc
    private val userService: UserService = mockk()
    private lateinit var userController: UserController

    private val sampleUser =
        UserDTO(
            id = 1L,
            username = "testuser",
            coachName = "Test Coach",
            discordTag = "test#1234",
            discordId = "123456789",
            position = CoachPosition.HEAD_COACH,
            role = UserRole.USER,
            team = "Test Team",
            delayOfGameInstances = 2,
            wins = 10,
            losses = 5,
            winPercentage = 0.67,
            conferenceWins = 6,
            conferenceLosses = 2,
            conferenceChampionshipWins = 1,
            conferenceChampionshipLosses = 0,
            bowlWins = 1,
            bowlLosses = 0,
            playoffWins = 2,
            playoffLosses = 1,
            nationalChampionshipWins = 0,
            nationalChampionshipLosses = 1,
            offensivePlaybook = OffensivePlaybook.AIR_RAID,
            defensivePlaybook = DefensivePlaybook.FOUR_THREE,
            averageResponseTime = 15.5,
            delayOfGameWarningOptOut = false,
        )

    @BeforeEach
    fun setup() {
        userController = UserController(userService)
        mockMvc =
            MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
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
    fun `getUserById returns user`() {
        every { userService.getUserDTOById(1L) } returns sampleUser

        mockMvc.perform(get("/api/v1/arceus/user/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(sampleUser.id))
            .andExpect(jsonPath("$.username").value(sampleUser.username))
            .andExpect(jsonPath("$.discordId").value(sampleUser.discordId))
    }

    @Test
    fun `getUserByDiscordId returns user`() {
        every { userService.getUserDTOByDiscordId("123456789") } returns sampleUser

        mockMvc.perform(get("/api/v1/arceus/user/discord").param("discordId", "123456789"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.discordId").value("123456789"))
    }

    @Test
    fun `getUserByTeam returns user`() {
        every { userService.getUserByTeam("Test Team") } returns sampleUser

        mockMvc.perform(get("/api/v1/arceus/user/team").param("team", "Test Team"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.team").value("Test Team"))
    }

    @Test
    fun `getAllUsers returns all users`() {
        val users =
            listOf(
                sampleUser,
                sampleUser.copy(
                    id = 2L,
                    username = "user2",
                    team = "Another Team",
                    offensivePlaybook = OffensivePlaybook.PRO,
                    defensivePlaybook = DefensivePlaybook.THREE_FOUR,
                ),
            )
        every { userService.getAllUsers() } returns users

        mockMvc.perform(get("/api/v1/arceus/user"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].offensivePlaybook").value("PRO"))
    }

    @Test
    fun `getFreeAgents returns list`() {
        val freeAgents = listOf(sampleUser)
        every { userService.getFreeAgents() } returns freeAgents

        mockMvc.perform(get("/api/v1/arceus/user/free_agents"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(sampleUser.id))
    }

    @Test
    fun `getUserDTOByName returns user`() {
        every { userService.getUserDTOByName("testuser") } returns sampleUser

        mockMvc.perform(get("/api/v1/arceus/user/name").param("name", "testuser"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("testuser"))
    }

    @Test
    fun `updateUserEmail updates own email`() {
        authenticateAs(1L)
        every { userService.updateEmail(1L, "newemail@example.com") } returns sampleUser

        mockMvc.perform(
            put("/api/v1/arceus/user/update/email")
                .param("id", "1")
                .param("newEmail", "newemail@example.com"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `updateUserEmail allows admin to update another user's email`() {
        authenticateAs(999L, role = "ADMIN")
        every { userService.updateEmail(1L, "newemail@example.com") } returns sampleUser

        mockMvc.perform(
            put("/api/v1/arceus/user/update/email")
                .param("id", "1")
                .param("newEmail", "newemail@example.com"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `updateUserEmail rejects a non-owner, non-admin caller`() {
        authenticateAs(2L)

        mockMvc.perform(
            put("/api/v1/arceus/user/update/email")
                .param("id", "1")
                .param("newEmail", "newemail@example.com"),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `updateUserRole updates user`() {
        every { userService.updateUser(any()) } returns sampleUser

        val jsonBody =
            """
            {
              "id": 1,
              "username": "testuser",
              "coachName": "Test Coach",
              "discordTag": "test#1234",
              "discordId": "123456789",
              "position": "HEAD_COACH",
              "role": "USER",
              "team": "Test Team",
              "wins": 10,
              "losses": 5,
              "conferenceWins": 6,
              "conferenceLosses": 2,
              "conferenceChampionshipWins": 1,
              "conferenceChampionshipLosses": 0,
              "bowlWins": 1,
              "bowlLosses": 0,
              "playoffWins": 2,
              "playoffLosses": 1,
              "nationalChampionshipWins": 0,
              "nationalChampionshipLosses": 1,
              "offensivePlaybook": "AIR_RAID",
              "defensivePlaybook": "FOUR_THREE"
            }
            """.trimIndent()

        mockMvc.perform(
            put("/api/v1/arceus/user/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("testuser"))
    }

    @Test
    fun `encryptEmails returns success`() {
        every { userService.hashEmails() } returns Unit

        mockMvc.perform(post("/api/v1/arceus/user/hash_emails"))
            .andExpect(status().isOk)
    }

    @Test
    fun `validateUser returns success`() {
        val validationRequest =
            UserValidationRequest(
                discordId = "1234",
                discordTag = "discordTag",
                username = "testUser",
                email = "testemail@test.com",
            )
        every { userService.validateUser(validationRequest) } returns
            UserValidationResponse(
                discordIdExists = true,
                discordTagExists = true,
                usernameExists = true,
                emailExists = true,
            )

        val jsonBody =
            """
            {
              "discordId": "1234",
              "discordTag": "discordTag",
              "username": "testUser",
              "email": "testemail@test.com"
            }
            """.trimIndent()

        mockMvc.perform(
            post("/api/v1/arceus/user/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `deleteTeam deletes user`() {
        every { userService.deleteUser(1L) } returns OK

        mockMvc.perform(delete("/api/v1/arceus/user/1"))
            .andExpect(status().isOk)
    }

    @Test
    fun `getUserById handles error`() {
        every { userService.getUserDTOById(1L) } throws RuntimeException("User not found")

        mockMvc.perform(get("/api/v1/arceus/user/1"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("User not found"))
    }
}
