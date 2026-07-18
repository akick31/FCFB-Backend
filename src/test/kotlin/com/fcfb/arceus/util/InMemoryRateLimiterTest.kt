package com.fcfb.arceus.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InMemoryRateLimiterTest {
    @Test
    fun `allows requests up to the limit within the window`() {
        val rateLimiter = InMemoryRateLimiter()

        repeat(3) {
            assertTrue(rateLimiter.allow("key", maxRequests = 3, windowSeconds = 60))
        }
    }

    @Test
    fun `rejects requests once the limit is exceeded within the window`() {
        val rateLimiter = InMemoryRateLimiter()

        repeat(3) { rateLimiter.allow("key", maxRequests = 3, windowSeconds = 60) }

        assertFalse(rateLimiter.allow("key", maxRequests = 3, windowSeconds = 60))
    }

    @Test
    fun `tracks separate keys independently`() {
        val rateLimiter = InMemoryRateLimiter()

        repeat(3) { rateLimiter.allow("key-a", maxRequests = 3, windowSeconds = 60) }

        assertTrue(rateLimiter.allow("key-b", maxRequests = 3, windowSeconds = 60))
    }

    @Test
    fun `allows requests again once the window resets`() {
        val rateLimiter = InMemoryRateLimiter()

        repeat(3) { rateLimiter.allow("key", maxRequests = 3, windowSeconds = 0) }

        assertTrue(rateLimiter.allow("key", maxRequests = 3, windowSeconds = 0))
    }
}
