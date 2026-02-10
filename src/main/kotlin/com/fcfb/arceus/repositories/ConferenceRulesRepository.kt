package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.model.ConferenceRules
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceRulesRepository : CrudRepository<ConferenceRules, Int> {
    @Query(value = "SELECT * FROM conference_rules WHERE conference = ?", nativeQuery = true)
    fun findByConference(conference: Conference): ConferenceRules?

    fun existsByConference(conference: Conference): Boolean
}
