package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.repositories.appearance.VenueRepository
import org.springframework.stereotype.Service

@Service
class VenueReferenceService(
    private val venueRepository: VenueRepository,
) {
    fun repoint(
        oldName: String,
        newName: String,
    ) {
        venueRepository.renameTeamStadiums(oldName, newName)
        venueRepository.renameGameVenues(oldName, newName)
        venueRepository.renamePresentationVenues(oldName, newName)
        venueRepository.renameScheduleVenues(oldName, newName)
        venueRepository.renameChampionshipVenues(oldName, newName)
        venueRepository.renameBowlLastVenues(oldName, newName)
        venueRepository.renameBowlFieldVenues(oldName, newName)
        venueRepository.renamePlayoffFieldVenues(oldName, newName)
        venueRepository.renameChangeLogVenues(oldName, newName)
    }
}
