package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.model.appearance.Venue
import com.fcfb.arceus.repositories.appearance.VenueRepository
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
