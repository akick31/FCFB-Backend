package com.fcfb.arceus.model

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "session")
data class Session(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id")
    val userId: Long,
    val token: String,
    @Column(name = "expiration_date")
    val expirationDate: LocalDateTime,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    constructor(userId: Long, token: String, expirationDate: LocalDateTime) : this(0, userId, token, expirationDate)
    constructor() : this(0, 0, "", LocalDateTime.now())
}
