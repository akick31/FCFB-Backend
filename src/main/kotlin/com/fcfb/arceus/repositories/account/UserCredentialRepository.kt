package com.fcfb.arceus.repositories.account

import com.fcfb.arceus.model.account.UserCredential
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserCredentialRepository : CrudRepository<UserCredential, Long>
