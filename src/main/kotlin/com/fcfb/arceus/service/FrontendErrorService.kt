package com.fcfb.arceus.service

import com.fcfb.arceus.dto.request.FrontendErrorRequest
import com.fcfb.arceus.util.InMemoryRateLimiter
import com.fcfb.arceus.util.Logger
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

private const val MAX_REQUESTS_PER_WINDOW = 20
private const val WINDOW_SECONDS = 60L

@Service
class FrontendErrorService(
    private val rateLimiter: InMemoryRateLimiter,
) {
    fun reportError(
        request: FrontendErrorRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val clientIp = servletRequest.remoteAddr
        if (!rateLimiter.allow(clientIp, MAX_REQUESTS_PER_WINDOW, WINDOW_SECONDS)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
        }

        MDC.put("origin", "website")
        try {
            Logger().error(
                "Frontend error at ${request.url}: ${request.message}\n${request.stack ?: "no stack trace"}",
            )
        } finally {
            MDC.remove("origin")
        }

        return ResponseEntity.noContent().build()
    }
}
