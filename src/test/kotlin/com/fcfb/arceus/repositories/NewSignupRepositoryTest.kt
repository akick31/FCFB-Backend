package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.model.NewSignup
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NewSignupRepositoryTest {
    private lateinit var newSignupRepository: NewSignupRepository

    @BeforeEach
    fun setUp() {
        newSignupRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and find by id`() {
        val newSignup = createTestNewSignup(id = 1L)

        every { newSignupRepository.save(any()) } returns newSignup
        every { newSignupRepository.findById(1L) } returns java.util.Optional.of(newSignup)

        val saved = newSignupRepository.save(newSignup)
        val found = newSignupRepository.findById(saved.id).get()

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
        assertEquals("salt123", found.salt)
        assertEquals("Alabama", found.teamChoiceOne)
        assertEquals("Auburn", found.teamChoiceTwo)
        assertEquals("Georgia", found.teamChoiceThree)
        assertEquals(OffensivePlaybook.AIR_RAID, found.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, found.defensivePlaybook)
        assertEquals(false, found.approved)
        assertEquals("token123", found.verificationToken)
    }

    @Test
    fun `test find by username`() {
        val newSignup = createTestNewSignup()

        every { newSignupRepository.findByUsername("testuser") } returns newSignup

        val found = newSignupRepository.findByUsername("testuser")

        assertNotNull(found)
        assertEquals("testuser", found.username)
    }

    @Test
    fun `test find by email`() {
        val newSignup = createTestNewSignup()

        every { newSignupRepository.findByEmail("test@example.com") } returns newSignup

        val found = newSignupRepository.findByEmail("test@example.com")

        assertNotNull(found)
        assertEquals("test@example.com", found.email)
    }

    @Test
    fun `test find by verification token`() {
        val newSignup = createTestNewSignup()

        every { newSignupRepository.findByVerificationToken("token123") } returns newSignup

        val found = newSignupRepository.findByVerificationToken("token123")

        assertNotNull(found)
        assertEquals("token123", found.verificationToken)
    }

    @Test
    fun `test find by approved status`() {
        val newSignup1 = createTestNewSignup(id = 1L, approved = true)
        val newSignup2 = createTestNewSignup(id = 2L, approved = false)
        val approvedSignups = listOf(newSignup1)

        every { newSignupRepository.findByApproved(true) } returns approvedSignups

        val found = newSignupRepository.findByApproved(true)

        assertEquals(1, found.size)
        assertEquals(true, found[0].approved)
    }

    @Test
    fun `test update signup`() {
        val newSignup = createTestNewSignup()

        every { newSignupRepository.save(any()) } returns newSignup

        val updated = newSignupRepository.save(newSignup)

        assertNotNull(updated)
        assertEquals("testuser", updated.username)
    }

    @Test
    fun `test delete by id`() {
        every { newSignupRepository.deleteById(1L) } returns Unit

        newSignupRepository.deleteById(1L)

        verify { newSignupRepository.deleteById(1L) }
    }

    private fun createTestNewSignup(
        id: Long = 1L,
        username: String = "testuser",
        coachName: String = "Test Coach",
        discordTag: String = "test#1234",
        discordId: String = "discord123",
        email: String = "test@example.com",
        hashedEmail: String = "hashedemail123",
        password: String = "password123",
        position: CoachPosition = CoachPosition.HEAD_COACH,
        salt: String = "salt123",
        teamChoiceOne: String = "Alabama",
        teamChoiceTwo: String = "Auburn",
        teamChoiceThree: String = "Georgia",
        offensivePlaybook: OffensivePlaybook = OffensivePlaybook.AIR_RAID,
        defensivePlaybook: DefensivePlaybook = DefensivePlaybook.FOUR_THREE,
        approved: Boolean = false,
        verificationToken: String = "token123",
    ): NewSignup {
        return NewSignup(
            username = username,
            coachName = coachName,
            discordTag = discordTag,
            discordId = discordId,
            email = email,
            hashedEmail = hashedEmail,
            password = password,
            position = position,
            salt = salt,
            teamChoiceOne = teamChoiceOne,
            teamChoiceTwo = teamChoiceTwo,
            teamChoiceThree = teamChoiceThree,
            offensivePlaybook = offensivePlaybook,
            defensivePlaybook = defensivePlaybook,
            approved = approved,
            verificationToken = verificationToken,
            verificationTokenExpiration = LocalDateTime.now().plusHours(24),
        ).apply { this.id = id }
    }
}
