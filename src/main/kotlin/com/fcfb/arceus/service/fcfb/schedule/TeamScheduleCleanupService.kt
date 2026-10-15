package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.repositories.schedule.ScheduleRepository
import com.fcfb.arceus.repositories.season.SeasonRepository
import com.fcfb.arceus.service.fcfb.season.OffseasonService
import org.springframework.stereotype.Service

@Service
class TeamScheduleCleanupService(
    private val scheduleRepository: ScheduleRepository,
    private val seasonRepository: SeasonRepository,
    private val offseasonService: OffseasonService,
) {
    fun removeTeamFromUpcomingSchedule(team: String) {
        val season = targetSeason() ?: return
        scheduleRepository.deleteUnplayedScheduleBySeasonAndTeam(season, team)
    }

    private fun targetSeason(): Int? {
        val inOffseason = offseasonService.getCurrentOffseason() != null
        val season = if (inOffseason) seasonRepository.getPendingSeason() else seasonRepository.getCurrentSeason()
        return season?.seasonNumber
    }
}
