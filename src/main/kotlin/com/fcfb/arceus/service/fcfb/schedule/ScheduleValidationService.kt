package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.dto.response.ScheduleValidationResult
import com.fcfb.arceus.dto.response.TeamScheduleGap
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.repositories.TeamRepository
import org.springframework.stereotype.Service

@Service
class ScheduleValidationService(
    private val scheduleRepository: ScheduleRepository,
    private val teamRepository: TeamRepository,
) {
    companion object {
        private val SEASON_WEEKS = 1..12
    }

    fun validateSchedule(season: Int): ScheduleValidationResult {
        val activeTeamNames =
            teamRepository.getAllActiveTeams()
                .filter { it.conference != "FAKE_TEAM" }
                .mapNotNull { it.name }

        val weeksByTeam = mutableMapOf<String, MutableSet<Int>>()
        (scheduleRepository.getScheduleBySeason(season) ?: emptyList()).forEach { entry ->
            val week = entry.week ?: return@forEach
            if (week !in SEASON_WEEKS) return@forEach
            entry.homeTeam?.let { weeksByTeam.getOrPut(it) { mutableSetOf() }.add(week) }
            entry.awayTeam?.let { weeksByTeam.getOrPut(it) { mutableSetOf() }.add(week) }
        }

        val incompleteTeams =
            activeTeamNames
                .mapNotNull { team ->
                    val missingWeeks = SEASON_WEEKS.filter { it !in (weeksByTeam[team] ?: emptySet()) }
                    if (missingWeeks.isEmpty()) null else TeamScheduleGap(team, missingWeeks)
                }
                .sortedBy { it.team }

        return ScheduleValidationResult(valid = incompleteTeams.isEmpty(), incompleteTeams = incompleteTeams)
    }
}
