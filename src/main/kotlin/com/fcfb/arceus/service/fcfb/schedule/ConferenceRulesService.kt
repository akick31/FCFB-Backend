package com.fcfb.arceus.service.fcfb.schedule

import com.fasterxml.jackson.databind.ObjectMapper
import com.fcfb.arceus.dto.request.ConferenceRulesRequest
import com.fcfb.arceus.dto.response.ConferenceRulesResponse
import com.fcfb.arceus.dto.standard.ProtectedRivalry
import com.fcfb.arceus.model.ConferenceRules
import com.fcfb.arceus.repositories.ConferenceRulesRepository
import com.fcfb.arceus.service.fcfb.ConferenceService
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service

@Service
class ConferenceRulesService(
    private val conferenceRulesRepository: ConferenceRulesRepository,
    private val conferenceService: ConferenceService,
    private val objectMapper: ObjectMapper,
) {
    fun saveConferenceRules(request: ConferenceRulesRequest): ConferenceRulesResponse {
        val conference = conferenceService.requireExists(request.conference).code
        val existing = conferenceRulesRepository.findByConference(conference)

        val rules = existing ?: ConferenceRules()
        rules.conference = conference
        rules.numConferenceGames = request.numConferenceGames

        // Serialize protected rivalries to JSON
        val rivalriesJson =
            if (request.protectedRivalries.isNotEmpty()) {
                objectMapper.writeValueAsString(request.protectedRivalries)
            } else {
                null
            }
        rules.protectedRivalries = rivalriesJson

        conferenceRulesRepository.save(rules)
        Logger.info(
            "Saved conference rules for $conference: " +
                "${request.numConferenceGames} games, ${request.protectedRivalries.size} rivalries",
        )

        return ConferenceRulesResponse(
            conference = request.conference,
            numConferenceGames = rules.numConferenceGames,
            protectedRivalries = request.protectedRivalries,
        )
    }

    fun getConferenceRules(conference: String): ConferenceRulesResponse? {
        val rules = conferenceRulesRepository.findByConference(conference) ?: return null

        // Deserialize protected rivalries from JSON
        val protectedRivalriesJson = rules.protectedRivalries
        val rivalries =
            if (protectedRivalriesJson != null && protectedRivalriesJson.isNotBlank()) {
                try {
                    objectMapper.readValue(protectedRivalriesJson, Array<ProtectedRivalry>::class.java).toList()
                } catch (e: Exception) {
                    Logger.error("Error deserializing protected rivalries for $conference: ${e.message}", e)
                    emptyList()
                }
            } else {
                emptyList()
            }

        return ConferenceRulesResponse(
            conference = rules.conference,
            numConferenceGames = rules.numConferenceGames,
            protectedRivalries = rivalries,
        )
    }
}
