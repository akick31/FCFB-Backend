package com.fcfb.arceus.repositories.log

import com.fcfb.arceus.model.log.UsernameHistory
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UsernameHistoryRepository : CrudRepository<UsernameHistory, Long> {
    fun findByUserId(userId: Long): List<UsernameHistory>

    fun findByUsername(username: String): List<UsernameHistory>
}
