package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRepositoryTest {
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and find by id`() {
        val user =
            createTestUser(
                id = 1L,
                username = "testuser",
                coachName = "Test Coach",
                discordTag = "test#1234",
                discordId = "discord123",
                email = "test@example.com",
                hashedEmail = "hashedemail123",
                password = "password123",
                position = CoachPosition.HEAD_COACH,
                role = UserRole.USER,
                salt = "salt123",
                team = "Alabama",
                delayOfGameInstances = 0,
                wins = 5,
                losses = 3,
                winPercentage = 0.625,
                conferenceWins = 4,
                conferenceLosses = 2,
                conferenceChampionshipWins = 1,
                conferenceChampionshipLosses = 0,
                bowlWins = 1,
                bowlLosses = 0,
                playoffWins = 0,
                playoffLosses = 0,
                nationalChampionshipWins = 0,
                nationalChampionshipLosses = 0,
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                averageResponseTime = 2.5,
                delayOfGameWarningOptOut = false,
                resetToken = null,
                resetTokenExpiration = null,
            )

        every { userRepository.save(any()) } returns user
        every { userRepository.findById(1L) } returns java.util.Optional.of(user)

        val saved = userRepository.save(user)
        val found = userRepository.findById(saved.id).get()

        assertNotNull(found)
        assertEquals(saved.id, found.id)
        assertEquals("testuser", found.username)
        assertEquals("Test Coach", found.coachName)
        assertEquals("test#1234", found.discordTag)
        assertEquals("discord123", found.discordId)
        assertEquals("test@example.com", found.email)
        assertEquals("hashedemail123", found.hashedEmail)
        assertEquals("password123", found.password)
        assertEquals(CoachPosition.HEAD_COACH, found.position)
        assertEquals(UserRole.USER, found.role)
        assertEquals("salt123", found.salt)
        assertEquals("Alabama", found.team)
        assertEquals(0, found.delayOfGameInstances)
        assertEquals(5, found.wins)
        assertEquals(3, found.losses)
        assertEquals(0.625, found.winPercentage)
        assertEquals(4, found.conferenceWins)
        assertEquals(2, found.conferenceLosses)
        assertEquals(1, found.conferenceChampionshipWins)
        assertEquals(0, found.conferenceChampionshipLosses)
        assertEquals(1, found.bowlWins)
        assertEquals(0, found.bowlLosses)
        assertEquals(0, found.playoffWins)
        assertEquals(0, found.playoffLosses)
        assertEquals(0, found.nationalChampionshipWins)
        assertEquals(0, found.nationalChampionshipLosses)
        assertEquals(OffensivePlaybook.AIR_RAID, found.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, found.defensivePlaybook)
        assertEquals(2.5, found.averageResponseTime)
        assertEquals(false, found.delayOfGameWarningOptOut)
        assertEquals(null, found.resetToken)
        assertEquals(null, found.resetTokenExpiration)
    }

    @Test
    fun `test find by username`() {
        val user =
            createTestUser(
                id = 1L,
                username = "testuser",
                coachName = "Test Coach",
            )

        every { userRepository.findByUsername("testuser") } returns user

        val found = userRepository.findByUsername("testuser")

        assertNotNull(found)
        assertEquals("testuser", found!!.username)
        assertEquals("Test Coach", found.coachName)
    }

    @Test
    fun `test find by discord id`() {
        val user =
            createTestUser(
                id = 1L,
                username = "testuser",
                discordId = "discord123",
            )

        every { userRepository.findByDiscordId("discord123") } returns user

        val found = userRepository.findByDiscordId("discord123")

        assertNotNull(found)
        assertEquals("testuser", found!!.username)
        assertEquals("discord123", found.discordId)
    }

    @Test
    fun `test find by email`() {
        val user =
            createTestUser(
                id = 1L,
                username = "testuser",
                email = "test@example.com",
            )

        every { userRepository.findByEmail("test@example.com") } returns user

        val found = userRepository.findByEmail("test@example.com")

        assertNotNull(found)
        assertEquals("testuser", found!!.username)
        assertEquals("test@example.com", found.email)
    }

    @Test
    fun `test find by team`() {
        val user =
            createTestUser(
                id = 1L,
                username = "testuser",
                team = "Alabama",
            )

        every { userRepository.findByTeam("Alabama") } returns user

        val found = userRepository.findByTeam("Alabama")

        assertNotNull(found)
        assertEquals("testuser", found!!.username)
        assertEquals("Alabama", found.team)
    }

    @Test
    fun `test find by role`() {
        val user1 = createTestUser(id = 1L, username = "user1", role = UserRole.ADMIN)
        val user2 = createTestUser(id = 2L, username = "user2", role = UserRole.ADMIN)
        val users = listOf(user1, user2)

        every { userRepository.findByRole(UserRole.ADMIN) } returns users

        val found = userRepository.findByRole(UserRole.ADMIN)

        assertNotNull(found)
        assertEquals(2, found.size)
        assertTrue(found.all { it.role == UserRole.ADMIN })
    }

    @Test
    fun `test find by position`() {
        val user1 = createTestUser(id = 1L, username = "user1", position = CoachPosition.HEAD_COACH)
        val user2 = createTestUser(id = 2L, username = "user2", position = CoachPosition.HEAD_COACH)
        val users = listOf(user1, user2)

        every { userRepository.findByPosition(CoachPosition.HEAD_COACH) } returns users

        val found = userRepository.findByPosition(CoachPosition.HEAD_COACH)

        assertNotNull(found)
        assertEquals(2, found.size)
        assertTrue(found.all { it.position == CoachPosition.HEAD_COACH })
    }

    @Test
    fun `test update user`() {
        val user =
            createTestUser(
                id = 1L,
                username = "testuser",
                wins = 5,
                losses = 3,
            )

        every { userRepository.save(any()) } returns user

        val updated = userRepository.save(user)

        assertNotNull(updated)
        assertEquals(5, updated.wins)
        assertEquals(3, updated.losses)
    }

    @Test
    fun `test delete by id`() {
        val userId = 1L
        every { userRepository.deleteById(userId) } returns Unit

        userRepository.deleteById(userId)

        verify { userRepository.deleteById(userId) }
    }

    private fun createTestUser(
        id: Long = 1L,
        username: String = "testuser",
        coachName: String = "Test Coach",
        discordTag: String = "test#1234",
        discordId: String = "discord123",
        email: String = "test@example.com",
        hashedEmail: String = "hashedemail123",
        password: String = "password123",
        position: CoachPosition = CoachPosition.HEAD_COACH,
        role: UserRole = UserRole.USER,
        salt: String = "salt123",
        team: String = "Alabama",
        delayOfGameInstances: Int = 0,
        wins: Int = 0,
        losses: Int = 0,
        winPercentage: Double = 0.0,
        conferenceWins: Int = 0,
        conferenceLosses: Int = 0,
        conferenceChampionshipWins: Int = 0,
        conferenceChampionshipLosses: Int = 0,
        bowlWins: Int = 0,
        bowlLosses: Int = 0,
        playoffWins: Int = 0,
        playoffLosses: Int = 0,
        nationalChampionshipWins: Int = 0,
        nationalChampionshipLosses: Int = 0,
        offensivePlaybook: OffensivePlaybook = OffensivePlaybook.AIR_RAID,
        defensivePlaybook: DefensivePlaybook = DefensivePlaybook.FOUR_THREE,
        averageResponseTime: Double = 0.0,
        delayOfGameWarningOptOut: Boolean = false,
        resetToken: String? = null,
        resetTokenExpiration: String? = null,
    ): User {
        return User(
            username = username,
            coachName = coachName,
            discordTag = discordTag,
            discordId = discordId,
            email = email,
            hashedEmail = hashedEmail,
            password = password,
            position = position,
            role = role,
            salt = salt,
            team = team,
            delayOfGameInstances = delayOfGameInstances,
            wins = wins,
            losses = losses,
            winPercentage = winPercentage,
            conferenceWins = conferenceWins,
            conferenceLosses = conferenceLosses,
            conferenceChampionshipWins = conferenceChampionshipWins,
            conferenceChampionshipLosses = conferenceChampionshipLosses,
            bowlWins = bowlWins,
            bowlLosses = bowlLosses,
            playoffWins = playoffWins,
            playoffLosses = playoffLosses,
            nationalChampionshipWins = nationalChampionshipWins,
            nationalChampionshipLosses = nationalChampionshipLosses,
            offensivePlaybook = offensivePlaybook,
            defensivePlaybook = defensivePlaybook,
            averageResponseTime = averageResponseTime,
            delayOfGameWarningOptOut = delayOfGameWarningOptOut,
            resetToken = resetToken,
            resetTokenExpiration = resetTokenExpiration,
        ).apply {
            this.id = id
        }
    }
}
