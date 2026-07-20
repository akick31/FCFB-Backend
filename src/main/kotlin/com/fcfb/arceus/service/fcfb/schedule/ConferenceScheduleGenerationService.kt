package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.dto.request.ConferenceScheduleRequest
import com.fcfb.arceus.dto.request.ScheduleEntry
import com.fcfb.arceus.dto.response.ScheduleGenJob
import com.fcfb.arceus.dto.response.ScheduleGenJobResponse
import com.fcfb.arceus.dto.response.ScheduleGenJobStatus
import com.fcfb.arceus.dto.response.ScheduleGenLog
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.service.fcfb.ScheduleService
import com.fcfb.arceus.service.fcfb.SeasonService
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Generates and manages conference schedules.
 *
 * [scheduleService] is injected lazily to break the circular dependency
 * between this class and [ScheduleService] (which delegates schedule
 * generation here but is itself needed here to create the resulting
 * schedule entries via [ScheduleService.createBulkScheduleEntries]).
 */
@Service
class ConferenceScheduleGenerationService(
    private val seasonService: SeasonService,
    private val scheduleRepository: ScheduleRepository,
    private val teamService: TeamService,
    @Lazy private val scheduleService: ScheduleService,
) {
    companion object {
        private val activeGenJobs = ConcurrentHashMap<String, ScheduleGenJob>()
    }

    /**
     * Check if the schedule is locked for a given season and throw if locked
     * @param season
     */
    private fun checkScheduleLock(season: Int) {
        if (seasonService.isScheduleLocked(season)) {
            throw IllegalStateException("Schedule for season $season is locked and cannot be modified")
        }
    }

    /**
     * Generate conference schedule automatically.
     *
     * Uses two strategies to GUARANTEE a complete schedule:
     *   1. **Circle method** (even team counts): generates mathematically guaranteed
     *      round-robin rounds via the polygon/circle algorithm, then selects
     *      [numGames] rounds. Every team plays exactly [numGames] games.
     *   2. **Greedy + retry** (odd team counts): greedy matchup selection with
     *      random reshuffling — retries up to 20 times to get a valid set.
     *
     * Week assignment uses **MRV + backtracking** (with dynamic variable ordering)
     * so that every matchup is guaranteed to land in a valid week if a valid
     * assignment exists.
     *
     * @param request  conference schedule configuration
     * @param conferenceTeams teams in the conference
     */
    fun generateConferenceSchedule(
        request: ConferenceScheduleRequest,
        conferenceTeams: List<Team>,
    ): List<Schedule> {
        checkScheduleLock(request.season)
        val teamNames = conferenceTeams.map { it.name ?: "" }.filter { it.isNotEmpty() }
        val numTeams = teamNames.size
        val numGames = minOf(request.numConferenceGames, numTeams - 1)
        val protectedRivalries = request.protectedRivalries
        val totalWeeks = 12

        Logger.info("Generating conference schedule for ${request.conference}: $numTeams teams, $numGames games per team")

        if (numTeams < 2) {
            Logger.warn("Not enough teams ($numTeams) to generate schedule for ${request.conference}")
            return emptyList()
        }

        // ── Delete existing conference games for this season/conference ──
        val existingSchedule = scheduleRepository.getScheduleBySeason(request.season) ?: emptyList()
        existingSchedule.filter { s ->
            s.gameType == GameType.CONFERENCE_GAME &&
                teamNames.contains(s.homeTeam) &&
                teamNames.contains(s.awayTeam)
        }.forEach { scheduleRepository.delete(it) }

        // ── Pre-populate week constraints from remaining (non-conference) games ──
        val teamWeekSchedule = teamNames.associateWith { mutableSetOf<Int>() }.toMutableMap()
        for (team in teamNames) {
            scheduleRepository.getScheduleBySeasonAndTeam(request.season, team)?.forEach { g ->
                if (g.week != null) teamWeekSchedule[team]?.add(g.week!!)
            }
        }

        // ── Step 1: Generate matchups ──
        val matchups: List<Triple<String, String, Int?>> // team1, team2, forcedWeek
        if (numTeams % 2 == 0) {
            matchups = selectMatchupsCircleMethod(teamNames, numGames, protectedRivalries)
            Logger.info("Circle method selected ${matchups.size} matchups for ${request.conference}")
        } else {
            matchups = selectMatchupsGreedy(teamNames, numGames, protectedRivalries)
            Logger.info("Greedy method selected ${matchups.size} matchups for ${request.conference}")
        }

        // ── Step 2: Assign weeks with backtracking ──
        val weekAssignments =
            assignMatchupsToWeeks(matchups, teamWeekSchedule, request.startWeek, totalWeeks)
                ?: throw IllegalStateException(
                    "Could not assign weeks for ${request.conference} conference schedule. " +
                        "Check for conflicting OOC games or reduce conference game count.",
                )

        Logger.info("Assigned ${weekAssignments.size} matchups to weeks for ${request.conference}")

        // ── Step 3: Determine home/away using previous season history ──
        // Look up the most recent season(s) to alternate home/away.
        // "prevHomeTeam" maps a sorted matchup key ("TeamA|TeamB") -> who was home last time
        val prevHomeTeam = buildPreviousSeasonHomeAwayMap(request.season, teamNames)

        val teamHomeGames = teamNames.associateWith { 0 }.toMutableMap()
        val teamAwayGames = teamNames.associateWith { 0 }.toMutableMap()

        val entries =
            weekAssignments.map { (matchup, week) ->
                val t1 = matchup.first
                val t2 = matchup.second
                val sortedKey = listOf(t1, t2).sorted().joinToString("|")

                val (home, away) =
                    if (sortedKey in prevHomeTeam) {
                        // Flip from last season: if t1 was home last time, make t1 away
                        val prevHome = prevHomeTeam[sortedKey]!!
                        if (prevHome == t1) t2 to t1 else t1 to t2
                    } else {
                        // No history — fall back to balance-based assignment
                        val h1 = teamHomeGames[t1] ?: 0
                        val a1 = teamAwayGames[t1] ?: 0
                        val h2 = teamHomeGames[t2] ?: 0
                        val a2 = teamAwayGames[t2] ?: 0
                        when {
                            h1 <= a1 && h2 >= a2 -> t1 to t2
                            h2 <= a2 && h1 >= a1 -> t2 to t1
                            h1 <= h2 -> t1 to t2
                            else -> t2 to t1
                        }
                    }
                teamHomeGames[home] = (teamHomeGames[home] ?: 0) + 1
                teamAwayGames[away] = (teamAwayGames[away] ?: 0) + 1

                ScheduleEntry(
                    season = request.season,
                    week = week,
                    subdivision = request.subdivision,
                    homeTeam = home,
                    awayTeam = away,
                    gameType = GameType.CONFERENCE_GAME,
                )
            }

        val result = scheduleService.createBulkScheduleEntries(entries)
        Logger.info("Generated ${result.size} conference games for ${request.conference}")
        return result
    }

    // Matchup Selection Helpers

    /**
     * Circle method: guaranteed to select exactly [numGames] matchups per team
     * for an **even** number of teams.
     *
     * Generates all (n-1) round-robin rounds via the polygon algorithm, then
     * selects [numGames] rounds — keeping any rounds that contain protected
     * rivalries.
     */
    private fun selectMatchupsCircleMethod(
        teamNames: List<String>,
        numGames: Int,
        protectedRivalries: List<com.fcfb.arceus.dto.standard.ProtectedRivalry>,
    ): List<Triple<String, String, Int?>> {
        val allRounds = generateCircleMethodRounds(teamNames)

        // Find rounds that contain protected rivalries — they must be kept
        val requiredRoundIdxs = mutableSetOf<Int>()
        for (rivalry in protectedRivalries) {
            if (rivalry.team1.isBlank() || rivalry.team2.isBlank()) continue
            for (roundIdx in allRounds.indices) {
                if (allRounds[roundIdx].any { (t1, t2) ->
                        (t1 == rivalry.team1 && t2 == rivalry.team2) ||
                            (t1 == rivalry.team2 && t2 == rivalry.team1)
                    }
                ) {
                    requiredRoundIdxs.add(roundIdx)
                    break
                }
            }
        }

        // Fill remaining round slots randomly
        val selectedRoundIdxs = requiredRoundIdxs.toMutableSet()
        val candidateIdxs = (allRounds.indices.toList() - requiredRoundIdxs).shuffled()
        for (idx in candidateIdxs) {
            if (selectedRoundIdxs.size >= numGames) break
            selectedRoundIdxs.add(idx)
        }

        // Extract matchups, attaching forced weeks from protected rivalries
        return selectedRoundIdxs.flatMap { roundIdx ->
            allRounds[roundIdx].map { (t1, t2) ->
                val forcedWeek =
                    protectedRivalries.firstOrNull { r ->
                        r.week != null &&
                            (
                                (r.team1 == t1 && r.team2 == t2) ||
                                    (r.team1 == t2 && r.team2 == t1)
                            )
                    }?.week
                Triple(t1, t2, forcedWeek)
            }
        }
    }

    /**
     * Greedy matchup selection for **odd** team counts (or when the circle
     * method is not applicable).  Retries with different random shuffles
     * up to 20 times to guarantee every team gets exactly [numGames] games.
     */
    private fun selectMatchupsGreedy(
        teamNames: List<String>,
        numGames: Int,
        protectedRivalries: List<com.fcfb.arceus.dto.standard.ProtectedRivalry>,
    ): List<Triple<String, String, Int?>> {
        val numTeams = teamNames.size
        val expectedMatchups = numTeams * numGames / 2

        for (attempt in 1..20) {
            val matchups = mutableListOf<Triple<String, String, Int?>>()
            val teamGameCounts = teamNames.associateWith { 0 }.toMutableMap()

            // Add protected rivalries first
            for (rivalry in protectedRivalries) {
                if (rivalry.team1.isBlank() || rivalry.team2.isBlank()) continue
                matchups.add(Triple(rivalry.team1, rivalry.team2, rivalry.week))
                teamGameCounts[rivalry.team1] = (teamGameCounts[rivalry.team1] ?: 0) + 1
                teamGameCounts[rivalry.team2] = (teamGameCounts[rivalry.team2] ?: 0) + 1
            }

            // Build all non-protected possible matchups and shuffle
            val possibleMatchups = mutableListOf<Pair<String, String>>()
            for (i in teamNames.indices) {
                for (j in i + 1 until numTeams) {
                    val pair = Pair(teamNames[i], teamNames[j])
                    if (!protectedRivalries.any { r ->
                            (r.team1 == pair.first && r.team2 == pair.second) ||
                                (r.team1 == pair.second && r.team2 == pair.first)
                        }
                    ) {
                        possibleMatchups.add(pair)
                    }
                }
            }
            possibleMatchups.shuffle()

            // Greedy fill
            for ((t1, t2) in possibleMatchups) {
                if ((teamGameCounts[t1] ?: 0) < numGames && (teamGameCounts[t2] ?: 0) < numGames) {
                    matchups.add(Triple(t1, t2, null))
                    teamGameCounts[t1] = (teamGameCounts[t1] ?: 0) + 1
                    teamGameCounts[t2] = (teamGameCounts[t2] ?: 0) + 1
                }
            }

            val allGood = teamNames.all { (teamGameCounts[it] ?: 0) >= numGames }
            if (allGood || matchups.size >= expectedMatchups) {
                return matchups
            }
            Logger.warn("Greedy matchup selection attempt $attempt: ${matchups.size}/$expectedMatchups matchups. Retrying...")
        }

        throw IllegalStateException("Could not select valid matchups after 20 attempts")
    }

    /**
     * Generate all rounds of a round-robin tournament using the circle (polygon)
     * method.  For *n* teams (n even) this produces n-1 rounds, each containing
     * n/2 games — every possible pairing appears exactly once.
     */
    private fun generateCircleMethodRounds(teamNames: List<String>): List<List<Pair<String, String>>> {
        val teams = teamNames.toMutableList()
        require(teams.size % 2 == 0) { "Circle method requires an even number of teams" }

        val n = teams.size
        val rounds = mutableListOf<List<Pair<String, String>>>()
        val fixed = teams[0]
        val rotating = teams.subList(1, n).toMutableList()

        for (round in 0 until n - 1) {
            val games = mutableListOf<Pair<String, String>>()
            games.add(Pair(fixed, rotating[0]))
            for (i in 1 until n / 2) {
                // Pair from opposite ends of the rotating array:
                // rotating[1] ↔ rotating[n-2], rotating[2] ↔ rotating[n-3], etc.
                // The correct opposite index is (n-1) - i  (last index of rotating = n-2, offset by i-1)
                games.add(Pair(rotating[i], rotating[n - 1 - i]))
            }
            rounds.add(games)
            // Rotate: move the last element to the front
            rotating.add(0, rotating.removeAt(rotating.size - 1))
        }

        // Sanity check: no team should ever play itself
        for ((roundIdx, round) in rounds.withIndex()) {
            for ((t1, t2) in round) {
                if (t1 == t2) {
                    Logger.error("Circle method BUG: self-matchup $t1 vs $t2 in round $roundIdx")
                }
            }
        }

        return rounds
    }

    // Week Assignment (MRV + Backtracking)

    /**
     * Assign each matchup to a week using **dynamic MRV + backtracking**.
     *
     * At every step the matchup with the fewest available weeks is chosen first
     * (Most Constrained Variable heuristic).  Within that matchup, the
     * least-loaded week is tried first.  If a dead end is reached, the
     * algorithm backtracks and tries the next option.
     *
     * Returns the list of (matchup, week) pairs, or `null` if no valid
     * assignment exists within the backtrack budget.
     */
    private fun assignMatchupsToWeeks(
        matchups: List<Triple<String, String, Int?>>,
        existingConstraints: Map<String, Set<Int>>,
        startWeek: Int,
        totalWeeks: Int,
    ): List<Pair<Triple<String, String, Int?>, Int>>? {
        // Mutable constraint tracker
        val teamWeeks = mutableMapOf<String, MutableSet<Int>>()
        existingConstraints.forEach { (team, weeks) ->
            teamWeeks[team] = weeks.toMutableSet()
        }

        val forced = matchups.filter { it.third != null }
        val flexible = matchups.filter { it.third == null }.toMutableList()
        val assignments = mutableListOf<Pair<Triple<String, String, Int?>, Int>>()

        // 1) Assign forced-week matchups first
        for (m in forced) {
            val week = m.third!!
            teamWeeks.getOrPut(m.first) { mutableSetOf() }.add(week)
            teamWeeks.getOrPut(m.second) { mutableSetOf() }.add(week)
            assignments.add(Pair(m, week))
        }

        // 2) Backtrack on flexible matchups using dynamic MRV
        var backtracks = 0
        val maxBacktracks = 500_000

        fun backtrack(): Boolean {
            if (flexible.isEmpty()) return true
            if (backtracks++ > maxBacktracks) return false

            // Dynamic MRV: pick the matchup with the fewest available weeks
            var bestIdx = -1
            var bestCount = Int.MAX_VALUE
            var bestWeeks: List<Int> = emptyList()

            for (i in flexible.indices) {
                val m = flexible[i]
                val avail =
                    (startWeek..totalWeeks).filter { w ->
                        teamWeeks[m.first]?.contains(w) != true &&
                            teamWeeks[m.second]?.contains(w) != true
                    }
                if (avail.isEmpty()) return false // early failure — prune immediately
                if (avail.size < bestCount) {
                    bestIdx = i
                    bestCount = avail.size
                    bestWeeks = avail
                }
            }

            val m = flexible.removeAt(bestIdx)
            // Try least-loaded weeks first for even distribution
            val sortedWeeks = bestWeeks.sortedBy { w -> assignments.count { it.second == w } }

            for (w in sortedWeeks) {
                teamWeeks.getOrPut(m.first) { mutableSetOf() }.add(w)
                teamWeeks.getOrPut(m.second) { mutableSetOf() }.add(w)
                assignments.add(Pair(m, w))

                if (backtrack()) return true

                teamWeeks[m.first]?.remove(w)
                teamWeeks[m.second]?.remove(w)
                assignments.removeAt(assignments.size - 1)
            }

            flexible.add(bestIdx, m) // put it back
            return false
        }

        return if (backtrack()) {
            Logger.info("Week assignment completed with $backtracks backtracks")
            assignments
        } else {
            Logger.error("Week assignment failed after $backtracks backtracks")
            null
        }
    }

    // Previous-Season Home/Away Lookup

    /**
     * Build a map of sorted matchup key ("TeamA|TeamB") -> which team was home
     * in the most recent previous season.
     *
     * Checks season-1 first, then season-2 for any matchups not found.
     * Only considers conference games between teams in [teamNames].
     */
    private fun buildPreviousSeasonHomeAwayMap(
        currentSeason: Int,
        teamNames: List<String>,
    ): Map<String, String> {
        val teamSet = teamNames.toSet()
        val prevHomeTeam = mutableMapOf<String, String>()

        // Season N-1 first (takes priority)
        val prevSeasonSchedule =
            try {
                scheduleRepository.getScheduleBySeason(currentSeason - 1) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        for (game in prevSeasonSchedule) {
            val home = game.homeTeam ?: continue
            val away = game.awayTeam ?: continue
            if (game.gameType != GameType.CONFERENCE_GAME) continue
            if (home !in teamSet || away !in teamSet) continue
            val key = listOf(home, away).sorted().joinToString("|")
            if (key !in prevHomeTeam) prevHomeTeam[key] = home
        }

        // Fall back to season N-2 for any matchups not found
        val twoAgoSchedule =
            try {
                scheduleRepository.getScheduleBySeason(currentSeason - 2) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        for (game in twoAgoSchedule) {
            val home = game.homeTeam ?: continue
            val away = game.awayTeam ?: continue
            if (game.gameType != GameType.CONFERENCE_GAME) continue
            if (home !in teamSet || away !in teamSet) continue
            val key = listOf(home, away).sorted().joinToString("|")
            if (key !in prevHomeTeam) prevHomeTeam[key] = home
        }

        Logger.info("Found ${prevHomeTeam.size} previous-season home/away records for alternation")
        return prevHomeTeam
    }

    // Async Generate-All-Conferences (fire-and-forget)

    /**
     * Start generating all conference schedules asynchronously.
     * Returns a job ID immediately; the actual processing happens in a background coroutine.
     * Poll [getScheduleGenJobStatus] for progress.
     */
    fun startAllConferenceGenerationAsync(season: Int): ScheduleGenJobResponse {
        val jobId = UUID.randomUUID().toString()
        val now = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val skipConferences = setOf(Conference.FBS_INDEPENDENT, Conference.FAKE_TEAM)
        val conferencesToGenerate = Conference.values().filter { it !in skipConferences }
        val validConferences =
            conferencesToGenerate.filter { conf ->
                val teams = teamService.getTeamsInConference(conf.name)
                !teams.isNullOrEmpty()
            }

        val job =
            ScheduleGenJob(
                jobId = jobId,
                season = season,
                status = ScheduleGenJobStatus.PENDING,
                totalConferences = validConferences.size,
                startedAt = now,
            )
        activeGenJobs[jobId] = job

        Logger.info("=== STARTING ALL-CONFERENCE SCHEDULE GENERATION (async) ===")
        Logger.info("Job ID: $jobId, Season: $season, Conferences: ${validConferences.size}")

        CoroutineScope(Dispatchers.IO).launch {
            processAllConferenceGeneration(
                jobId,
                season,
                validConferences,
            )
        }

        return ScheduleGenJobResponse(
            jobId = jobId,
            message =
                "Started generating schedules for ${validConferences.size} conferences. " +
                    "Poll /schedule/generate-all-conferences/status/$jobId for progress.",
        )
    }

    private fun processAllConferenceGeneration(
        jobId: String,
        season: Int,
        conferences: List<Conference>,
    ) {
        val job = activeGenJobs[jobId] ?: return
        job.status = ScheduleGenJobStatus.IN_PROGRESS

        for ((index, conference) in conferences.withIndex()) {
            val timestamp =
                ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            try {
                Logger.info("[${index + 1}/${conferences.size}] Generating schedule for ${conference.name}...")
                val teams = teamService.getTeamsInConference(conference.name) ?: emptyList()
                val subdivision = teams.firstOrNull()?.subdivision ?: Subdivision.FBS

                val request =
                    ConferenceScheduleRequest(
                        season = season,
                        conference = conference.name,
                        subdivision = subdivision,
                        numConferenceGames = 9,
                        protectedRivalries = emptyList(),
                        startWeek = 1,
                    )

                val generated = generateConferenceSchedule(request, teams)
                job.completedConferences++
                job.totalGamesGenerated += generated.size

                job.logs.add(
                    ScheduleGenLog(
                        conference = conference.name,
                        status = "SUCCESS",
                        gamesGenerated = generated.size,
                        message = "Generated ${generated.size} games",
                        timestamp = timestamp,
                    ),
                )
                Logger.info("[${index + 1}/${conferences.size}] ${conference.name}: ${generated.size} games generated")
            } catch (e: Exception) {
                job.failedConferences++
                val errorMsg = e.message ?: "Unknown error"

                job.logs.add(
                    ScheduleGenLog(
                        conference = conference.name,
                        status = "FAILED",
                        gamesGenerated = 0,
                        message = errorMsg,
                        timestamp = timestamp,
                    ),
                )
                Logger.error("[${index + 1}/${conferences.size}] FAILED ${conference.name}: $errorMsg")
            }
        }

        val completedAt = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        job.completedAt = completedAt
        job.status = ScheduleGenJobStatus.COMPLETED

        Logger.info("=== ALL-CONFERENCE GENERATION COMPLETE ===")
        Logger.info(
            "Job: $jobId — Conferences: ${job.completedConferences}/${job.totalConferences} succeeded, " +
                "${job.failedConferences} failed, ${job.totalGamesGenerated} total games",
        )
    }

    fun getScheduleGenJobStatus(jobId: String): ScheduleGenJob? = activeGenJobs[jobId]
}
