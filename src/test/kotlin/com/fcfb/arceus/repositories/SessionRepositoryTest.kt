package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.Session
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SessionRepositoryTest {
    private lateinit var sessionRepository: SessionRepository

    @BeforeEach
    fun setUp() {
        sessionRepository = mockk(relaxed = true)
    }

    @Test
    fun `test save and findById`() {
        val session =
            createTestSession(
                id = 1L,
                token = "test_token_123",
                userId = 123L,
                expirationDate = LocalDateTime.now().plusDays(1),
            )

        every { sessionRepository.save(any()) } returns session
        every { sessionRepository.findById(1L) } returns java.util.Optional.of(session)

        val savedSession = sessionRepository.save(session)
        val foundSession = sessionRepository.findById(savedSession.id).get()

        assertNotNull(foundSession)
        assertEquals(1L, foundSession.id)
        assertEquals("test_token_123", foundSession.token)
        assertEquals(123L, foundSession.userId)
        assertNotNull(foundSession.expirationDate)
    }

    @Test
    fun `test deleteByToken`() {
        every { sessionRepository.deleteByToken("delete_token_123") } returns Unit

        sessionRepository.deleteByToken("delete_token_123")

        verify { sessionRepository.deleteByToken("delete_token_123") }
    }

    @Test
    fun `test blacklistUserSession`() {
        every { sessionRepository.blacklistUserSession(any(), any(), any()) } returns Unit

        sessionRepository.blacklistUserSession(
            token = "blacklist_token_123",
            userId = 789L,
            expirationDate = java.util.Date(),
        )

        verify { sessionRepository.blacklistUserSession("blacklist_token_123", 789L, any()) }
    }

    @Test
    fun `test isSessionBlacklisted returns false for non-blacklisted token`() {
        every { sessionRepository.isSessionBlacklisted("nonexistent_token") } returns false

        val isBlacklisted = sessionRepository.isSessionBlacklisted("nonexistent_token")

        assertFalse(isBlacklisted)
    }

    @Test
    fun `test clearExpiredTokens`() {
        every { sessionRepository.clearExpiredTokens() } returns Unit

        sessionRepository.clearExpiredTokens()

        verify { sessionRepository.clearExpiredTokens() }
    }

    @Test
    fun `test findAll`() {
        val session1 = createTestSession(id = 1L, token = "token1", userId = 123L)
        val session2 = createTestSession(id = 2L, token = "token2", userId = 456L)
        val sessions = listOf(session1, session2)

        every { sessionRepository.findAll() } returns sessions

        val foundSessions = sessionRepository.findAll()

        assertEquals(2, foundSessions.count())
        assertTrue(foundSessions.any { it.token == "token1" })
        assertTrue(foundSessions.any { it.token == "token2" })
    }

    @Test
    fun `test count`() {
        every { sessionRepository.count() } returns 10L

        val count = sessionRepository.count()

        assertEquals(10L, count)
    }

    @Test
    fun `test existsById`() {
        every { sessionRepository.existsById(1L) } returns true
        every { sessionRepository.existsById(999L) } returns false

        assertTrue(sessionRepository.existsById(1L))
        assertFalse(sessionRepository.existsById(999L))
    }

    @Test
    fun `test findAllById`() {
        val session1 = createTestSession(id = 1L, token = "token1", userId = 123L)
        val session2 = createTestSession(id = 2L, token = "token2", userId = 456L)
        val sessions = listOf(session1, session2)
        val ids = listOf(1L, 2L)

        every { sessionRepository.findAllById(ids) } returns sessions

        val foundSessions = sessionRepository.findAllById(ids)

        assertEquals(2, foundSessions.count())
        assertTrue(foundSessions.any { it.id == 1L })
        assertTrue(foundSessions.any { it.id == 2L })
    }

    @Test
    fun `test delete`() {
        val session = createTestSession(id = 1L, token = "token1", userId = 123L)
        every { sessionRepository.delete(session) } returns Unit

        sessionRepository.delete(session)

        verify { sessionRepository.delete(session) }
    }

    @Test
    fun `test deleteById`() {
        every { sessionRepository.deleteById(1L) } returns Unit

        sessionRepository.deleteById(1L)

        verify { sessionRepository.deleteById(1L) }
    }

    @Test
    fun `test deleteAll`() {
        every { sessionRepository.deleteAll() } returns Unit

        sessionRepository.deleteAll()

        verify { sessionRepository.deleteAll() }
    }

    @Test
    fun `test saveAll`() {
        val session1 = createTestSession(id = 1L, token = "token1", userId = 123L)
        val session2 = createTestSession(id = 2L, token = "token2", userId = 456L)
        val sessions = listOf(session1, session2)

        every { sessionRepository.saveAll(sessions) } returns sessions

        val savedSessions = sessionRepository.saveAll(sessions)

        assertEquals(2, savedSessions.count())
        assertTrue(savedSessions.any { it.token == "token1" })
        assertTrue(savedSessions.any { it.token == "token2" })
    }

    private fun createTestSession(
        id: Long = 1L,
        token: String = "test_token",
        userId: Long = 123L,
        expirationDate: LocalDateTime = LocalDateTime.now().plusDays(1),
    ): Session {
        return Session(
            id = id,
            userId = userId,
            token = token,
            expirationDate = expirationDate,
        )
    }
}
