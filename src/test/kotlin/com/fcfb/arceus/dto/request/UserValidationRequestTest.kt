package com.fcfb.arceus.dto.request

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserValidationRequestTest {
    @Test
    fun `UserValidationRequest should be a data class`() {
        val request = createTestUserValidationRequest()

        // Data classes automatically implement equals, hashCode, toString, and copy
        assertNotNull(request.toString())
        assertTrue(request.toString().contains("UserValidationRequest"))
    }

    @Test
    fun `UserValidationRequest should create instance with all properties`() {
        val request =
            UserValidationRequest(
                discordId = "123456789012345678",
                discordTag = "testuser#1234",
                username = "testuser",
                email = "testuser@example.com",
            )

        assertEquals("123456789012345678", request.discordId)
        assertEquals("testuser#1234", request.discordTag)
        assertEquals("testuser", request.username)
        assertEquals("testuser@example.com", request.email)
    }

    @Test
    fun `UserValidationRequest properties should be mutable`() {
        val request = createTestUserValidationRequest()

        request.discordId = "987654321098765432"
        request.discordTag = "updateduser#5678"
        request.username = "updateduser"
        request.email = "updated@example.com"

        assertEquals("987654321098765432", request.discordId)
        assertEquals("updateduser#5678", request.discordTag)
        assertEquals("updateduser", request.username)
        assertEquals("updated@example.com", request.email)
    }

    @Test
    fun `UserValidationRequest should handle Discord ID formats`() {
        val request = createTestUserValidationRequest()

        // Discord IDs are typically 18-digit numbers
        assertTrue(request.discordId.length >= 17)
        assertTrue(request.discordId.all { it.isDigit() })

        // Test with different Discord ID
        request.discordId = "999999999999999999"
        assertEquals("999999999999999999", request.discordId)
    }

    @Test
    fun `UserValidationRequest should handle Discord tag formats`() {
        val request = createTestUserValidationRequest()

        // Test various Discord tag formats
        request.discordTag = "user#1234"
        assertEquals("user#1234", request.discordTag)

        request.discordTag = "longusernamewithspecialchars123#9999"
        assertEquals("longusernamewithspecialchars123#9999", request.discordTag)

        request.discordTag = "user_with.special-chars#0001"
        assertEquals("user_with.special-chars#0001", request.discordTag)
    }

    @Test
    fun `UserValidationRequest should handle username formats`() {
        val request = createTestUserValidationRequest()

        // Test different username formats
        request.username = "simpleuser"
        assertEquals("simpleuser", request.username)

        request.username = "user123"
        assertEquals("user123", request.username)

        request.username = "user_with_underscores"
        assertEquals("user_with_underscores", request.username)

        request.username = "user-with-dashes"
        assertEquals("user-with-dashes", request.username)
    }

    @Test
    fun `UserValidationRequest should handle email formats`() {
        val request = createTestUserValidationRequest()

        // Test different email formats
        request.email = "simple@example.com"
        assertEquals("simple@example.com", request.email)

        request.email = "user.name@domain.co.uk"
        assertEquals("user.name@domain.co.uk", request.email)

        request.email = "user+tag@example.org"
        assertEquals("user+tag@example.org", request.email)

        request.email = "user123@sub.domain.com"
        assertEquals("user123@sub.domain.com", request.email)
    }

    @Test
    fun `UserValidationRequest should handle long values`() {
        val request = createTestUserValidationRequest()

        val longUsername = "a".repeat(100)
        val longEmail = "user@" + "domain".repeat(20) + ".com"

        request.username = longUsername
        request.email = longEmail

        assertEquals(longUsername, request.username)
        assertEquals(longEmail, request.email)
    }

    @Test
    fun `UserValidationRequest data class should support copy functionality`() {
        val original = createTestUserValidationRequest()
        val copied = original.copy(username = "copieduser", email = "copied@example.com")

        assertEquals("copieduser", copied.username)
        assertEquals("copied@example.com", copied.email)
        assertEquals(original.discordId, copied.discordId)
        assertEquals(original.discordTag, copied.discordTag)
    }

    @Test
    fun `UserValidationRequest data class should support equality comparison`() {
        val request1 = createTestUserValidationRequest()
        val request2 = createTestUserValidationRequest()
        val request3 = request1.copy(username = "different")

        assertEquals(request1, request2)
        assertTrue(request1 != request3)
    }

    @Test
    fun `UserValidationRequest should handle special characters in fields`() {
        val request =
            UserValidationRequest(
                discordId = "123456789012345678",
                discordTag = "user.with_special-chars123#9999",
                username = "user@domain.com",
                email = "user+special@domain-name.co.uk",
            )

        assertEquals("123456789012345678", request.discordId)
        assertEquals("user.with_special-chars123#9999", request.discordTag)
        assertEquals("user@domain.com", request.username)
        assertEquals("user+special@domain-name.co.uk", request.email)
    }

    @Test
    fun `UserValidationRequest should handle empty strings`() {
        val request =
            UserValidationRequest(
                discordId = "",
                discordTag = "",
                username = "",
                email = "",
            )

        assertEquals("", request.discordId)
        assertEquals("", request.discordTag)
        assertEquals("", request.username)
        assertEquals("", request.email)
    }

    @Test
    fun `UserValidationRequest should handle all field updates`() {
        val request =
            UserValidationRequest(
                discordId = "initial",
                discordTag = "initial",
                username = "initial",
                email = "initial",
            )

        // Update all fields
        request.discordId = "123456789012345678"
        request.discordTag = "newuser#1234"
        request.username = "newusername"
        request.email = "new@example.com"

        assertEquals("123456789012345678", request.discordId)
        assertEquals("newuser#1234", request.discordTag)
        assertEquals("newusername", request.username)
        assertEquals("new@example.com", request.email)
    }

    @Test
    fun `UserValidationRequest should handle case sensitive values`() {
        val request =
            UserValidationRequest(
                discordId = "123456789012345678",
                discordTag = "TestUser#1234",
                username = "TestUsername",
                email = "Test@Example.COM",
            )

        assertEquals("123456789012345678", request.discordId)
        assertEquals("TestUser#1234", request.discordTag)
        assertEquals("TestUsername", request.username)
        assertEquals("Test@Example.COM", request.email)
    }

    private fun createTestUserValidationRequest(): UserValidationRequest {
        return UserValidationRequest(
            discordId = "123456789012345678",
            discordTag = "testuser#1234",
            username = "testuser",
            email = "testuser@example.com",
        )
    }
}
