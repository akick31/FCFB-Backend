package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.dto.request.ScheduleEntry
import com.fcfb.arceus.dto.response.OocGenerationResult
import com.fcfb.arceus.dto.response.OocUnmatchedSlot
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.fcfb.SeasonService
import com.fcfb.arceus.util.Logger
import org.springframework.stereotype.Service

@Service
class OutOfConferenceScheduleGenerationService(
    private val seasonService: SeasonService,
    private val scheduleRepository: ScheduleRepository,
    private val teamRepository: TeamRepository,
) {
    companion object {
        private const val TOTAL_WEEKS = 12
        private const val FAKE_TEAM_CONFERENCE = "FAKE_TEAM"
        private const val RETRIES_PER_WEEK = 10
    }

    fun generateOutOfConferenceSchedule(season: Int): OocGenerationResult {
        checkScheduleLock(season)

        val teams = loadEligibleTeams()
        val openWeeks = teams.keys.associateWith { (1..TOTAL_WEEKS).toMutableSet() }.toMutableMap()
        val opponents = teams.keys.associateWith { mutableSetOf<String>() }.toMutableMap()
        blockExistingRegularSeasonGames(season, openWeeks, opponents)

        val newEntries = mutableListOf<ScheduleEntry>()
        val unmatched = mutableListOf<OocUnmatchedSlot>()
        for (week in 1..TOTAL_WEEKS) {
            val unmatchedThisWeek = fillWeek(week, season, teams, openWeeks, opponents, newEntries)
            unmatchedThisWeek.forEach { unmatched.add(OocUnmatchedSlot(team = it, week = week)) }
        }

        val saved = if (newEntries.isNotEmpty()) saveScheduleEntries(newEntries) else emptyList()
        Logger.info(
            "OOC auto-fill for season $season: ${saved.size} games scheduled, ${unmatched.size} slots left unmatched",
        )
        return OocGenerationResult(gamesScheduled = saved.size, unmatchedSlots = unmatched)
    }

    private fun checkScheduleLock(season: Int) {
        if (seasonService.isScheduleLocked(season)) {
            throw IllegalStateException("Schedule for season $season is locked and cannot be modified")
        }
    }

    private fun loadEligibleTeams(): Map<String, Team> =
        teamRepository.findByActive(true)
            .filter { it.conference != FAKE_TEAM_CONFERENCE && !it.name.isNullOrBlank() }
            .associateBy { it.name!! }

    private fun blockExistingRegularSeasonGames(
        season: Int,
        openWeeks: MutableMap<String, MutableSet<Int>>,
        opponents: MutableMap<String, MutableSet<String>>,
    ) {
        val regularSeasonGames =
            (scheduleRepository.getScheduleBySeason(season) ?: emptyList())
                .filter { it.gameType == GameType.CONFERENCE_GAME || it.gameType == GameType.OUT_OF_CONFERENCE }
        for (game in regularSeasonGames) {
            val home = game.homeTeam
            val away = game.awayTeam
            openWeeks[home]?.remove(game.week)
            openWeeks[away]?.remove(game.week)
            opponents[home]?.add(away)
            opponents[away]?.add(home)
        }
    }

    private fun fillWeek(
        week: Int,
        season: Int,
        teams: Map<String, Team>,
        openWeeks: MutableMap<String, MutableSet<Int>>,
        opponents: MutableMap<String, MutableSet<String>>,
        newEntries: MutableList<ScheduleEntry>,
    ): List<String> {
        var pending = teams.keys.filter { week in (openWeeks[it] ?: emptySet()) }
        for (attempt in 1..RETRIES_PER_WEEK) {
            if (pending.size < 2) break
            val matched = matchTeamsForWeek(pending, teams, opponents)
            for ((t1, t2) in matched) {
                opponents[t1]?.add(t2)
                opponents[t2]?.add(t1)
                openWeeks[t1]?.remove(week)
                openWeeks[t2]?.remove(week)
                newEntries.add(buildOocEntry(season, week, teams.getValue(t1), t1, t2))
            }
            val matchedTeams = matched.flatMap { listOf(it.first, it.second) }.toSet()
            pending = pending.filterNot { it in matchedTeams }
        }
        return pending
    }

    private fun matchTeamsForWeek(
        pending: List<String>,
        teams: Map<String, Team>,
        opponents: Map<String, Set<String>>,
    ): List<Pair<String, String>> {
        val shuffled = pending.shuffled()
        val matched = mutableSetOf<String>()
        val pairs = mutableListOf<Pair<String, String>>()
        for (i in shuffled.indices) {
            val t1 = shuffled[i]
            if (t1 in matched) continue
            val team1 = teams[t1] ?: continue
            val partner =
                shuffled.drop(i + 1).firstOrNull { t2 ->
                    t2 !in matched &&
                        opponents[t1]?.contains(t2) != true &&
                        teams[t2]?.conference != team1.conference &&
                        teams[t2]?.subdivision == team1.subdivision
                }
            if (partner != null) {
                matched.add(t1)
                matched.add(partner)
                pairs.add(t1 to partner)
            }
        }
        return pairs
    }

    private fun buildOocEntry(
        season: Int,
        week: Int,
        team1: Team,
        t1: String,
        t2: String,
    ): ScheduleEntry {
        val (home, away) = if (listOf(true, false).random()) t1 to t2 else t2 to t1
        return ScheduleEntry(
            season = season,
            week = week,
            subdivision = team1.subdivision ?: Subdivision.FBS,
            homeTeam = home,
            awayTeam = away,
            gameType = GameType.OUT_OF_CONFERENCE,
        )
    }

    private fun saveScheduleEntries(entries: List<ScheduleEntry>): List<Schedule> {
        val schedules =
            entries.map { entry ->
                val schedule = Schedule()
                schedule.season = entry.season
                schedule.week = entry.week
                schedule.subdivision = entry.subdivision
                schedule.homeTeam = entry.homeTeam
                schedule.awayTeam = entry.awayTeam
                schedule.tvChannel = entry.tvChannel
                schedule.gameType = entry.gameType
                schedule.started = false
                schedule.finished = false
                schedule
            }
        return scheduleRepository.saveAll(schedules).toList()
    }
}
