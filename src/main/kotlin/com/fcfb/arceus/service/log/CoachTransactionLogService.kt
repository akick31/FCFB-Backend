package com.fcfb.arceus.service.log

import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.UsernameHistoryRepository
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.UserForbiddenException
import org.springframework.stereotype.Component

@Component
class CoachTransactionLogService(
    private val coachTransactionLogRepository: CoachTransactionLogRepository,
    private val userRepository: UserRepository,
    private val usernameHistoryRepository: UsernameHistoryRepository,
) {
    fun logCoachTransaction(transaction: CoachTransactionLog) = coachTransactionLogRepository.save(transaction)

    fun getEntireCoachTransactionLog() = coachTransactionLogRepository.getEntireCoachTransactionLog()

    fun backfillDiscordIds(): Int {
        if (!AuthContext.isAdmin()) throw UserForbiddenException()

        var updated = 0
        for (entry in coachTransactionLogRepository.getEntireCoachTransactionLog()) {
            val discordIds = (entry.coach ?: emptyList()).mapNotNull { resolveDiscordId(it) }.distinct()
            if (discordIds.isNotEmpty()) {
                entry.coachDiscordIds = discordIds.toMutableList()
                coachTransactionLogRepository.save(entry)
                updated++
            }
        }
        return updated
    }

    private fun resolveDiscordId(username: String): String? {
        userRepository.findByUsername(username)?.discordId?.let { return it }
        val historicalUserId = usernameHistoryRepository.findByUsername(username).firstOrNull()?.userId ?: return null
        return userRepository.getById(historicalUserId)?.discordId
    }
}
