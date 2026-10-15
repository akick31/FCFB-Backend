package com.fcfb.arceus.model.game

import com.fcfb.arceus.enums.notification.NotificationStatus
import com.fcfb.arceus.enums.notification.NotificationType
import java.time.LocalDateTime
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "notification_outbox")
class NotificationOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @Basic
    @Column(name = "game_id")
    var gameId: Int = 0

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "type")
    lateinit var type: NotificationType

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "status")
    var status: NotificationStatus = NotificationStatus.PENDING

    @Basic
    @Column(name = "payload")
    var payload: String? = null

    @Basic
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Basic
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()

    constructor()

    constructor(gameId: Int, type: NotificationType, payload: String?) {
        this.gameId = gameId
        this.type = type
        this.payload = payload
    }
}
