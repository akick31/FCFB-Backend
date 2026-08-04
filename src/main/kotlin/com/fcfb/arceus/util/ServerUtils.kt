package com.fcfb.arceus.util

import kotlinx.coroutines.delay
import org.springframework.stereotype.Component

@Component
class ServerUtils {
    /** Retries the given operation, backing off exponentially between attempts. */
    suspend fun <T> retryWithExponentialBackoff(
        retries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        repeat(retries - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                Logger.warn("Attempt ${attempt + 1} failed: ${e.message}. Retrying in ${currentDelay}ms...")
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }
}
