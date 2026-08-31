package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "username_history")
class UsernameHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,
    @Basic
    @Column(name = "user_id")
    var userId: Long = 0,
    @Basic
    @Column(name = "username")
    var username: String = "",
    @Basic
    @Column(name = "changed_at")
    var changedAt: String? = null,
)
