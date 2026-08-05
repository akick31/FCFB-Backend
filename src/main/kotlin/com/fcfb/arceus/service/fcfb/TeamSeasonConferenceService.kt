package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.TeamSeasonConference
import com.fcfb.arceus.repositories.TeamSeasonConferenceRepository
import org.springframework.stereotype.Service

@Service
class TeamSeasonConferenceService(
    private val teamSeasonConferenceRepository: TeamSeasonConferenceRepository,
    private val teamService: TeamService,
) {
    fun snapshotSeason(seasonNumber: Int) {
        teamService.getAllTeams().forEach { team ->
            val teamName = team.name ?: return@forEach
            val existing = teamSeasonConferenceRepository.findByTeamAndSeasonNumber(teamName, seasonNumber)
            if (existing != null) {
                existing.conference = team.conference
                teamSeasonConferenceRepository.save(existing)
            } else {
                teamSeasonConferenceRepository.save(TeamSeasonConference(teamName, seasonNumber, team.conference))
            }
        }
    }

    fun getConference(
        team: String,
        seasonNumber: Int,
    ): String? = teamSeasonConferenceRepository.findByTeamAndSeasonNumber(team, seasonNumber)?.conference

    fun getConferencesForSeason(seasonNumber: Int): Map<String, String?> =
        teamSeasonConferenceRepository.findBySeasonNumber(seasonNumber).associate { it.team to it.conference }
}
