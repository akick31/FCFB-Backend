package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.PostseasonFieldRequest
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.model.appearance.ConferenceChampionshipField
import com.fcfb.arceus.repositories.appearance.ConferenceChampionshipFieldRepository
import com.fcfb.arceus.repositories.conference.ConferenceRepository
import org.springframework.stereotype.Service

@Service
class ConferenceChampionshipFieldService(
    private val conferenceChampionshipFieldRepository: ConferenceChampionshipFieldRepository,
    private val conferenceRepository: ConferenceRepository,
    private val postseasonFieldUpdateService: PostseasonFieldUpdateService,
) {
    fun getAll(): List<ConferenceChampionshipField> = conferenceChampionshipFieldRepository.findAll().toList()

    fun getField(conference: String): ConferenceChampionshipField =
        conferenceChampionshipFieldRepository.findById(conference).orElseGet { newField(conference) }

    fun updateField(
        conference: String,
        request: PostseasonFieldRequest,
    ): ConferenceChampionshipField =
        conferenceChampionshipFieldRepository.save(postseasonFieldUpdateService.applyTo(getField(conference), request))

    private fun newField(conference: String): ConferenceChampionshipField {
        val existing =
            conferenceRepository.findById(
                conference,
            ).orElseThrow { InvalidUniformException("No conference with code $conference") }
        return ConferenceChampionshipField().apply { this.conference = existing.code }
    }
}
