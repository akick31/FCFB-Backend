package com.fcfb.arceus.repositories.conference

import com.fcfb.arceus.model.conference.Conference
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceRepository : CrudRepository<Conference, String> {
    fun findAllByOrderByLabelAsc(): List<Conference>

    fun findAllByActiveTrueOrderByLabelAsc(): List<Conference>
}
