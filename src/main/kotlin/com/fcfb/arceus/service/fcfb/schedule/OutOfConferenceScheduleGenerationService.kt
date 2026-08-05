package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.dto.request.ScheduleEntry
import com.fcfb.arceus.dto.response.OocGenerationResult
import com.fcfb.arceus.dto.response.OocUnmatchedSlot
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Schedule
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

    /**
     * Fills every active, real team's remaining open weeks (1-12) with random,
     * non-repeating, cross-conference OOC opponents. Additive only — never
     * touches an already-scheduled game. Best-effort: commits whatever it can
     * match and reports any team/week slots it couldn't fill rather than
     * failing the whole run.
     */
    fun generateOutOfConferenceSchedule(season: Int): OocGenerationResult {
        if (seasonService.isScheduleLocked(season)) {
            throw IllegalStateException("Schedule for season $season is locked and cannot be modified")
        }

        val teams =
            teamRepository.findByActive(true)
                .filter { it.conference != FAKE_TEAM_CONFERENCE && !it.name.isNullOrBlank() }
        val teamByName = teams.associateBy { it.name!! }
        val teamNames = teamByName.keys.toList()

        val regularSeasonGames =
            (scheduleRepository.getScheduleBySeason(season) ?: emptyList())
                .filter { it.gameType == GameType.CONFERENCE_GAME || it.gameType == GameType.OUT_OF_CONFERENCE }

        val openWeeks = teamNames.associateWith { (1..TOTAL_WEEKS).toMutableSet() }.toMutableMap()
        val opponents = teamNames.associateWith { mutableSetOf<String>() }.toMutableMap()

        for (game in regularSeasonGames) {
            val home = game.homeTeam
            val away = game.awayTeam
            openWeeks[home]?.remove(game.week)
            openWeeks[away]?.remove(game.week)
            opponents[home]?.add(away)
            opponents[away]?.add(home)
        }

        val newEntries = mutableListOf<ScheduleEntry>()
        val unmatched = mutableListOf<OocUnmatchedSlot>()

        for (week in 1..TOTAL_WEEKS) {
            var pending = teamNames.filter { week in (openWeeks[it] ?: emptySet()) }

            for (attempt in 1..RETRIES_PER_WEEK) {
                if (pending.size < 2) break
                val shuffled = pending.shuffled()
                val matchedThisAttempt = mutableSetOf<String>()

                for (i in shuffled.indices) {
                    val t1 = shuffled[i]
                    if (t1 in matchedThisAttempt) continue
                    val team1 = teamByName[t1] ?: continue

                    val partner =
                        shuffled.drop(i + 1).firstOrNull { t2 ->
                            t2 !in matchedThisAttempt &&
                                opponents[t1]?.contains(t2) != true &&
                                teamByName[t2]?.conference != team1.conference &&
                                teamByName[t2]?.subdivision == team1.subdivision
                        }

                    if (partner != null) {
                        matchedThisAttempt.add(t1)
                        matchedThisAttempt.add(partner)
                        opponents[t1]?.add(partner)
                        opponents[partner]?.add(t1)
                        openWeeks[t1]?.remove(week)
                        openWeeks[partner]?.remove(week)

                        val (home, away) = if (listOf(true, false).random()) t1 to partner else partner to t1
                        newEntries.add(
                            ScheduleEntry(
                                season = season,
                                week = week,
                                subdivision = team1.subdivision ?: Subdivision.FBS,
                                homeTeam = home,
                                awayTeam = away,
                                gameType = GameType.OUT_OF_CONFERENCE,
                            ),
                        )
                    }
                }

                pending = pending.filterNot { it in matchedThisAttempt }
            }

            for (team in pending) {
                unmatched.add(OocUnmatchedSlot(team = team, week = week))
            }
        }

        val saved = if (newEntries.isNotEmpty()) saveScheduleEntries(newEntries) else emptyList()
        Logger.info(
            "OOC auto-fill for season $season: ${saved.size} games scheduled, ${unmatched.size} slots left unmatched",
        )

        return OocGenerationResult(gamesScheduled = saved.size, unmatchedSlots = unmatched)
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
