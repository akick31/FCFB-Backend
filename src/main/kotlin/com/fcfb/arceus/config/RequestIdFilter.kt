package com.fcfb.arceus.config

import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val REQUEST_ID_HEADER = "X-Request-Id"
private const val REQUEST_ID_MDC_KEY = "request_id"

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {
    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = request.getHeader(REQUEST_ID_HEADER) ?: UUID.randomUUID().toString()
        MDC.put(REQUEST_ID_MDC_KEY, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY)
        }
    }
}
