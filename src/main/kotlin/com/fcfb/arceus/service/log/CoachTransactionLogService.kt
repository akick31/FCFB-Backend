package com.fcfb.arceus.service.log

import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.service.fcfb.statistics.CoachStatsService
import org.springframework.stereotype.Component

@Component
class CoachTransactionLogService(
    private val coachTransactionLogRepository: CoachTransactionLogRepository,
    private val userRepository: UserRepository,
    private val coachStatsService: CoachStatsService,
) {
    fun logCoachTransaction(transaction: CoachTransactionLog): CoachTransactionLog {
        val saved = coachTransactionLogRepository.save(transaction)
        saved.coachDiscordIds.orEmpty().forEach { discordId ->
            userRepository.getByDiscordId(discordId)?.let { coachStatsService.regenerateForCoach(it) }
        }
        return saved
    }

    fun getEntireCoachTransactionLog() = coachTransactionLogRepository.getEntireCoachTransactionLog()
}
