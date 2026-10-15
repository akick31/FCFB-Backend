package com.fcfb.arceus.repositories.schedule

import com.fcfb.arceus.model.schedule.ConferenceRules
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceRulesRepository : CrudRepository<ConferenceRules, Int> {
    fun findByConference(conference: String): ConferenceRules?

    fun existsByConference(conference: String): Boolean
}
