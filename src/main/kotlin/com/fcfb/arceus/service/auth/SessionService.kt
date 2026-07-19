package com.fcfb.arceus.service.auth

import com.fcfb.arceus.enums.user.UserRole
import com.fcfb.arceus.repositories.SessionRepository
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

private const val ROLE_CLAIM = "role"

@Service
class SessionService(
    private val sessionRepository: SessionRepository,
    @Value("\${jwt.secret}") private val secretKey: String,
) {
    fun blacklistUserSession(token: String) {
        val claims = parseClaims(token)
        sessionRepository.blacklistUserSession(token, claims.subject.toLong(), claims.expiration)
    }

    fun isSessionBlacklisted(token: String) = sessionRepository.isSessionBlacklisted(token)

    fun clearExpiredTokens() = sessionRepository.clearExpiredTokens()

    fun generateToken(
        userId: Long,
        role: UserRole,
    ): String {
        return Jwts.builder()
            .setSubject(userId.toString())
            .claim(ROLE_CLAIM, role.name)
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 3600 * 1000))
            .signWith(Keys.hmacShaKeyFor(secretKey.toByteArray()))
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extractUserIdFromToken(token: String): Long = parseClaims(token).subject.toLong()

    fun extractRoleFromToken(token: String): UserRole = UserRole.valueOf(parseClaims(token)[ROLE_CLAIM] as String)

    private fun parseClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(secretKey.toByteArray()))
            .build()
            .parseClaimsJws(token)
            .body
    }
}
