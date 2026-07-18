package com.fcfb.arceus.service.log

import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import org.springframework.stereotype.Component

@Component
class CoachTransactionLogService(
    private val coachTransactionLogRepository: CoachTransactionLogRepository,
) {
    fun logCoachTransaction(transaction: CoachTransactionLog) = coachTransactionLogRepository.save(transaction)

    fun getEntireCoachTransactionLog() = coachTransactionLogRepository.getEntireCoachTransactionLog()
}
