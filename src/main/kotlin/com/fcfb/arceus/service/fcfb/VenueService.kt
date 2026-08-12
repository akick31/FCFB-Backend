package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Venue
import com.fcfb.arceus.repositories.VenueRepository
import org.springframework.stereotype.Service

@Service
class VenueService(
    private val venueRepository: VenueRepository,
) {
    fun getAllVenues(): List<Venue> = venueRepository.findAllByOrderByNameAsc()

    fun findOrCreateByName(name: String): Venue {
        val trimmed = name.trim()
        return venueRepository.findById(trimmed).orElseGet { venueRepository.save(Venue(trimmed)) }
    }

    fun deleteByName(name: String) = venueRepository.deleteById(name.trim())
}
