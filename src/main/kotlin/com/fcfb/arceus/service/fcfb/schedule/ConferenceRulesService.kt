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

        rules.protectedRivalries =
            if (request.protectedRivalries.isNotEmpty()) {
                objectMapper.writeValueAsString(request.protectedRivalries)
            } else {
                null
            }
        rules.divisions =
            if (request.divisions.isNotEmpty()) {
                objectMapper.writeValueAsString(request.divisions)
            } else {
                null
            }

        conferenceRulesRepository.save(rules)
        Logger.info(
            "Saved conference rules for $conference: " +
                "${request.numConferenceGames} games, ${request.protectedRivalries.size} rivalries, ${request.divisions.size} divisions",
        )

        return ConferenceRulesResponse(
            conference = request.conference,
            numConferenceGames = rules.numConferenceGames,
            protectedRivalries = request.protectedRivalries,
            divisions = request.divisions,
        )
    }

    fun getConferenceRules(conference: String): ConferenceRulesResponse? {
        val rules = conferenceRulesRepository.findByConference(conference) ?: return null

        return ConferenceRulesResponse(
            conference = rules.conference,
            numConferenceGames = rules.numConferenceGames,
            protectedRivalries = deserializeRivalries(conference, rules.protectedRivalries),
            divisions = deserializeDivisions(conference, rules.divisions),
        )
    }

    private fun deserializeRivalries(
        conference: String,
        json: String?,
    ): List<ProtectedRivalry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            objectMapper.readValue(json, Array<ProtectedRivalry>::class.java).toList()
        } catch (e: Exception) {
            Logger.error("Error deserializing protected rivalries for $conference: ${e.message}", e)
            emptyList()
        }
    }

    private fun deserializeDivisions(
        conference: String,
        json: String?,
    ): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            objectMapper.readValue(json, Array<String>::class.java).toList()
        } catch (e: Exception) {
            Logger.error("Error deserializing divisions for $conference: ${e.message}", e)
            emptyList()
        }
    }
}
