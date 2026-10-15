package com.fcfb.arceus.repositories.game

import com.fcfb.arceus.enums.notification.NotificationStatus
import com.fcfb.arceus.enums.notification.NotificationType
import com.fcfb.arceus.model.game.NotificationOutbox
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationOutboxRepository : CrudRepository<NotificationOutbox, Long> {
    fun findByTypeAndStatus(
        type: NotificationType,
        status: NotificationStatus,
    ): List<NotificationOutbox>

    fun findByGameIdAndType(
        gameId: Int,
        type: NotificationType,
    ): NotificationOutbox?

    fun deleteByGameIdAndType(
        gameId: Int,
        type: NotificationType,
    )
}
