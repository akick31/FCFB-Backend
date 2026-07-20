package com.fcfb.arceus.dto.response

import com.fcfb.arceus.enums.user.UserRole
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoginResponseTest {
    @Test
    fun `LoginResponse should be a data class`() {
        val loginResponse = createTestLoginResponse()

        // Data classes automatically implement equals, hashCode, toString, and copy
        assertNotNull(loginResponse.toString())
        assertTrue(loginResponse.toString().contains("LoginResponse"))
    }

    @Test
    fun `LoginResponse should create instance with all properties`() {
        val loginResponse =
            LoginResponse(
                token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
                userId = 12345L,
                role = UserRole.USER,
            )

        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", loginResponse.token)
        assertEquals(12345L, loginResponse.userId)
        assertEquals(UserRole.USER, loginResponse.role)
    }

    @Test
    fun `LoginResponse should handle different User Role enum values`() {
        val userResponse = createTestLoginResponse().copy(role = UserRole.USER)
        val adminResponse = createTestLoginResponse().copy(role = UserRole.ADMIN)
        val commissionerResponse = createTestLoginResponse().copy(role = UserRole.CONFERENCE_COMMISSIONER)

        assertEquals(UserRole.USER, userResponse.role)
        assertEquals(UserRole.ADMIN, adminResponse.role)
        assertEquals(UserRole.CONFERENCE_COMMISSIONER, commissionerResponse.role)
    }

    @Test
    fun `LoginResponse should handle different token formats`() {
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIxNTE2MjM5MDIyfQ.SflKxwRJf36POk6yJV_adQssw5c"
        val simpleToken = "simple-token-123"
        val longToken = "a".repeat(500)

        val jwtResponse = createTestLoginResponse().copy(token = jwtToken)
        val simpleResponse = createTestLoginResponse().copy(token = simpleToken)
        val longResponse = createTestLoginResponse().copy(token = longToken)

        assertEquals(jwtToken, jwtResponse.token)
        assertEquals(simpleToken, simpleResponse.token)
        assertEquals(longToken, longResponse.token)
    }

    @Test
    fun `LoginResponse should handle different user IDs`() {
        val response1 = createTestLoginResponse().copy(userId = 1L)
        val response2 = createTestLoginResponse().copy(userId = 999999L)
        val response3 = createTestLoginResponse().copy(userId = 0L)

        assertEquals(1L, response1.userId)
        assertEquals(999999L, response2.userId)
        assertEquals(0L, response3.userId)
    }

    @Test
    fun `LoginResponse should handle empty token`() {
        val response = createTestLoginResponse().copy(token = "")

        assertEquals("", response.token)
        assertEquals(12345L, response.userId)
        assertEquals(UserRole.USER, response.role)
    }

    @Test
    fun `LoginResponse data class should support copy functionality`() {
        val original = createTestLoginResponse()
        val copied =
            original.copy(
                token = "new-token-456",
                role = UserRole.ADMIN,
            )

        assertEquals("new-token-456", copied.token)
        assertEquals(UserRole.ADMIN, copied.role)
        assertEquals(original.userId, copied.userId)
    }

    @Test
    fun `LoginResponse data class should support equality comparison`() {
        val response1 = createTestLoginResponse()
        val response2 = createTestLoginResponse()
        val response3 = response1.copy(token = "different-token")

        assertEquals(response1, response2)
        assertTrue(response1 != response3)
    }

    @Test
    fun `LoginResponse should be immutable`() {
        val response = createTestLoginResponse()

        // All properties should be val (immutable)
        // This is enforced by the data class declaration
        assertNotNull(response.token)
        assertNotNull(response.userId)
        assertNotNull(response.role)
    }

    @Test
    fun `LoginResponse should handle JWT token structure`() {
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ey2QT4fwpMeJ.f36POk6yJV_adQssw5c"
        val response = createTestLoginResponse().copy(token = jwtToken)

        assertEquals(jwtToken, response.token)

        // JWT tokens have 3 parts separated by dots
        val parts = response.token.split(".")
        assertEquals(3, parts.size)
        assertTrue(parts.all { it.isNotEmpty() })
    }

    @Test
    fun `LoginResponse should handle different authentication scenarios`() {
        // Regular user login
        val userLogin =
            LoginResponse(
                token = "user-token-123",
                userId = 1001L,
                role = UserRole.USER,
            )

        // Admin login
        val adminLogin =
            LoginResponse(
                token = "admin-token-456",
                userId = 2001L,
                role = UserRole.ADMIN,
            )

        // Conference commissioner login
        val commissionerLogin =
            LoginResponse(
                token = "commissioner-token-789",
                userId = 3001L,
                role = UserRole.CONFERENCE_COMMISSIONER,
            )

        assertEquals(UserRole.USER, userLogin.role)
        assertEquals(1001L, userLogin.userId)

        assertEquals(UserRole.ADMIN, adminLogin.role)
        assertEquals(2001L, adminLogin.userId)

        assertEquals(UserRole.CONFERENCE_COMMISSIONER, commissionerLogin.role)
        assertEquals(3001L, commissionerLogin.userId)
    }

    @Test
    fun `LoginResponse should handle token with special characters`() {
        val tokenWithSpecialChars = "token-with_special.chars+and/numbers123="
        val response = createTestLoginResponse().copy(token = tokenWithSpecialChars)

        assertEquals(tokenWithSpecialChars, response.token)
    }

    @Test
    fun `LoginResponse should handle large user IDs`() {
        val largeUserId = Long.MAX_VALUE
        val response = createTestLoginResponse().copy(userId = largeUserId)

        assertEquals(largeUserId, response.userId)
    }

    @Test
    fun `LoginResponse should support component destructuring`() {
        val response = createTestLoginResponse()
        val (token, userId, role) = response

        assertEquals(response.token, token)
        assertEquals(response.userId, userId)
        assertEquals(response.role, role)
    }

    @Test
    fun `LoginResponse should handle all role types correctly`() {
        val roles =
            listOf(
                UserRole.USER,
                UserRole.ADMIN,
                UserRole.CONFERENCE_COMMISSIONER,
            )

        roles.forEach { role ->
            val response = createTestLoginResponse().copy(role = role)
            assertEquals(role, response.role)
        }
    }

    private fun createTestLoginResponse(): LoginResponse {
        return LoginResponse(
            token = "test-token-123",
            userId = 12345L,
            role = UserRole.USER,
        )
    }
}
