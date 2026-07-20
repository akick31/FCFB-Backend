package com.fcfb.arceus.filters

import com.fcfb.arceus.service.auth.SessionService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val SERVICE_KEY_HEADER = "X-Service-Key"
private const val SERVICE_PRINCIPAL = "discord-bot"

/**
 * Not a Spring-managed [org.springframework.stereotype.Component]: WebConfig constructs and wires this
 * directly into the Spring Security filter chain so it runs exactly once, rather than also being
 * picked up by Spring Boot's generic Filter auto-registration.
 */
class JwtAuthenticationFilter(
    private val sessionService: SessionService,
    private val botServiceKey: String,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val serviceKey = request.getHeader(SERVICE_KEY_HEADER)
        val authHeader = request.getHeader("Authorization")

        if (serviceKey != null && MessageDigest.isEqual(serviceKey.toByteArray(), botServiceKey.toByteArray())) {
            authenticate(SERVICE_PRINCIPAL, "ROLE_SERVICE")
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer ")
            if (sessionService.validateToken(token) && !sessionService.isSessionBlacklisted(token)) {
                val userId = sessionService.extractUserIdFromToken(token)
                val role = sessionService.extractRoleFromToken(token)
                authenticate(userId.toString(), "ROLE_${role.name}")
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticate(
        principal: String,
        authority: String,
    ) {
        val auth = UsernamePasswordAuthenticationToken(principal, null, listOf(SimpleGrantedAuthority(authority)))
        SecurityContextHolder.getContext().authentication = auth
    }
}
