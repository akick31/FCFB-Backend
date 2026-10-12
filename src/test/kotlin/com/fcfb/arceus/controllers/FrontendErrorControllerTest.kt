package com.fcfb.arceus.controllers

import com.fcfb.arceus.dto.request.FrontendErrorRequest
import com.fcfb.arceus.service.FrontendErrorService
import com.fcfb.arceus.util.InMemoryRateLimiter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import javax.servlet.http.HttpServletRequest

class FrontendErrorControllerTest {
    private lateinit var rateLimiter: InMemoryRateLimiter
    private lateinit var controller: FrontendErrorController
    private lateinit var servletRequest: HttpServletRequest

    @BeforeEach
    fun setUp() {
        rateLimiter = mockk()
        controller = FrontendErrorController(FrontendErrorService(rateLimiter))
        servletRequest = mockk()
        every { servletRequest.remoteAddr } returns "127.0.0.1"
    }

    @Test
    fun `reportError returns 204 when within the rate limit`() {
        every { rateLimiter.allow("127.0.0.1", 20, 60L) } returns true
        val request = FrontendErrorRequest(message = "boom", stack = "at foo.js:1", url = "https://fakecollegefootball.com")

        val response = controller.reportError(request, servletRequest)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        assertNull(MDC.get("origin"))
    }

    @Test
    fun `reportError returns 429 when the rate limit is exceeded`() {
        every { rateLimiter.allow("127.0.0.1", 20, 60L) } returns false
        val request = FrontendErrorRequest(message = "boom", stack = null, url = null)

        val response = controller.reportError(request, servletRequest)

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode)
    }

    @Test
    fun `HTTP reporting retains rate limiting by remote address`() {
        every { rateLimiter.allow("127.0.0.1", 20, 60L) } returnsMany listOf(true, false)
        val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        val report =
            post("${ApiConstants.FULL_PATH}/internal/frontend-errors")
                .header("X-Forwarded-For", "192.0.2.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message":"boom","stack":null,"url":null}""")

        mockMvc.perform(report)
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
        mockMvc.perform(report)
            .andExpect(status().isTooManyRequests)
            .andExpect(content().string(""))

        verify(exactly = 2) { rateLimiter.allow("127.0.0.1", 20, 60L) }
    }
}
