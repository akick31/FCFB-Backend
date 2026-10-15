package com.fcfb.arceus.repositories.log

import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.log.CoachTransactionLog
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CoachTransactionLogRepository : CrudRepository<CoachTransactionLog, Int> {
    @Query("SELECT * FROM coach_transaction_log", nativeQuery = true)
    fun getEntireCoachTransactionLog(): List<CoachTransactionLog>

    fun findByTeam(team: String): List<CoachTransactionLog>

    fun findByTransaction(transaction: TransactionType): List<CoachTransactionLog>

    fun findByPosition(position: CoachPosition): List<CoachTransactionLog>

    fun findByProcessedBy(processedBy: String): List<CoachTransactionLog>
}
