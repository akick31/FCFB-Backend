package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.Session
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
interface SessionRepository : JpaRepository<Session, Long> {
    fun deleteByToken(token: String)

    @Query("INSERT INTO session (token, user_id, expiration_date) VALUES (?, ?, ?)", nativeQuery = true)
    fun blacklistUserSession(
        token: String,
        userId: Long,
        expirationDate: Date,
    )

    @Query("SELECT EXISTS(SELECT 1 FROM session WHERE token = ?)", nativeQuery = true)
    fun isSessionBlacklisted(token: String): Int

    @Query("DELETE FROM session WHERE expiration_date < NOW()", nativeQuery = true)
    fun clearExpiredTokens()

    fun findByToken(token: String): Session?

    fun findByUserId(userId: Long): List<Session>

    fun findByExpirationDate(expirationDate: java.time.LocalDateTime): List<Session>
}
