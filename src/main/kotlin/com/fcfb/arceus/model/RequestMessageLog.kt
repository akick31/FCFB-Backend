package com.fcfb.arceus.model

import com.fcfb.arceus.enums.system.MessageType
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.TypeDef
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
@Table(name = "request_message_log")
@TypeDef(name = "json", typeClass = JsonStringType::class)
class RequestMessageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "message_type", nullable = false)
    var messageType: MessageType? = null

    @Basic
    @Column(name = "game_id", nullable = false)
    var gameId: Int? = null

    @Basic
    @Column(name = "play_id")
    var playId: Int? = null

    @Basic
    @Column(name = "message_id", nullable = false, columnDefinition = "mediumtext")
    var messageId: Long? = null

    @Basic
    @Column(name = "message_location", nullable = false)
    var messageLocation: String? = null

    @Basic
    @Column(name = "message_ts", nullable = false)
    var messageTs: String? = null

    constructor(
        messageType: MessageType,
        gameId: Int,
        playId: Int?,
        messageId: Long,
        messageLocation: String,
        messageTs: String?,
    ) {
        this.messageType = messageType
        this.gameId = gameId
        this.playId = playId
        this.messageId = messageId
        this.messageLocation = messageLocation
        this.messageTs = messageTs
    }

    constructor()
}
