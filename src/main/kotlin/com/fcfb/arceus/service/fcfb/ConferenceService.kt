package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Conference
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.util.InvalidConferenceException
import org.springframework.stereotype.Service

@Service
class ConferenceService(
    private val conferenceRepository: ConferenceRepository,
) {
    fun getAll(): List<Conference> = conferenceRepository.findAllByOrderByDisplayOrderAsc()

    fun getActive(): List<Conference> = conferenceRepository.findAllByActiveTrueOrderByDisplayOrderAsc()

    fun requireExists(code: String): Conference =
        conferenceRepository.findById(code).orElseThrow {
            InvalidConferenceException("Unknown conference: $code")
        }

    fun create(
        code: String,
        label: String,
        logoUrl: String?,
        logoUrlDark: String?,
        displayOrder: Int,
    ): Conference {
        if (conferenceRepository.existsById(code)) {
            throw InvalidConferenceException("Conference $code already exists")
        }
        return conferenceRepository.save(Conference(code, label, logoUrl, logoUrlDark, true, displayOrder))
    }

    fun update(
        code: String,
        label: String,
        logoUrl: String?,
        logoUrlDark: String?,
        displayOrder: Int,
    ): Conference {
        val conference = requireExists(code)
        conference.label = label
        conference.logoUrl = logoUrl
        conference.logoUrlDark = logoUrlDark
        conference.displayOrder = displayOrder
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
}
