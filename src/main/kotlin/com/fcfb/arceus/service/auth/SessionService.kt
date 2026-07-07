package com.fcfb.arceus.service.auth

import com.fcfb.arceus.repositories.SessionRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date

@Service
class SessionService(
    private val sessionRepository: SessionRepository,
    @Value("\${jwt.secret}") private val secretKey: String,
) {
    fun blacklistUserSession(token: String) {
        val userId = extractUserIdFromToken(token)
        val expirationDate = extractExpirationDateFromToken(token)
        sessionRepository.blacklistUserSession(token, userId, expirationDate)
    }

    fun isSessionBlacklisted(token: String) = sessionRepository.isSessionBlacklisted(token)

    fun clearExpiredTokens() = sessionRepository.clearExpiredTokens()

    fun generateToken(userId: Long): String {
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 3600 * 1000))
            .signWith(Keys.hmacShaKeyFor(secretKey.toByteArray()))
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims =
                Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.toByteArray()))
                    .build()
                    .parseClaimsJws(token)
            !claims.body.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    private fun extractUserIdFromToken(token: String): Long {
        val claims =
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.toByteArray()))
                .build()
                .parseClaimsJws(token)
        return claims.body.subject.toLong() // The subject contains the userId
    }

    private fun extractExpirationDateFromToken(token: String): Date {
        val claims =
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.toByteArray()))
                .build()
                .parseClaimsJws(token)
        return claims.body.expiration // Extracts the expiration date from the token
    }
}
