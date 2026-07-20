package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.FrontendErrorRequest
import com.fcfb.arceus.util.InMemoryRateLimiter
import com.fcfb.arceus.util.Logger
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private const val MAX_REQUESTS_PER_WINDOW = 20
private const val WINDOW_SECONDS = 60L

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/internal/frontend-errors")
class FrontendErrorController(
    private val rateLimiter: InMemoryRateLimiter,
) {
    @PostMapping("")
    fun reportError(
        @RequestBody request: FrontendErrorRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<Unit> {
        val clientIp = servletRequest.remoteAddr
        if (!rateLimiter.allow(clientIp, MAX_REQUESTS_PER_WINDOW, WINDOW_SECONDS)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
        }

        MDC.put("origin", "website")
        try {
            Logger.error(
                "Frontend error at ${request.url}: ${request.message}\n${request.stack ?: "no stack trace"}",
            )
        } finally {
            MDC.remove("origin")
        }

        return ResponseEntity.noContent().build()
    }
}
