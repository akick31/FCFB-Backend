package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.ConferenceRules
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceRulesRepository : CrudRepository<ConferenceRules, Int> {
    fun findByConference(conference: String): ConferenceRules?

    fun existsByConference(conference: String): Boolean
}
