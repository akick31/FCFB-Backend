package com.fcfb.arceus.model.game

import com.fcfb.arceus.enums.system.Platform
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "platform_transport")
@TypeDef(name = "json", typeClass = JsonStringType::class)
class PlatformTransport(
    @Id
    @Column(name = "game_id")
    var gameId: Int = 0,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "home_platform", nullable = false)
    var homePlatform: Platform = Platform.DISCORD,
    @Basic
    @Column(name = "home_platform_id")
    var homePlatformId: String? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "away_platform", nullable = false)
    var awayPlatform: Platform = Platform.DISCORD,
    @Basic
    @Column(name = "away_platform_id")
    var awayPlatformId: String? = null,
    @Type(type = "json")
    @Column(name = "request_message_id", columnDefinition = "longtext")
    var requestMessageId: List<String>? = listOf(),
    @Basic
    @Column(name = "last_message_timestamp", columnDefinition = "datetime")
    var lastMessageTimestamp: String? = null,
)
