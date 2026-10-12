package com.fcfb.arceus.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ServerUtilsTest {
    private val utils = ServerUtils()

    @Test
    fun `cancellation propagates without retry`() =
        runBlocking {
            val cancellation = CancellationException("Cancelled")
            var attempts = 0
            assertSame(
                cancellation,
                assertFailsWith<CancellationException> {
                    utils.retryWithExponentialBackoff(initialDelay = 1) {
                        attempts++
                        throw cancellation
                    }
                },
            )
            assertEquals(1, attempts)
        }

    @Test
    fun `ordinary failures retain retry count and final exception`() =
        runBlocking {
            val failure = IllegalStateException("Unavailable")
            var attempts = 0
            assertSame(
                failure,
                assertFailsWith<IllegalStateException> {
                    utils.retryWithExponentialBackoff(initialDelay = 1) {
                        attempts++
                        throw failure
                    }
                },
            )
            assertEquals(3, attempts)
        }

    @Test
    fun `successful retry retains result`() =
        runBlocking {
            var attempts = 0
            val result =
                utils.retryWithExponentialBackoff(initialDelay = 1) {
                    attempts++
                    if (attempts == 1) throw IllegalStateException("Unavailable")
                    "Ready"
                }
            assertEquals("Ready", result)
            assertEquals(2, attempts)
        }
}
