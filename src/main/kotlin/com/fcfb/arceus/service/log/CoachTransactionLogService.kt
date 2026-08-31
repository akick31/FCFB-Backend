package com.fcfb.arceus.service.log

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.CoachTransactionLog
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.repositories.UsernameHistoryRepository
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.UserForbiddenException
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class CoachTransactionLogService(
    private val coachTransactionLogRepository: CoachTransactionLogRepository,
    private val userRepository: UserRepository,
    private val usernameHistoryRepository: UsernameHistoryRepository,
    private val gameRepository: GameRepository,
) {
    private val transactionDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
    private val gameTimestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun logCoachTransaction(transaction: CoachTransactionLog) = coachTransactionLogRepository.save(transaction)

    fun getEntireCoachTransactionLog() = coachTransactionLogRepository.getEntireCoachTransactionLog()

    fun backfillDiscordIds(): Int {
        if (!AuthContext.isAdmin()) throw UserForbiddenException()

        var updated = 0
        for (entry in coachTransactionLogRepository.getEntireCoachTransactionLog()) {
            val discordIds = (entry.coach ?: emptyList()).mapNotNull { resolveDiscordId(it) }.distinct()
            if (discordIds.isNotEmpty()) {
                entry.coachDiscordIds = discordIds.toMutableList()
                coachTransactionLogRepository.save(entry)
                updated++
            }
        }
        return updated
    }

    private data class EarliestTeamGame(
        val coaches: List<String>,
        val coachDiscordIds: List<String>,
        val timestamp: LocalDateTime,
    )

    /** Synthesizes a HIRED entry, dated to each team's earliest tracked game in [season], for teams with no earlier log entry. */
    fun backfillPreLogHires(season: Int): Int {
        if (!AuthContext.isAdmin()) throw UserForbiddenException()

        val games = gameRepository.findBySeason(season).filter { it.gameType != GameType.SCRIMMAGE }
        val earliestByTeam = mutableMapOf<String, EarliestTeamGame>()
        for (game in games) {
            val timestamp = parseGameTimestamp(game.timestamp) ?: continue
            recordEarliestGame(earliestByTeam, game.homeTeam, game.homeCoaches, game.homeCoachDiscordIds, timestamp)
            recordEarliestGame(earliestByTeam, game.awayTeam, game.awayCoaches, game.awayCoachDiscordIds, timestamp)
        }

        val earliestLogDateByTeam =
            coachTransactionLogRepository.getEntireCoachTransactionLog()
                .mapNotNull { entry -> entry.team?.let { team -> parseTransactionDate(entry.transactionDate)?.let { team to it } } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, dates) -> dates.min() }

        var inserted = 0
        for ((team, info) in earliestByTeam) {
            if (info.coaches.isEmpty()) continue
            val earliestLogDate = earliestLogDateByTeam[team]
            if (earliestLogDate != null && !earliestLogDate.isAfter(info.timestamp)) continue

            coachTransactionLogRepository.save(
                CoachTransactionLog(
                    team,
                    CoachPosition.HEAD_COACH,
                    info.coaches.toMutableList(),
                    TransactionType.HIRED,
                    info.timestamp.format(transactionDateFormat),
                    "system-backfill",
                    info.coachDiscordIds.toMutableList(),
                ),
            )
            inserted++
        }
        return inserted
    }

    private fun recordEarliestGame(
        earliestByTeam: MutableMap<String, EarliestTeamGame>,
        team: String?,
        coaches: List<String>?,
        coachDiscordIds: List<String>?,
        timestamp: LocalDateTime,
    ) {
        if (team.isNullOrBlank()) return
        val current = earliestByTeam[team]
        if (current == null || timestamp.isBefore(current.timestamp)) {
            earliestByTeam[team] = EarliestTeamGame(coaches ?: emptyList(), coachDiscordIds ?: emptyList(), timestamp)
        }
    }

    private fun resolveDiscordId(username: String): String? {
        userRepository.findByUsername(username)?.discordId?.let { return it }
        val historicalUserId = usernameHistoryRepository.findByUsername(username).firstOrNull()?.userId ?: return null
        return userRepository.getById(historicalUserId)?.discordId
    }

    private fun parseTransactionDate(value: String?): LocalDateTime? =
        try {
            value?.let { LocalDateTime.parse(it, transactionDateFormat) }
        } catch (e: Exception) {
            null
        }

    private fun parseGameTimestamp(value: String?): LocalDateTime? =
        try {
            value?.let { LocalDateTime.parse(it, gameTimestampFormat) }
        } catch (e: Exception) {
            null
        }
}
