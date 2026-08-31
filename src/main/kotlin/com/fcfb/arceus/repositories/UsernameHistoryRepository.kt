package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.UsernameHistory
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UsernameHistoryRepository : CrudRepository<UsernameHistory, Long> {
    fun findByUserId(userId: Long): List<UsernameHistory>

    fun findByUsername(username: String): List<UsernameHistory>
}
