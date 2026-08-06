package com.fcfb.arceus.dto.response

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserValidationResponseTest {
    @Test
    fun `UserValidationResponse should be a data class`() {
        val response = createTestUserValidationResponse()

        assertNotNull(response.toString())
        assertTrue(response.toString().contains("UserValidationResponse"))
    }

    @Test
    fun `UserValidationResponse should create instance with all properties`() {
        val response =
            UserValidationResponse(
                discordIdExists = true,
                discordTagExists = false,
                usernameExists = true,
                emailExists = false,
            )

        assertTrue(response.discordIdExists)
        assertFalse(response.discordTagExists)
        assertTrue(response.usernameExists)
        assertFalse(response.emailExists)
    }

    @Test
    fun `UserValidationResponse properties should be mutable`() {
        val response = createTestUserValidationResponse()

        response.discordIdExists = false
        response.discordTagExists = true
        response.usernameExists = false
        response.emailExists = true

        assertFalse(response.discordIdExists)
        assertTrue(response.discordTagExists)
        assertFalse(response.usernameExists)
        assertTrue(response.emailExists)
    }

    @Test
    fun `UserValidationResponse should handle all true values`() {
        val response =
            UserValidationResponse(
                discordIdExists = true,
                discordTagExists = true,
                usernameExists = true,
                emailExists = true,
            )

        assertTrue(response.discordIdExists)
        assertTrue(response.discordTagExists)
        assertTrue(response.usernameExists)
        assertTrue(response.emailExists)
    }

    @Test
    fun `UserValidationResponse should handle all false values`() {
        val response =
            UserValidationResponse(
                discordIdExists = false,
                discordTagExists = false,
                usernameExists = false,
                emailExists = false,
            )

        assertFalse(response.discordIdExists)
        assertFalse(response.discordTagExists)
        assertFalse(response.usernameExists)
        assertFalse(response.emailExists)
    }

    @Test
    fun `UserValidationResponse should handle mixed boolean values`() {
        val response1 =
            UserValidationResponse(
                discordIdExists = true,
                discordTagExists = false,
                usernameExists = true,
                emailExists = false,
            )

        val response2 =
            UserValidationResponse(
                discordIdExists = false,
                discordTagExists = true,
                usernameExists = false,
                emailExists = true,
            )

        assertTrue(response1.discordIdExists)
        assertFalse(response1.discordTagExists)
        assertTrue(response1.usernameExists)
        assertFalse(response1.emailExists)

        assertFalse(response2.discordIdExists)
        assertTrue(response2.discordTagExists)
        assertFalse(response2.usernameExists)
        assertTrue(response2.emailExists)
    }

    @Test
    fun `UserValidationResponse data class should support copy functionality`() {
        val original = createTestUserValidationResponse()
        val copied =
            original.copy(
                discordIdExists = false,
                emailExists = true,
            )

        assertFalse(copied.discordIdExists)
        assertTrue(copied.emailExists)
        assertEquals(original.discordTagExists, copied.discordTagExists)
        assertEquals(original.usernameExists, copied.usernameExists)
    }

    @Test
    fun `UserValidationResponse data class should support equality comparison`() {
        val response1 = createTestUserValidationResponse()
        val response2 = createTestUserValidationResponse()
        val response3 = response1.copy(discordIdExists = false)

        assertEquals(response1, response2)
        assertTrue(response1 != response3)
    }

    @Test
    fun `UserValidationResponse should handle validation scenarios`() {
        val newUser =
            UserValidationResponse(
                discordIdExists = false,
                discordTagExists = false,
                usernameExists = false,
                emailExists = false,
            )

        assertFalse(newUser.discordIdExists)
        assertFalse(newUser.discordTagExists)
        assertFalse(newUser.usernameExists)
        assertFalse(newUser.emailExists)

        val existingUser =
            UserValidationResponse(
                discordIdExists = true,
                discordTagExists = true,
                usernameExists = true,
                emailExists = true,
            )

        assertTrue(existingUser.discordIdExists)
        assertTrue(existingUser.discordTagExists)
        assertTrue(existingUser.usernameExists)
        assertTrue(existingUser.emailExists)

        val partialConflict =
            UserValidationResponse(
                discordIdExists = true,
                discordTagExists = false,
                usernameExists = false,
                emailExists = true,
            )

        assertTrue(partialConflict.discordIdExists)
        assertFalse(partialConflict.discordTagExists)
        assertFalse(partialConflict.usernameExists)
        assertTrue(partialConflict.emailExists)
    }

    @Test
    fun `UserValidationResponse should handle individual property updates`() {
        val response =
            UserValidationResponse(
                discordIdExists = false,
                discordTagExists = false,
                usernameExists = false,
                emailExists = false,
            )

        response.discordIdExists = true
        assertTrue(response.discordIdExists)
        assertFalse(response.discordTagExists)
        assertFalse(response.usernameExists)
        assertFalse(response.emailExists)

        response.discordTagExists = true
        assertTrue(response.discordIdExists)
        assertTrue(response.discordTagExists)
        assertFalse(response.usernameExists)
        assertFalse(response.emailExists)

        response.usernameExists = true
        assertTrue(response.discordIdExists)
        assertTrue(response.discordTagExists)
        assertTrue(response.usernameExists)
        assertFalse(response.emailExists)

        response.emailExists = true
        assertTrue(response.discordIdExists)
        assertTrue(response.discordTagExists)
        assertTrue(response.usernameExists)
        assertTrue(response.emailExists)
    }

    @Test
    fun `UserValidationResponse should support boolean operations`() {
        val response = createTestUserValidationResponse()

        val hasAnyConflict =
            response.discordIdExists || response.discordTagExists ||
                response.usernameExists || response.emailExists
        assertTrue(hasAnyConflict)

        val hasAllConflicts =
            response.discordIdExists && response.discordTagExists &&
                response.usernameExists && response.emailExists
        assertFalse(hasAllConflicts)

        val noDiscordIdConflict = !response.discordIdExists
        assertFalse(noDiscordIdConflict)
    }

    @Test
    fun `UserValidationResponse should handle validation result interpretation`() {
        val response = createTestUserValidationResponse()

        val conflictCount =
            listOf(
                response.discordIdExists,
                response.discordTagExists,
                response.usernameExists,
                response.emailExists,
            ).count { it }

        assertTrue(conflictCount > 0)

        val canCreateUser =
            !response.discordIdExists && !response.discordTagExists &&
                !response.usernameExists && !response.emailExists
        assertFalse(canCreateUser)
    }

    private fun createTestUserValidationResponse(): UserValidationResponse {
        return UserValidationResponse(
            discordIdExists = true,
            discordTagExists = false,
            usernameExists = true,
            emailExists = false,
        )
    }
}
