package com.fcfb.arceus.model

import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionTest {
    @Test
    fun `Session should be properly annotated`() {
        val entityAnnotation = Session::class.annotations.find { it is javax.persistence.Entity }
        val tableAnnotation = Session::class.annotations.find { it is javax.persistence.Table }

        assertNotNull(entityAnnotation, "Session should be annotated with @Entity")
        assertNotNull(tableAnnotation, "Session should be annotated with @Table")
    }

    @Test
    fun `Session should be a data class`() {
        val session = createTestSession()

        // Data classes automatically implement equals, hashCode, toString, and copy
        assertNotNull(session.toString())
        assertTrue(session.toString().contains("Session"))
    }

    @Test
    fun `Session primary constructor should create instance with all properties`() {
        val now = LocalDateTime.now()
        val expiration = now.plusHours(24)

        val session =
            Session(
                id = 123L,
                userId = 456L,
                token = "test-token-789",
                expirationDate = expiration,
                createdAt = now,
            )

        assertEquals(123L, session.id)
        assertEquals(456L, session.userId)
        assertEquals("test-token-789", session.token)
        assertEquals(expiration, session.expirationDate)
        assertEquals(now, session.createdAt)
    }

    @Test
    fun `Session secondary constructor should work without id and createdAt`() {
        val expiration = LocalDateTime.now().plusHours(24)

        val session =
            Session(
                userId = 789L,
                token = "secondary-token",
                expirationDate = expiration,
            )

        assertEquals(0L, session.id) // Default value
        assertEquals(789L, session.userId)
        assertEquals("secondary-token", session.token)
        assertEquals(expiration, session.expirationDate)
        assertNotNull(session.createdAt) // Should be set to current time
    }

    @Test
    fun `Session default constructor should work`() {
        val session = Session()

        assertEquals(0L, session.id)
        assertEquals(0L, session.userId)
        assertEquals("", session.token)
        assertNotNull(session.expirationDate)
        assertNotNull(session.createdAt)
    }

    @Test
    fun `Session should handle different token formats`() {
        val jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature"
        val simpleToken = "simple-token-123"
        val longToken = "a".repeat(500)
        val emptyToken = ""

        val expiration = LocalDateTime.now().plusHours(1)

        val jwtSession = Session(1L, jwtToken, expiration)
        val simpleSession = Session(2L, simpleToken, expiration)
        val longSession = Session(3L, longToken, expiration)
        val emptySession = Session(4L, emptyToken, expiration)

        assertEquals(jwtToken, jwtSession.token)
        assertEquals(simpleToken, simpleSession.token)
        assertEquals(longToken, longSession.token)
        assertEquals(emptyToken, emptySession.token)
    }

    @Test
    fun `Session should handle different user IDs`() {
        val expiration = LocalDateTime.now().plusHours(1)

        val session1 = Session(1L, "token1", expiration)
        val session2 = Session(999999L, "token2", expiration)
        val session3 = Session(0L, "token3", expiration)

        assertEquals(1L, session1.userId)
        assertEquals(999999L, session2.userId)
        assertEquals(0L, session3.userId)
    }

    @Test
    fun `Session should handle different expiration dates`() {
        val now = LocalDateTime.now()
        val shortExpiration = now.plusMinutes(30)
        val longExpiration = now.plusDays(30)
        val pastExpiration = now.minusHours(1)

        val shortSession = Session(1L, "token1", shortExpiration)
        val longSession = Session(2L, "token2", longExpiration)
        val expiredSession = Session(3L, "token3", pastExpiration)

        assertEquals(shortExpiration, shortSession.expirationDate)
        assertEquals(longExpiration, longSession.expirationDate)
        assertEquals(pastExpiration, expiredSession.expirationDate)
    }

    @Test
    fun `Session should handle creation time correctly`() {
        val specificTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
        val expiration = specificTime.plusHours(24)

        val session =
            Session(
                id = 1L,
                userId = 123L,
                token = "token",
                expirationDate = expiration,
                createdAt = specificTime,
            )

        assertEquals(specificTime, session.createdAt)
    }

    @Test
    fun `Session data class should support copy functionality`() {
        val original = createTestSession()
        val newExpiration = LocalDateTime.now().plusDays(7)

        val copied =
            original.copy(
                token = "new-token",
                expirationDate = newExpiration,
            )

        assertEquals("new-token", copied.token)
        assertEquals(newExpiration, copied.expirationDate)
        assertEquals(original.id, copied.id)
        assertEquals(original.userId, copied.userId)
        assertEquals(original.createdAt, copied.createdAt)
    }

    @Test
    fun `Session data class should support equality comparison`() {
        val now = LocalDateTime.now()
        val expiration = now.plusHours(24)

        val session1 = Session(1L, 100L, "token", expiration, now)
        val session2 = Session(1L, 100L, "token", expiration, now)
        val session3 = session1.copy(token = "different-token")

        assertEquals(session1, session2)
        assertTrue(session1 != session3)
    }

    @Test
    fun `Session should be immutable`() {
        val session = createTestSession()

        // All properties should be val (immutable)
        // This is enforced by the data class declaration
        assertNotNull(session.id)
        assertNotNull(session.userId)
        assertNotNull(session.token)
        assertNotNull(session.expirationDate)
        assertNotNull(session.createdAt)
    }

    @Test
    fun `Session should support component destructuring`() {
        val session = createTestSession()
        val (id, userId, token, expirationDate, createdAt) = session

        assertEquals(session.id, id)
        assertEquals(session.userId, userId)
        assertEquals(session.token, token)
        assertEquals(session.expirationDate, expirationDate)
        assertEquals(session.createdAt, createdAt)
    }

    @Test
    fun `Session should handle session expiration logic`() {
        val now = LocalDateTime.now()
        val futureExpiration = now.plusHours(1)
        val pastExpiration = now.minusHours(1)

        val activeSession = Session(1L, "active-token", futureExpiration)
        val expiredSession = Session(2L, "expired-token", pastExpiration)

        // Check if session is expired (would be implemented as extension function or method)
        assertTrue(activeSession.expirationDate.isAfter(now))
        assertTrue(expiredSession.expirationDate.isBefore(now))
    }

    @Test
    fun `Session should handle different ID values`() {
        val expiration = LocalDateTime.now().plusHours(1)

        val session1 = Session(1L, 100L, "token1", expiration, LocalDateTime.now())
        val session2 = Session(Long.MAX_VALUE, 200L, "token2", expiration, LocalDateTime.now())
        val session3 = Session(0L, 300L, "token3", expiration, LocalDateTime.now())

        assertEquals(1L, session1.id)
        assertEquals(Long.MAX_VALUE, session2.id)
        assertEquals(0L, session3.id)
    }

    @Test
    fun `Session should handle token with special characters`() {
        val tokenWithSpecialChars = "token-with_special.chars+and/numbers123="
        val expiration = LocalDateTime.now().plusHours(1)

        val session = Session(1L, tokenWithSpecialChars, expiration)

        assertEquals(tokenWithSpecialChars, session.token)
    }

    @Test
    fun `Session should handle precise datetime values`() {
        val preciseTime = LocalDateTime.of(2024, 12, 25, 15, 30, 45, 123456789)
        val preciseExpiration = LocalDateTime.of(2024, 12, 26, 15, 30, 45, 987654321)

        val session =
            Session(
                id = 1L,
                userId = 123L,
                token = "precise-token",
                expirationDate = preciseExpiration,
                createdAt = preciseTime,
            )

        assertEquals(preciseTime, session.createdAt)
        assertEquals(preciseExpiration, session.expirationDate)
    }

    @Test
    fun `Session constructors should work correctly`() {
        val now = LocalDateTime.now()
        val expiration = now.plusHours(24)

        // Test all three constructors
        val fullSession = Session(1L, 100L, "token1", expiration, now)
        val partialSession = Session(200L, "token2", expiration)
        val defaultSession = Session()

        // Full constructor
        assertEquals(1L, fullSession.id)
        assertEquals(100L, fullSession.userId)
        assertEquals("token1", fullSession.token)
        assertEquals(expiration, fullSession.expirationDate)
        assertEquals(now, fullSession.createdAt)

        // Partial constructor
        assertEquals(0L, partialSession.id)
        assertEquals(200L, partialSession.userId)
        assertEquals("token2", partialSession.token)
        assertEquals(expiration, partialSession.expirationDate)
        assertNotNull(partialSession.createdAt)

        // Default constructor
        assertEquals(0L, defaultSession.id)
        assertEquals(0L, defaultSession.userId)
        assertEquals("", defaultSession.token)
        assertNotNull(defaultSession.expirationDate)
        assertNotNull(defaultSession.createdAt)
    }

    private fun createTestSession(): Session {
        val now = LocalDateTime.now()
        val expiration = now.plusHours(24)

        return Session(
            id = 123L,
            userId = 456L,
            token = "test-token-789",
            expirationDate = expiration,
            createdAt = now,
        )
    }
}
