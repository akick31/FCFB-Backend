package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.Venue
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VenueRepository : CrudRepository<Venue, String> {
    fun findAllByOrderByNameAsc(): List<Venue>
}
