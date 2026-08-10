package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.BowlVenue
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BowlVenueRepository : CrudRepository<BowlVenue, String>
