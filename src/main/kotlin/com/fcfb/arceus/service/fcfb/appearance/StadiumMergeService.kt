package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.StadiumMergeRequest
import com.fcfb.arceus.dto.response.StadiumResponse
import com.fcfb.arceus.enums.animation.StadiumChangeAction
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.model.appearance.StadiumSnapshot
import com.fcfb.arceus.model.appearance.Venue
import com.fcfb.arceus.repositories.appearance.VenueRepository
import com.fcfb.arceus.util.AuthContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StadiumMergeService(
    private val venueRepository: VenueRepository,
    private val venueReferenceService: VenueReferenceService,
    private val stadiumService: StadiumService,
    private val stadiumChangeLogService: StadiumChangeLogService,
) {
    @Transactional
    fun merge(request: StadiumMergeRequest): StadiumResponse {
        AuthContext().requireAdminOrCommissioner()
        val source = venue(request.source)
        val target = venue(request.into)
        if (source.name == target.name) throw InvalidUniformException("A stadium cannot be merged into itself")
        val before = StadiumSnapshot.of(target)
        venueReferenceService.repoint(source.name, target.name)
        fillMissingLocation(target, source)
        venueRepository.save(target)
        stadiumChangeLogService.record(
            target.name,
            StadiumChangeAction.MERGE,
            before,
            StadiumSnapshot.of(target),
            "Merged ${source.name} into this stadium",
        )
        venueRepository.delete(source)
        stadiumService.evictThumbnails(target.name)
        return stadiumService.get(target.name)
    }

    private fun fillMissingLocation(
        target: Venue,
        source: Venue,
    ) {
        if (target.city.isNullOrBlank() && target.state.isNullOrBlank()) {
            target.city = source.city
            target.state = source.state
            target.country = source.country
        }
        if (target.capacity == null) target.capacity = source.capacity
    }

    private fun venue(name: String): Venue =
        venueRepository.findById(name.trim()).orElseThrow { InvalidUniformException("No stadium named $name") }
}
