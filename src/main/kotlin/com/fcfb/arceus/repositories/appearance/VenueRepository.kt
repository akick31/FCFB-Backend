package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.Venue
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface VenueRepository : CrudRepository<Venue, String> {
    fun findAllByOrderByNameAsc(): List<Venue>
}
