package com.fcfb.arceus.service.fcfb.schedule

import com.fcfb.arceus.dto.request.ConferenceScheduleRequest
import com.fcfb.arceus.dto.request.ScheduleEntry
import com.fcfb.arceus.dto.response.ScheduleGenJob
import com.fcfb.arceus.dto.response.ScheduleGenJobResponse
import com.fcfb.arceus.dto.response.ScheduleGenJobStatus
import com.fcfb.arceus.dto.response.ScheduleGenLog
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.Conference
import com.fcfb.arceus.model.Schedule
import com.fcfb.arceus.model.Team
import com.fcfb.arceus.repositories.ConferenceRepository
import com.fcfb.arceus.repositories.ScheduleRepository
import com.fcfb.arceus.service.fcfb.SeasonService
import com.fcfb.arceus.service.fcfb.TeamService
import com.fcfb.arceus.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class ConferenceScheduleGenerationService(
    private val seasonService: SeasonService,
    private val scheduleRepository: ScheduleRepository,
    private val teamService: TeamService,
    private val conferenceRepository: ConferenceRepository,
    private val conferenceRulesService: ConferenceRulesService,
) {
    companion object {
        private val activeGenJobs = ConcurrentHashMap<String, ScheduleGenJob>()
        private const val RECENT_OPPONENT_LOOKBACK_SEASONS = 5
    }

    private fun checkScheduleLock(season: Int) {
        if (seasonService.isScheduleLocked(season)) {
            throw IllegalStateException("Schedule for season $season is locked and cannot be modified")
        }
    }

    private fun matchupKey(
        t1: String,
        t2: String,
    ): String = listOf(t1, t2).sorted().joinToString("|")

    fun generateConferenceSchedule(request: ConferenceScheduleRequest): List<Schedule> {
        val conferenceTeams = (teamService.getTeamsInConference(request.conference) ?: emptyList()).filter { it.active }
        return generateConferenceScheduleForTeams(request, conferenceTeams)
    }

    private fun generateConferenceScheduleForTeams(
        request: ConferenceScheduleRequest,
        conferenceTeams: List<Team>,
    ): List<Schedule> {
        checkScheduleLock(request.season)
        val teamNames = conferenceTeams.map { it.name ?: "" }.filter { it.isNotEmpty() }
        val numTeams = teamNames.size
        val numGames = minOf(request.numConferenceGames, numTeams - 1)
        val totalWeeks = 12

        Logger.info("Generating conference schedule for ${request.conference}: $numTeams teams, $numGames games per team")

        if (numTeams < 2) {
            Logger.warn("Not enough teams ($numTeams) to generate schedule for ${request.conference}")
            return emptyList()
        }

        deleteExistingConferenceGames(request.season, teamNames)
        val teamWeekSchedule = buildTeamWeekConstraints(request.season, teamNames)
        val matchups = selectMatchups(request, conferenceTeams, teamNames, numGames)

        val weekAssignments =
            assignMatchupsToWeeks(matchups, teamWeekSchedule, request.startWeek, totalWeeks)
                ?: throw IllegalStateException(
                    "Could not assign weeks for ${request.conference} conference schedule. " +
                        "Check for conflicting OOC games or reduce conference game count.",
                )
        Logger.info("Assigned ${weekAssignments.size} matchups to weeks for ${request.conference}")

        val entries = buildScheduleEntries(request, teamNames, weekAssignments)
        val result = saveScheduleEntries(entries)
        Logger.info("Generated ${result.size} conference games for ${request.conference}")
        return result
    }

    private fun deleteExistingConferenceGames(
        season: Int,
        teamNames: List<String>,
    ) {
        val existingSchedule = scheduleRepository.getScheduleBySeason(season) ?: emptyList()
        existingSchedule.filter { s ->
            s.gameType == GameType.CONFERENCE_GAME &&
                (teamNames.contains(s.homeTeam) || teamNames.contains(s.awayTeam))
        }.forEach { scheduleRepository.delete(it) }
    }

    private fun buildTeamWeekConstraints(
        season: Int,
        teamNames: List<String>,
    ): MutableMap<String, MutableSet<Int>> {
        val teamWeekSchedule = teamNames.associateWith { mutableSetOf<Int>() }.toMutableMap()
        for (team in teamNames) {
            scheduleRepository.getScheduleBySeasonAndTeam(season, team)?.forEach { g ->
                if (g.week != null) teamWeekSchedule[team]?.add(g.week!!)
            }
        }
        return teamWeekSchedule
    }

    private fun selectMatchups(
        request: ConferenceScheduleRequest,
        conferenceTeams: List<Team>,
        teamNames: List<String>,
        numGames: Int,
    ): List<Triple<String, String, Int?>> {
        val protectedRivalries = request.protectedRivalries
        if (request.divisions.isNotEmpty()) {
            val matchups =
                selectMatchupsWithDivisions(conferenceTeams, teamNames, numGames, protectedRivalries, request.conference, request.season)
            Logger.info("Division-aware selection produced ${matchups.size} matchups for ${request.conference}")
            return matchups
        }
        if (teamNames.size % 2 == 0) {
            val matchups = selectMatchupsCircleMethod(teamNames, numGames, protectedRivalries)
            Logger.info("Circle method selected ${matchups.size} matchups for ${request.conference}")
            return matchups
        }
        val feasibleGames = feasibleGreedyGameCount(teamNames.size, numGames)
        if (feasibleGames != numGames) {
            Logger.warn(
                "${request.conference} has an odd number of teams (${teamNames.size}); " +
                    "$numGames games per team is impossible (odd teams * odd games must be even). " +
                    "Using $feasibleGames games per team instead.",
            )
        }
        val matchups = selectMatchupsGreedy(teamNames, feasibleGames, protectedRivalries)
        Logger.info("Greedy method selected ${matchups.size} matchups for ${request.conference}")
        return matchups
    }

    private fun buildScheduleEntries(
        request: ConferenceScheduleRequest,
        teamNames: List<String>,
        weekAssignments: List<Pair<Triple<String, String, Int?>, Int>>,
    ): List<ScheduleEntry> {
        val prevHomeTeam = buildPreviousSeasonHomeAwayMap(request.season, teamNames)
        val teamHomeGames = teamNames.associateWith { 0 }.toMutableMap()
        val teamAwayGames = teamNames.associateWith { 0 }.toMutableMap()

        return weekAssignments.map { (matchup, week) ->
            val (home, away) = determineHomeAway(matchup.first, matchup.second, prevHomeTeam, teamHomeGames, teamAwayGames)
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
    }

    private fun determineHomeAway(
        t1: String,
        t2: String,
        prevHomeTeam: Map<String, String>,
        teamHomeGames: Map<String, Int>,
        teamAwayGames: Map<String, Int>,
    ): Pair<String, String> {
        val previousHome = prevHomeTeam[matchupKey(t1, t2)]
        if (previousHome != null) {
            return if (previousHome == t1) t2 to t1 else t1 to t2
        }

        val h1 = teamHomeGames[t1] ?: 0
        val a1 = teamAwayGames[t1] ?: 0
        val h2 = teamHomeGames[t2] ?: 0
        val a2 = teamAwayGames[t2] ?: 0
        return when {
            h1 <= a1 && h2 >= a2 -> t1 to t2
            h2 <= a2 && h1 >= a1 -> t2 to t1
            h1 <= h2 -> t1 to t2
            else -> t2 to t1
        }
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
                schedule.tvChannel =
                    if (entry.gameType in listOf(GameType.BOWL, GameType.PLAYOFFS, GameType.NATIONAL_CHAMPIONSHIP)) {
                        TVChannel.ESPN
                    } else {
                        entry.tvChannel
                    }
                schedule.gameType = entry.gameType
                schedule.started = false
                schedule.finished = false
                schedule.playoffRound = entry.playoffRound
                schedule.playoffHomeSeed = entry.playoffHomeSeed
                schedule.playoffAwaySeed = entry.playoffAwaySeed
                schedule.postseasonGameName = entry.postseasonGameName
                schedule.postseasonGameLogo = entry.postseasonGameLogo
                schedule
            }
        return scheduleRepository.saveAll(schedules).toList()
    }

    private fun selectMatchupsCircleMethod(
        teamNames: List<String>,
        numGames: Int,
        protectedRivalries: List<com.fcfb.arceus.dto.standard.ProtectedRivalry>,
    ): List<Triple<String, String, Int?>> {
        val allRounds = generateCircleMethodRounds(teamNames)

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

        val selectedRoundIdxs = requiredRoundIdxs.toMutableSet()
        val candidateIdxs = (allRounds.indices.toList() - requiredRoundIdxs).shuffled()
        for (idx in candidateIdxs) {
            if (selectedRoundIdxs.size >= numGames) break
            selectedRoundIdxs.add(idx)
        }

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

    private fun feasibleGreedyGameCount(
        numTeams: Int,
        numGames: Int,
    ): Int = if (numTeams % 2 != 0 && numGames % 2 != 0) maxOf(numGames - 1, 0) else numGames

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

            for (rivalry in protectedRivalries) {
                if (rivalry.team1.isBlank() || rivalry.team2.isBlank()) continue
                matchups.add(Triple(rivalry.team1, rivalry.team2, rivalry.week))
                teamGameCounts[rivalry.team1] = (teamGameCounts[rivalry.team1] ?: 0) + 1
                teamGameCounts[rivalry.team2] = (teamGameCounts[rivalry.team2] ?: 0) + 1
            }

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

    private fun selectMatchupsWithDivisions(
        conferenceTeams: List<Team>,
        teamNames: List<String>,
        numGames: Int,
        protectedRivalries: List<com.fcfb.arceus.dto.standard.ProtectedRivalry>,
        conference: String,
        season: Int,
    ): List<Triple<String, String, Int?>> {
        val divisionOf = buildDivisionMap(conferenceTeams)
        val divisionGroups = teamNames.filter { divisionOf[it] != null }.groupBy { divisionOf[it]!! }

        val intraDivisionGames = buildIntraDivisionRoundRobin(divisionGroups, protectedRivalries)
        val intraDivisionKeys = intraDivisionGames.map { matchupKey(it.first, it.second) }.toSet()
        val crossDivisionRivalries = buildCrossDivisionRivalries(teamNames, protectedRivalries, intraDivisionKeys)

        val requiredCount = countGamesPerTeam(teamNames, intraDivisionGames + crossDivisionRivalries)
        validateDivisionGameCounts(teamNames, requiredCount, divisionOf, divisionGroups, numGames, conference)

        val remaining = teamNames.associateWith { numGames - (requiredCount[it] ?: 0) }
        val usedKeys = intraDivisionKeys + crossDivisionRivalries.map { matchupKey(it.first, it.second) }
        val filler =
            if (remaining.values.any { it > 0 }) {
                selectCrossDivisionFiller(remaining, usedKeys, divisionOf, teamNames, season)
            } else {
                emptyList()
            }

        return intraDivisionGames + crossDivisionRivalries + filler
    }

    private fun buildDivisionMap(conferenceTeams: List<Team>): Map<String, String?> =
        conferenceTeams.associate { (it.name ?: "") to it.division?.takeIf { d -> d.isNotBlank() } }

    private fun buildIntraDivisionRoundRobin(
        divisionGroups: Map<String, List<String>>,
        protectedRivalries: List<com.fcfb.arceus.dto.standard.ProtectedRivalry>,
    ): List<Triple<String, String, Int?>> {
        val matchups = mutableListOf<Triple<String, String, Int?>>()
        for (teams in divisionGroups.values) {
            if (teams.size < 2) continue
            for (i in teams.indices) {
                for (j in i + 1 until teams.size) {
                    val t1 = teams[i]
                    val t2 = teams[j]
                    val forcedWeek =
                        protectedRivalries.firstOrNull { r ->
                            r.week != null && ((r.team1 == t1 && r.team2 == t2) || (r.team1 == t2 && r.team2 == t1))
                        }?.week
                    matchups.add(Triple(t1, t2, forcedWeek))
                }
            }
        }
        return matchups
    }

    private fun buildCrossDivisionRivalries(
        teamNames: List<String>,
        protectedRivalries: List<com.fcfb.arceus.dto.standard.ProtectedRivalry>,
        alreadyMatchedKeys: Set<String>,
    ): List<Triple<String, String, Int?>> {
        val matchups = mutableListOf<Triple<String, String, Int?>>()
        for (rivalry in protectedRivalries) {
            val t1 = rivalry.team1
            val t2 = rivalry.team2
            if (t1.isBlank() || t2.isBlank() || t1 !in teamNames || t2 !in teamNames) continue
            if (matchupKey(t1, t2) in alreadyMatchedKeys) continue
            matchups.add(Triple(t1, t2, rivalry.week))
        }
        return matchups
    }

    private fun countGamesPerTeam(
        teamNames: List<String>,
        matchups: List<Triple<String, String, Int?>>,
    ): Map<String, Int> {
        val counts = teamNames.associateWith { 0 }.toMutableMap()
        matchups.forEach { (t1, t2, _) ->
            counts[t1] = (counts[t1] ?: 0) + 1
            counts[t2] = (counts[t2] ?: 0) + 1
        }
        return counts
    }

    private fun validateDivisionGameCounts(
        teamNames: List<String>,
        requiredCount: Map<String, Int>,
        divisionOf: Map<String, String?>,
        divisionGroups: Map<String, List<String>>,
        numGames: Int,
        conference: String,
    ) {
        val shortfalls = teamNames.filter { (requiredCount[it] ?: 0) > numGames }
        if (shortfalls.isEmpty()) return
        val details =
            shortfalls.joinToString("; ") { t ->
                val divSize = divisionOf[t]?.let { divisionGroups[it]?.size } ?: 0
                val rivalryCount = (requiredCount[t] ?: 0) - maxOf(divSize - 1, 0)
                "$t needs ${requiredCount[t]} games (${maxOf(divSize - 1, 0)} division round robin + " +
                    "$rivalryCount cross-division rivalries)"
            }
        throw IllegalStateException("Cannot generate schedule for $conference: numConferenceGames is $numGames but $details")
    }

    private fun selectCrossDivisionFiller(
        remaining: Map<String, Int>,
        alreadyMatchedKeys: Set<String>,
        divisionOf: Map<String, String?>,
        teamNames: List<String>,
        currentSeason: Int,
    ): List<Triple<String, String, Int?>> {
        val lastPlayedSeason = buildRecentOpponentSeasons(currentSeason, teamNames)
        val candidatePairs = findCrossDivisionCandidatePairs(teamNames, divisionOf, alreadyMatchedKeys)

        for (attempt in 1..20) {
            val matchups = fillByStaleness(candidatePairs, remaining, lastPlayedSeason, currentSeason)
            if (matchups != null) return matchups
            Logger.warn("Cross-division filler attempt $attempt failed. Retrying...")
        }

        throw IllegalStateException(
            "Could not fill cross-division conference games after 20 attempts. " +
                "Check numConferenceGames against division sizes and rivalry counts.",
        )
    }

    private fun findCrossDivisionCandidatePairs(
        teamNames: List<String>,
        divisionOf: Map<String, String?>,
        alreadyMatchedKeys: Set<String>,
    ): List<Pair<String, String>> {
        val candidates = mutableListOf<Pair<String, String>>()
        for (i in teamNames.indices) {
            for (j in i + 1 until teamNames.size) {
                val t1 = teamNames[i]
                val t2 = teamNames[j]
                if (divisionOf[t1] != null && divisionOf[t1] == divisionOf[t2]) continue
                if (matchupKey(t1, t2) in alreadyMatchedKeys) continue
                candidates.add(t1 to t2)
            }
        }
        return candidates
    }

    private fun fillByStaleness(
        candidatePairs: List<Pair<String, String>>,
        remaining: Map<String, Int>,
        lastPlayedSeason: Map<String, Int>,
        currentSeason: Int,
    ): List<Triple<String, String, Int?>>? {
        val staleFloor = currentSeason - RECENT_OPPONENT_LOOKBACK_SEASONS - 1
        val ordered =
            candidatePairs
                .groupBy { (t1, t2) -> currentSeason - (lastPlayedSeason[matchupKey(t1, t2)] ?: staleFloor) }
                .entries
                .sortedByDescending { it.key }
                .flatMap { it.value.shuffled() }

        val work = remaining.toMutableMap()
        val matchups = mutableListOf<Triple<String, String, Int?>>()
        for ((t1, t2) in ordered) {
            val r1 = work[t1] ?: 0
            val r2 = work[t2] ?: 0
            if (r1 > 0 && r2 > 0) {
                matchups.add(Triple(t1, t2, null))
                work[t1] = r1 - 1
                work[t2] = r2 - 1
            }
        }
        return if (work.values.all { it == 0 }) matchups else null
    }

    private fun buildRecentOpponentSeasons(
        currentSeason: Int,
        teamNames: List<String>,
    ): Map<String, Int> {
        val teamSet = teamNames.toSet()
        val lastPlayed = mutableMapOf<String, Int>()

        for (seasonsAgo in 1..RECENT_OPPONENT_LOOKBACK_SEASONS) {
            val season = currentSeason - seasonsAgo
            val schedule =
                try {
                    scheduleRepository.getScheduleBySeason(season) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            for (game in schedule) {
                val home = game.homeTeam ?: continue
                val away = game.awayTeam ?: continue
                if (game.gameType != GameType.CONFERENCE_GAME) continue
                if (home !in teamSet || away !in teamSet) continue
                val key = matchupKey(home, away)
                if (key !in lastPlayed) lastPlayed[key] = season
            }
        }

        return lastPlayed
    }

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
                games.add(Pair(rotating[i], rotating[n - 1 - i]))
            }
            rounds.add(games)
            rotating.add(0, rotating.removeAt(rotating.size - 1))
        }

        for ((roundIdx, round) in rounds.withIndex()) {
            for ((t1, t2) in round) {
                if (t1 == t2) {
                    Logger.error("Circle method BUG: self-matchup $t1 vs $t2 in round $roundIdx")
                }
            }
        }

        return rounds
    }

    private fun assignMatchupsToWeeks(
        matchups: List<Triple<String, String, Int?>>,
        existingConstraints: Map<String, Set<Int>>,
        startWeek: Int,
        totalWeeks: Int,
    ): List<Pair<Triple<String, String, Int?>, Int>>? {
        val teamWeeks = mutableMapOf<String, MutableSet<Int>>()
        existingConstraints.forEach { (team, weeks) ->
            teamWeeks[team] = weeks.toMutableSet()
        }

        val forced = matchups.filter { it.third != null }
        val flexible = matchups.filter { it.third == null }.toMutableList()
        val assignments = mutableListOf<Pair<Triple<String, String, Int?>, Int>>()

        for (m in forced) {
            val week = m.third!!
            teamWeeks.getOrPut(m.first) { mutableSetOf() }.add(week)
            teamWeeks.getOrPut(m.second) { mutableSetOf() }.add(week)
            assignments.add(Pair(m, week))
        }

        var backtracks = 0
        val maxBacktracks = 500_000

        fun backtrack(): Boolean {
            if (flexible.isEmpty()) return true
            if (backtracks++ > maxBacktracks) return false

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
                if (avail.isEmpty()) return false
                if (avail.size < bestCount) {
                    bestIdx = i
                    bestCount = avail.size
                    bestWeeks = avail
                }
            }

            val m = flexible.removeAt(bestIdx)
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

            flexible.add(bestIdx, m)
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

    private fun buildPreviousSeasonHomeAwayMap(
        currentSeason: Int,
        teamNames: List<String>,
    ): Map<String, String> {
        val teamSet = teamNames.toSet()
        val prevHomeTeam = homeTeamByMatchup(currentSeason - 1, teamSet).toMutableMap()
        homeTeamByMatchup(currentSeason - 2, teamSet).forEach { (key, home) -> prevHomeTeam.putIfAbsent(key, home) }
        Logger.info("Found ${prevHomeTeam.size} previous-season home/away records for alternation")
        return prevHomeTeam
    }

    private fun homeTeamByMatchup(
        season: Int,
        teamSet: Set<String>,
    ): Map<String, String> {
        val schedule =
            try {
                scheduleRepository.getScheduleBySeason(season) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        val result = mutableMapOf<String, String>()
        for (game in schedule) {
            val home = game.homeTeam ?: continue
            val away = game.awayTeam ?: continue
            if (game.gameType != GameType.CONFERENCE_GAME) continue
            if (home !in teamSet || away !in teamSet) continue
            result.putIfAbsent(matchupKey(home, away), home)
        }
        return result
    }

    fun startAllConferenceGenerationAsync(season: Int): ScheduleGenJobResponse {
        val jobId = UUID.randomUUID().toString()
        val now = ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val skipConferences = setOf("FBS_INDEPENDENT", "FAKE_TEAM")
        val conferencesToGenerate = conferenceRepository.findAllByActiveTrueOrderByLabelAsc().filter { it.code !in skipConferences }
        val validConferences =
            conferencesToGenerate.filter { conf ->
                val teams = teamService.getTeamsInConference(conf.code)
                teams?.any { it.active } == true
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
                Logger.info("[${index + 1}/${conferences.size}] Generating schedule for ${conference.code}...")
                val teams = (teamService.getTeamsInConference(conference.code) ?: emptyList()).filter { it.active }
                val subdivision = teams.firstOrNull()?.subdivision ?: Subdivision.FBS
                val rules = conferenceRulesService.getConferenceRules(conference.code)

                val request =
                    ConferenceScheduleRequest(
                        season = season,
                        conference = conference.code,
                        subdivision = subdivision,
                        numConferenceGames = rules?.numConferenceGames ?: 9,
                        protectedRivalries = rules?.protectedRivalries ?: emptyList(),
                        startWeek = 1,
                        divisions = rules?.divisions ?: emptyList(),
                    )

                val generated = generateConferenceScheduleForTeams(request, teams)
                job.completedConferences++
                job.totalGamesGenerated += generated.size

                job.logs.add(
                    ScheduleGenLog(
                        conference = conference.code,
                        status = "SUCCESS",
                        gamesGenerated = generated.size,
                        message = "Generated ${generated.size} games",
                        timestamp = timestamp,
                    ),
                )
                Logger.info("[${index + 1}/${conferences.size}] ${conference.code}: ${generated.size} games generated")
            } catch (e: Exception) {
                job.failedConferences++
                val errorMsg = e.message ?: "Unknown error"

                job.logs.add(
                    ScheduleGenLog(
                        conference = conference.code,
                        status = "FAILED",
                        gamesGenerated = 0,
                        message = errorMsg,
                        timestamp = timestamp,
                    ),
                )
                Logger.error("[${index + 1}/${conferences.size}] FAILED ${conference.code}: $errorMsg")
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
