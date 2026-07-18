package com.fcfb.arceus.config

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class RequestIdFilterTest {
    private val filter = RequestIdFilter()

    @Test
    fun `generates a request id when none is provided and clears it after the chain runs`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>(relaxed = true)
        var mdcDuringChain: String? = null
        val chain =
            FilterChain { _, _ ->
                mdcDuringChain = MDC.get("request_id")
            }
        every { request.getHeader("X-Request-Id") } returns null

        filter.doFilterInternal(request, response, chain)

        assertEquals(true, mdcDuringChain?.isNotBlank())
        assertNull(MDC.get("request_id"))
        verify { response.setHeader("X-Request-Id", mdcDuringChain) }
    }

    @Test
    fun `reuses an inbound request id header instead of generating a new one`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>(relaxed = true)
        var mdcDuringChain: String? = null
        val chain =
            FilterChain { _, _ ->
                mdcDuringChain = MDC.get("request_id")
            }
        every { request.getHeader("X-Request-Id") } returns "inbound-id"

        filter.doFilterInternal(request, response, chain)

        assertEquals("inbound-id", mdcDuringChain)
        verify { response.setHeader("X-Request-Id", "inbound-id") }
    }

    @Test
    fun `clears mdc even if the chain throws`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>(relaxed = true)
        val chain =
            FilterChain { _, _ ->
                throw RuntimeException("boom")
            }
        every { request.getHeader("X-Request-Id") } returns null

        runCatching { filter.doFilterInternal(request, response, chain) }

        assertNull(MDC.get("request_id"))
    }
}
