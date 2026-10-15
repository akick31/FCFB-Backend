package com.fcfb.arceus.model.account

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_credential")
class UserCredential(
    @Id
    @Column(name = "user_id")
    var userId: Long = 0,
    @Basic
    @Column(name = "email", nullable = false)
    var email: String = "",
    @Basic
    @Column(name = "hashed_email", nullable = false)
    var hashedEmail: String = "",
    @Basic
    @Column(name = "password", nullable = false)
    var password: String = "",
    @Basic
    @Column(name = "salt", nullable = false)
    var salt: String = "",
    @Basic
    @Column(name = "api_key_hash")
    var apiKeyHash: String? = null,
    @Basic
    @Column(name = "reset_token")
    var resetToken: String? = null,
    @Basic
    @Column(name = "reset_token_expiration")
    var resetTokenExpiration: String? = null,
)
