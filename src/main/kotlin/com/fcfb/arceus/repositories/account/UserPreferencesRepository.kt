package com.fcfb.arceus.repositories.account

import com.fcfb.arceus.model.account.UserPreferences
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserPreferencesRepository : CrudRepository<UserPreferences, Long>
