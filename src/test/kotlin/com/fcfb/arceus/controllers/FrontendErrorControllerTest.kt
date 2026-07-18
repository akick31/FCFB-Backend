package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.FrontendErrorRequest
import com.fcfb.arceus.util.InMemoryRateLimiter
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import javax.servlet.http.HttpServletRequest

class FrontendErrorControllerTest {
    private lateinit var rateLimiter: InMemoryRateLimiter
    private lateinit var controller: FrontendErrorController
    private lateinit var servletRequest: HttpServletRequest

    @BeforeEach
    fun setUp() {
        rateLimiter = mockk()
        controller = FrontendErrorController(rateLimiter)
        servletRequest = mockk()
        every { servletRequest.remoteAddr } returns "127.0.0.1"
    }

    @Test
    fun `reportError returns 204 when within the rate limit`() {
        every { rateLimiter.allow("127.0.0.1", any(), any()) } returns true
        val request = FrontendErrorRequest(message = "boom", stack = "at foo.js:1", url = "https://fakecollegefootball.com")

        val response = controller.reportError(request, servletRequest)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
    }

    @Test
    fun `reportError returns 429 when the rate limit is exceeded`() {
        every { rateLimiter.allow("127.0.0.1", any(), any()) } returns false
        val request = FrontendErrorRequest(message = "boom", stack = null, url = null)

        val response = controller.reportError(request, servletRequest)

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode)
    }
}
