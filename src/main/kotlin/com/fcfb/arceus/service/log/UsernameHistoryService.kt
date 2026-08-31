package com.fcfb.arceus.service.log

import com.fcfb.arceus.model.UsernameHistory
import com.fcfb.arceus.repositories.UsernameHistoryRepository
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class UsernameHistoryService(
    private val usernameHistoryRepository: UsernameHistoryRepository,
) {
    fun recordUsernameChange(
        userId: Long,
        previousUsername: String,
    ) {
        usernameHistoryRepository.save(
            UsernameHistory(
                userId = userId,
                username = previousUsername,
                changedAt = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")),
            ),
        )
    }

    fun getHistoricalUsernames(userId: Long): List<String> = usernameHistoryRepository.findByUserId(userId).map { it.username }
}
