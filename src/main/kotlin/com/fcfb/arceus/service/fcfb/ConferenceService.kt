package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Conference
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.util.InvalidConferenceException
import org.springframework.stereotype.Service

@Service
class ConferenceService(
    private val conferenceRepository: ConferenceRepository,
) {
    fun getAll(): List<Conference> = conferenceRepository.findAllByOrderByLabelAsc()

    fun getActive(): List<Conference> = conferenceRepository.findAllByActiveTrueOrderByLabelAsc()

    fun requireExists(code: String): Conference =
        conferenceRepository.findById(code).orElseThrow {
            InvalidConferenceException("Unknown conference: $code")
        }

    fun create(
        code: String,
        label: String,
        logoUrl: String?,
        logoUrlDark: String?,
        abbreviation: String?,
    ): Conference {
        if (conferenceRepository.existsById(code)) {
            throw InvalidConferenceException("Conference $code already exists")
        }
        return conferenceRepository.save(Conference(code, label, logoUrl, logoUrlDark, abbreviation, true))
    }

    fun update(
        code: String,
        label: String,
        logoUrl: String?,
        logoUrlDark: String?,
        abbreviation: String?,
    ): Conference {
        val conference = requireExists(code)
        conference.label = label
        conference.logoUrl = logoUrl
        conference.logoUrlDark = logoUrlDark
        conference.abbreviation = abbreviation
        return conferenceRepository.save(conference)
    }

    fun setActive(
        code: String,
        active: Boolean,
    ): Conference {
        val conference = requireExists(code)
        conference.active = active
        return conferenceRepository.save(conference)
    }

    fun updateChampionshipVenue(
        code: String,
        venue: String,
    ): Conference {
        val conference = requireExists(code)
        conference.championshipVenue = venue
        return conferenceRepository.save(conference)
    }
}
