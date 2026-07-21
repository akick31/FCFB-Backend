package com.fcfb.arceus.service.auth

import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.repositories.SessionRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SessionServiceTest {
    private val sessionRepository: SessionRepository = mockk()
    private lateinit var sessionService: SessionService

    @BeforeEach
    fun setup() {
        sessionService = SessionService(sessionRepository, "test-secret-key-that-is-long-enough-for-hs512-signing")
    }

    @Test
    fun `should validate a freshly generated token`() {
        val token = sessionService.generateToken(22L, UserRole.USER)

        assertTrue(sessionService.validateToken(token))
        assertEquals(22L, sessionService.extractUserIdFromToken(token))
        assertEquals(UserRole.USER, sessionService.extractRoleFromToken(token))
    }

    @Test
    fun `should reject a malformed token`() {
        assertFalse(sessionService.validateToken("not-a-real-token"))
    }

    @Test
    fun `should treat an integer 0 from the native query as not blacklisted`() {
        every { sessionRepository.isSessionBlacklisted("some-token") } returns 0

        assertFalse(sessionService.isSessionBlacklisted("some-token"))
    }

    @Test
    fun `should treat an integer 1 from the native query as blacklisted`() {
        every { sessionRepository.isSessionBlacklisted("some-token") } returns 1

        assertTrue(sessionService.isSessionBlacklisted("some-token"))
    }
}
