package com.fcfb.arceus.filters

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorsFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // Handle preflight request (OPTIONS)
        if ("OPTIONS" == request.method) {
            response.status = HttpServletResponse.SC_OK

            val allowedOrigins = setOf("https://fakecollegefootball.com")
            val origin = request.getHeader("Origin")

            if (origin != null && origin in allowedOrigins) {
                response.addHeader("Access-Control-Allow-Origin", origin)
                response.addHeader("Access-Control-Allow-Credentials", "true")
            }
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, HEAD")
            response.addHeader(
                "Access-Control-Allow-Headers",
                "Origin, " +
                    "Accept, " +
                    "X-Requested-With, " +
                    "Content-Type, " +
                    "Authorization, " +
                    "Access-Control-Request-Method, " +
                    "Access-Control-Request-Headers",
            )
            response.addIntHeader("Access-Control-Max-Age", 3600) // Cache preflight request for 1 hour
        } else {
            // Allow cross-origin requests from any origin
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, PATCH, HEAD")
            response.addHeader(
                "Access-Control-Allow-Headers",
                "Origin, " +
                    "Accept, " +
                    "X-Requested-With, " +
                    "Content-Type, " +
                    "Authorization, " +
                    "Access-Control-Request-Method, " +
                    "Access-Control-Request-Headers",
            )
            response.addHeader("Access-Control-Expose-Headers", "Access-Control-Allow-Origin, Access-Control-Allow-Credentials")
            response.addHeader("Access-Control-Allow-Credentials", "false")
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response)
    }
}
