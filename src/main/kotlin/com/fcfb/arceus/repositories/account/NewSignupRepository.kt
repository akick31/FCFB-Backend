package com.fcfb.arceus.repositories.account

import com.fcfb.arceus.model.account.NewSignup
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface NewSignupRepository : CrudRepository<NewSignup, Long> {
    fun getById(id: Long): NewSignup?

    @Query("SELECT * FROM new_signup", nativeQuery = true)
    fun getNewSignups(): List<NewSignup>

    fun getByDiscordId(discordId: String): NewSignup?

    fun getByVerificationToken(token: String): NewSignup?

    fun findByUsername(username: String): NewSignup?

    fun findByEmail(email: String): NewSignup?

    fun findByVerificationToken(verificationToken: String): NewSignup?

    fun findByApproved(approved: Boolean): List<NewSignup>
}
