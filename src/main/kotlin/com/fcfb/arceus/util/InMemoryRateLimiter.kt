package com.fcfb.arceus.util

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryRateLimiter {
    private data class Window(var count: Int, var windowStart: Instant)

    private val windows = ConcurrentHashMap<String, Window>()

    fun allow(
        key: String,
        maxRequests: Int,
        windowSeconds: Long,
    ): Boolean {
        val now = Instant.now()
        val window = windows.computeIfAbsent(key) { Window(0, now) }
        synchronized(window) {
            if (now.isAfter(window.windowStart.plusSeconds(windowSeconds))) {
                window.count = 0
                window.windowStart = now
            }
            if (window.count >= maxRequests) {
                return false
            }
            window.count += 1
            return true
        }
    }
}
