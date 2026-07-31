package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.Conference
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceRepository : CrudRepository<Conference, String> {
    fun findAllByOrderByDisplayOrderAsc(): List<Conference>

    fun findAllByActiveTrueOrderByDisplayOrderAsc(): List<Conference>
}
