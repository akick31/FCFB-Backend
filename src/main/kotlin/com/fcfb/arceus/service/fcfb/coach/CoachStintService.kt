package com.fcfb.arceus.service.fcfb.coach

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.service.log.UsernameHistoryService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CoachStintService(
    private val coachTransactionLogRepository: CoachTransactionLogRepository,
    private val teamRepository: TeamRepository,
    private val usernameHistoryService: UsernameHistoryService,
) {
    private val transactionDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
    private val gameTimestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun getStintsForCoach(
        coach: String,
        user: User?,
    ): List<CoachStint> = buildStints(user?.discordId, resolveCoachNames(coach, user), user?.team)

    fun countsForCoach(
        stints: List<CoachStint>,
        currentTeam: String?,
        team: String,
        timestamp: LocalDateTime,
    ): Boolean = team == currentTeam || isPrimaryTeamAt(stints, team, timestamp)

    fun isPrimaryTeamAt(
        stints: List<CoachStint>,
        team: String,
        timestamp: LocalDateTime,
    ): Boolean {
        val activeOnTeam = stints.filter { it.team == team && covers(it, timestamp) }
        if (activeOnTeam.isEmpty()) return false
        if (activeOnTeam.any { !it.interim }) return true
        return stints.none { !it.interim && it.team != team && covers(it, timestamp) }
    }

    /**
     * A HIRED is often logged after the coach's first game, leaving early games just before the stint.
     * Pull each team's earliest stint back to its earliest actual game so those games are attributed.
     */
    fun backdateStartsToGames(
        stints: List<CoachStint>,
        earliestGameByTeam: Map<String, LocalDateTime>,
    ) {
        stints.groupBy { it.team }.forEach { (team, teamStints) ->
            val earliestGame = earliestGameByTeam[team] ?: return@forEach
            val earliestStint = teamStints.minByOrNull { it.start } ?: return@forEach
            if (earliestGame < earliestStint.start) earliestStint.start = earliestGame
        }
    }

    fun parseGameTimestamp(value: String?): LocalDateTime? =
        try {
            value?.let { LocalDateTime.parse(it, gameTimestampFormat) }
        } catch (e: Exception) {
            null
        }

    private fun resolveCoachNames(
        coach: String,
        user: User?,
    ): Set<String> {
        val historicalNames = user?.let { usernameHistoryService.getHistoricalUsernames(it.id) } ?: emptyList()
        return (historicalNames + coach).toSet()
    }

    private fun buildStints(
        discordId: String?,
        coachNames: Set<String>,
        currentTeam: String?,
    ): List<CoachStint> {
        val log = coachTransactionLogRepository.getEntireCoachTransactionLog()
        val fakeTeams = fakeTeamNames()
        val entries =
            log
                .filter { it.matches(discordId, coachNames) }
                .mapNotNull { entry -> parseTransactionDate(entry.transactionDate)?.let { entry to it } }
                .sortedWith(compareBy({ it.second }, { it.first.id ?: 0 }))

        val stints = mutableListOf<CoachStint>()
        val open = mutableMapOf<String, Pair<LocalDateTime, Boolean>>()
        for ((entry, date) in entries) {
            val team = entry.team ?: continue
            if (team in fakeTeams) continue
            when (entry.transaction) {
                TransactionType.HIRED -> {
                    open.remove(team)?.let { stints.add(closeStint(team, it, date)) }
                    open[team] = date to false
                }
                TransactionType.HIRED_INTERIM -> {
                    open.remove(team)?.let { stints.add(closeStint(team, it, date)) }
                    open[team] = date to true
                }
                TransactionType.FIRED -> stints.add(closeStint(team, open.remove(team), date))
                else -> {}
            }
        }
        open.forEach { (team, value) -> stints.add(CoachStint(team, value.first, null, value.second)) }

        appendUnloggedCurrentTeamStint(stints, currentTeam, fakeTeams, log)
        return stints
    }

    /**
     * Closes an open stint at the given date. When no hire is open (a FIRED whose HIRED predates the
     * transaction log) the tenure is still real, so close it as a permanent stint from the start of time.
     */
    private fun closeStint(
        team: String,
        opened: Pair<LocalDateTime, Boolean>?,
        firedAt: LocalDateTime,
    ): CoachStint =
        if (opened != null) {
            CoachStint(team, opened.first, firedAt, opened.second)
        } else {
            CoachStint(team, LocalDateTime.MIN, firedAt, false)
        }

    /**
     * A coach's current team can be entirely absent from the transaction log (an original team
     * assignment that predates the log). With no competing claim on that team's history, treat the
     * whole tenure as one open permanent stint.
     */
    private fun appendUnloggedCurrentTeamStint(
        stints: MutableList<CoachStint>,
        currentTeam: String?,
        fakeTeams: Set<String>,
        log: List<CoachTransactionLog>,
    ) {
        if (currentTeam == null || currentTeam in fakeTeams) return
        if (stints.any { it.team == currentTeam }) return
        if (log.any { it.team == currentTeam }) return
        stints.add(CoachStint(currentTeam, LocalDateTime.MIN, null, interim = false))
    }

    private fun CoachTransactionLog.matches(
        discordId: String?,
        coachNames: Set<String>,
    ): Boolean {
        val matchesDiscordId = discordId != null && (coachDiscordIds ?: emptyList()).contains(discordId)
        val matchesName = (coach ?: emptyList()).any { coachNames.contains(it) }
        return matchesDiscordId || matchesName
    }

    private fun parseTransactionDate(value: String?): LocalDateTime? =
        try {
            value?.let { LocalDateTime.parse(it, transactionDateFormat) }
        } catch (e: Exception) {
            null
        }

    private fun covers(
        stint: CoachStint,
        timestamp: LocalDateTime,
    ): Boolean = timestamp >= stint.start && (stint.end == null || timestamp <= stint.end)

    private fun fakeTeamNames(): Set<String> =
        teamRepository.findAll().filter {
            it.subdivision == Subdivision.FAKE
        }.mapNotNull { it.name }.toSet()
}
