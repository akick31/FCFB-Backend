package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewSignupTest {
    @Test
    fun `test NewSignup entity annotations`() {
        val newSignup = NewSignup()

        val entityAnnotation = NewSignup::class.java.getAnnotation(javax.persistence.Entity::class.java)
        assertNotNull(entityAnnotation)

        val tableAnnotation = NewSignup::class.java.getAnnotation(javax.persistence.Table::class.java)
        assertNotNull(tableAnnotation)
        assertEquals("new_signup", tableAnnotation.name)
    }

    @Test
    fun `test NewSignup default constructor`() {
        val newSignup = NewSignup()

        assertEquals(0L, newSignup.id)
        assertNull(newSignup.discordId)
        assertFalse(newSignup.approved)
    }

    @Test
    fun `test NewSignup parameterized constructor`() {
        val newSignup =
            NewSignup(
                username = "newcoach",
                coachName = "New Coach",
                discordTag = "newcoach#1234",
                discordId = "123456789",
                email = "newcoach@example.com",
                hashedEmail = "hashed@example.com",
                password = "hashedpassword",
                position = CoachPosition.HEAD_COACH,
                salt = "salt123",
                teamChoiceOne = "Alabama",
                teamChoiceTwo = "Georgia",
                teamChoiceThree = "Ohio State",
                offensivePlaybook = OffensivePlaybook.AIR_RAID,
                defensivePlaybook = DefensivePlaybook.FOUR_THREE,
                approved = false,
                verificationToken = "verification123",
                verificationTokenExpiration = LocalDateTime.now().plusHours(24),
            )

        assertEquals("newcoach", newSignup.username)
        assertEquals("New Coach", newSignup.coachName)
        assertEquals("newcoach#1234", newSignup.discordTag)
        assertEquals("123456789", newSignup.discordId)
        assertEquals("newcoach@example.com", newSignup.email)
        assertEquals("hashed@example.com", newSignup.hashedEmail)
        assertEquals("hashedpassword", newSignup.password)
        assertEquals(CoachPosition.HEAD_COACH, newSignup.position)
        assertEquals("salt123", newSignup.salt)
        assertEquals("Alabama", newSignup.teamChoiceOne)
        assertEquals("Georgia", newSignup.teamChoiceTwo)
        assertEquals("Ohio State", newSignup.teamChoiceThree)
        assertEquals(OffensivePlaybook.AIR_RAID, newSignup.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, newSignup.defensivePlaybook)
        assertFalse(newSignup.approved)
        assertEquals("verification123", newSignup.verificationToken)
    }

    @Test
    fun `test NewSignup property mutability`() {
        val newSignup = NewSignup()

        newSignup.id = 1L
        newSignup.username = "updatedcoach"
        newSignup.coachName = "Updated Coach"
        newSignup.discordTag = "updatedcoach#5678"
        newSignup.discordId = "987654321"
        newSignup.email = "updatedcoach@example.com"
        newSignup.hashedEmail = "updatedhashed@example.com"
        newSignup.password = "updatedpassword"
        newSignup.position = CoachPosition.OFFENSIVE_COORDINATOR
        newSignup.salt = "updatedsalt"
        newSignup.teamChoiceOne = "Michigan"
        newSignup.teamChoiceTwo = "Penn State"
        newSignup.teamChoiceThree = "Wisconsin"
        newSignup.offensivePlaybook = OffensivePlaybook.SPREAD
        newSignup.defensivePlaybook = DefensivePlaybook.THREE_FOUR
        newSignup.approved = true
        newSignup.verificationToken = "updatedverification"

        assertEquals(1L, newSignup.id)
        assertEquals("updatedcoach", newSignup.username)
        assertEquals("Updated Coach", newSignup.coachName)
        assertEquals("updatedcoach#5678", newSignup.discordTag)
        assertEquals("987654321", newSignup.discordId)
        assertEquals("updatedcoach@example.com", newSignup.email)
        assertEquals("updatedhashed@example.com", newSignup.hashedEmail)
        assertEquals("updatedpassword", newSignup.password)
        assertEquals(CoachPosition.OFFENSIVE_COORDINATOR, newSignup.position)
        assertEquals("updatedsalt", newSignup.salt)
        assertEquals("Michigan", newSignup.teamChoiceOne)
        assertEquals("Penn State", newSignup.teamChoiceTwo)
        assertEquals("Wisconsin", newSignup.teamChoiceThree)
        assertEquals(OffensivePlaybook.SPREAD, newSignup.offensivePlaybook)
        assertEquals(DefensivePlaybook.THREE_FOUR, newSignup.defensivePlaybook)
        assertTrue(newSignup.approved)
        assertEquals("updatedverification", newSignup.verificationToken)
    }

    @Test
    fun `test NewSignup with null discordId`() {
        val newSignup = NewSignup()
        newSignup.discordId = null

        assertNull(newSignup.discordId)
    }

    @Test
    fun `test NewSignup with all CoachPosition values`() {
        val newSignup = NewSignup()

        CoachPosition.entries.forEach { position ->
            newSignup.position = position
            assertEquals(position, newSignup.position)
        }
    }

    @Test
    fun `test NewSignup with different playbook combinations`() {
        val newSignup = NewSignup()

        OffensivePlaybook.entries.forEach { offensivePlaybook ->
            newSignup.offensivePlaybook = offensivePlaybook
            assertEquals(offensivePlaybook, newSignup.offensivePlaybook)
        }

        DefensivePlaybook.entries.forEach { defensivePlaybook ->
            newSignup.defensivePlaybook = defensivePlaybook
            assertEquals(defensivePlaybook, newSignup.defensivePlaybook)
        }
    }

    @Test
    fun `test NewSignup approval status`() {
        val newSignup = NewSignup()

        newSignup.approved = false
        assertFalse(newSignup.approved)

        newSignup.approved = true
        assertTrue(newSignup.approved)

        newSignup.approved = false
        assertFalse(newSignup.approved)
    }

    @Test
    fun `test NewSignup team choices`() {
        val newSignup = NewSignup()

        newSignup.teamChoiceOne = "Alabama"
        newSignup.teamChoiceTwo = "Georgia"
        newSignup.teamChoiceThree = "Ohio State"

        assertEquals("Alabama", newSignup.teamChoiceOne)
        assertEquals("Georgia", newSignup.teamChoiceTwo)
        assertEquals("Ohio State", newSignup.teamChoiceThree)

        newSignup.teamChoiceOne = "Michigan"
        newSignup.teamChoiceTwo = "Penn State"
        newSignup.teamChoiceThree = "Wisconsin"

        assertEquals("Michigan", newSignup.teamChoiceOne)
        assertEquals("Penn State", newSignup.teamChoiceTwo)
        assertEquals("Wisconsin", newSignup.teamChoiceThree)
    }

    @Test
    fun `test NewSignup verification token`() {
        val newSignup = NewSignup()

        newSignup.verificationToken = "token123"
        assertEquals("token123", newSignup.verificationToken)

        newSignup.verificationToken = "newtoken456"
        assertEquals("newtoken456", newSignup.verificationToken)

        newSignup.verificationToken = "verylongtokenwithspecialchars!@#$%"
        assertEquals("verylongtokenwithspecialchars!@#$%", newSignup.verificationToken)
    }

    @Test
    fun `test NewSignup salt management`() {
        val newSignup = NewSignup()

        newSignup.salt = "salt123"
        assertEquals("salt123", newSignup.salt)

        newSignup.salt = "newsalt456"
        assertEquals("newsalt456", newSignup.salt)

        newSignup.salt = "verylongsaltwithspecialchars!@#$%"
        assertEquals("verylongsaltwithspecialchars!@#$%", newSignup.salt)
    }

    @Test
    fun `test NewSignup email and hashed email`() {
        val newSignup = NewSignup()

        newSignup.email = "test@example.com"
        newSignup.hashedEmail = "hashed@example.com"

        assertEquals("test@example.com", newSignup.email)
        assertEquals("hashed@example.com", newSignup.hashedEmail)

        newSignup.email = "newemail@test.org"
        newSignup.hashedEmail = "newhashed@test.org"

        assertEquals("newemail@test.org", newSignup.email)
        assertEquals("newhashed@test.org", newSignup.hashedEmail)
    }

    @Test
    fun `test NewSignup password management`() {
        val newSignup = NewSignup()

        newSignup.password = "password123"
        assertEquals("password123", newSignup.password)

        newSignup.password = "newpassword456"
        assertEquals("newpassword456", newSignup.password)

        newSignup.password = "verylongpasswordwithspecialchars!@#$%"
        assertEquals("verylongpasswordwithspecialchars!@#$%", newSignup.password)
    }

    @Test
    fun `test NewSignup discord information`() {
        val newSignup = NewSignup()

        newSignup.discordTag = "coach#1234"
        newSignup.discordId = "123456789"

        assertEquals("coach#1234", newSignup.discordTag)
        assertEquals("123456789", newSignup.discordId)

        newSignup.discordTag = "newcoach#5678"
        newSignup.discordId = "987654321"

        assertEquals("newcoach#5678", newSignup.discordTag)
        assertEquals("987654321", newSignup.discordId)

        newSignup.discordId = null
        assertNull(newSignup.discordId)
    }

    @Test
    fun `test NewSignup username and coach name`() {
        val newSignup = NewSignup()

        newSignup.username = "coach123"
        newSignup.coachName = "Coach Name"

        assertEquals("coach123", newSignup.username)
        assertEquals("Coach Name", newSignup.coachName)

        newSignup.username = "newcoach456"
        newSignup.coachName = "New Coach Name"

        assertEquals("newcoach456", newSignup.username)
        assertEquals("New Coach Name", newSignup.coachName)
    }

    @Test
    fun `test NewSignup complete signup process`() {
        val newSignup = NewSignup()

        newSignup.username = "newcoach"
        newSignup.coachName = "New Coach"
        newSignup.discordTag = "newcoach#1234"
        newSignup.discordId = "123456789"
        newSignup.email = "newcoach@example.com"
        newSignup.hashedEmail = "hashed@example.com"
        newSignup.password = "hashedpassword"
        newSignup.position = CoachPosition.HEAD_COACH
        newSignup.salt = "salt123"
        newSignup.teamChoiceOne = "Alabama"
        newSignup.teamChoiceTwo = "Georgia"
        newSignup.teamChoiceThree = "Ohio State"
        newSignup.offensivePlaybook = OffensivePlaybook.AIR_RAID
        newSignup.defensivePlaybook = DefensivePlaybook.FOUR_THREE
        newSignup.approved = false
        newSignup.verificationToken = "verification123"

        assertEquals("newcoach", newSignup.username)
        assertEquals("New Coach", newSignup.coachName)
        assertEquals("newcoach#1234", newSignup.discordTag)
        assertEquals("123456789", newSignup.discordId)
        assertEquals("newcoach@example.com", newSignup.email)
        assertEquals("hashed@example.com", newSignup.hashedEmail)
        assertEquals("hashedpassword", newSignup.password)
        assertEquals(CoachPosition.HEAD_COACH, newSignup.position)
        assertEquals("salt123", newSignup.salt)
        assertEquals("Alabama", newSignup.teamChoiceOne)
        assertEquals("Georgia", newSignup.teamChoiceTwo)
        assertEquals("Ohio State", newSignup.teamChoiceThree)
        assertEquals(OffensivePlaybook.AIR_RAID, newSignup.offensivePlaybook)
        assertEquals(DefensivePlaybook.FOUR_THREE, newSignup.defensivePlaybook)
        assertFalse(newSignup.approved)
        assertEquals("verification123", newSignup.verificationToken)

        newSignup.approved = true
        assertTrue(newSignup.approved)
    }
}
