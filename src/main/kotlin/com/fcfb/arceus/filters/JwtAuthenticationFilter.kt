package com.fcfb.arceus.filters

import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.service.auth.SessionService
import com.fcfb.arceus.util.EncryptionUtils
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val SERVICE_KEY_HEADER = "X-Service-Key"
private const val API_KEY_HEADER = "X-Api-Key"
private const val SERVICE_PRINCIPAL = "discord-bot"
private const val WEBSITE_PRINCIPAL = "website"

class JwtAuthenticationFilter(
    private val sessionService: SessionService,
    private val botServiceKey: String,
    private val websiteServiceKey: String,
    private val userRepository: UserRepository,
    private val encryptionUtils: EncryptionUtils,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val apiKey = request.getHeader(API_KEY_HEADER)
        val authHeader = request.getHeader("Authorization")
        val serviceKey = request.getHeader(SERVICE_KEY_HEADER)

        if (apiKey != null) {
            val user = userRepository.findByApiKeyHash(encryptionUtils.hash(apiKey))
            if (user != null) {
                authenticate(user.id.toString(), "ROLE_${user.role.name}")
            }
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer ")
            if (sessionService.validateToken(token) && !sessionService.isSessionBlacklisted(token)) {
                val userId = sessionService.extractUserIdFromToken(token)
                val role = sessionService.extractRoleFromToken(token)
                authenticate(userId.toString(), "ROLE_${role.name}")
            }
        } else if (serviceKey != null) {
            if (MessageDigest.isEqual(serviceKey.toByteArray(), botServiceKey.toByteArray())) {
                authenticate(SERVICE_PRINCIPAL, "ROLE_SERVICE")
            } else if (MessageDigest.isEqual(serviceKey.toByteArray(), websiteServiceKey.toByteArray())) {
                authenticate(WEBSITE_PRINCIPAL, "ROLE_WEBSITE")
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
