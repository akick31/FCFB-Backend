package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.user.TransactionType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.model.User
import com.fcfb.arceus.repositories.CoachTransactionLogRepository
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.service.log.UsernameHistoryService
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class CoachStatsService(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
    private val teamRepository: TeamRepository,
    private val coachTransactionLogRepository: CoachTransactionLogRepository,
    private val seasonStatsService: SeasonStatsService,
    private val userRepository: UserRepository,
    private val usernameHistoryService: UsernameHistoryService,
) {
    private data class Stint(
        val team: String,
        val start: LocalDateTime,
        val end: LocalDateTime?,
    )

    private val transactionDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
    private val gameTimestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val minimumStintLength: Duration = Duration.ofDays(14)

    fun getCoachStats(coach: String): List<SeasonStats> {
        val user = userRepository.findByUsername(coach)
        val discordId = user?.discordId
        val coachNames = resolveCoachNames(coach, user)
        val stints =
            buildStints(
                discordId,
                coachNames,
            ).filter { it.end == null || Duration.between(it.start, it.end) >= minimumStintLength }
        if (stints.isEmpty()) {
            return emptyList()
        }

        val teams = stints.map { it.team }.toSet()
        val gamesByTeam = teams.associateWith { teamGames(it) }
        val gameStatsByTeam = teams.associateWith { gameStatsRepository.findByTeam(it) }

        val now = LocalDateTime.now()
        val coachGameStats = mutableListOf<GameStats>()
        val seen = mutableSetOf<String>()
        for (stint in stints) {
            val end = stint.end ?: now
            val statsByGameId = (gameStatsByTeam[stint.team] ?: emptyList()).associateBy { it.gameId }
            for (game in gamesByTeam[stint.team] ?: emptyList()) {
                val timestamp = parseGameTimestamp(game.timestamp) ?: continue
                if (timestamp < stint.start || timestamp > end) continue
                val stats = statsByGameId[game.gameId] ?: continue
                if (seen.add("${game.gameId}_${stint.team}")) {
                    coachGameStats.add(stats)
                }
            }
        }
        if (coachGameStats.isEmpty()) {
            return emptyList()
        }

        val gameStatsByGameId = gameStatsRepository.findByGameIdIn(coachGameStats.map { it.gameId }.toSet()).groupBy { it.gameId }
        val conferenceByTeam = teamRepository.findAll().mapNotNull { team -> team.name?.let { it to team.conference } }.toMap()

        return coachGameStats
            .filter { it.season != null && it.team != null }
            .groupBy { "${it.team}_${it.season}" }
            .map { (_, rows) ->
                val first = rows.first()
                seasonStatsService.aggregateGameStatsToSeasonStats(
                    rows,
                    first.team!!,
                    first.season!!,
                    gameStatsByGameId,
                    conferenceByTeam[first.team],
                )
            }
            .sortedWith(compareByDescending<SeasonStats> { it.seasonNumber }.thenBy { it.team })
    }

    private fun teamGames(team: String): List<Game> =
        (gameRepository.findByHomeTeam(team) + gameRepository.findByAwayTeam(team))
            .filter { it.gameType != GameType.SCRIMMAGE }

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
    ): List<Stint> {
        val entries =
            coachTransactionLogRepository.getEntireCoachTransactionLog()
                .filter { entry ->
                    val matchesDiscordId = discordId != null && (entry.coachDiscordIds ?: emptyList()).contains(discordId)
                    val matchesName = (entry.coach ?: emptyList()).any { name -> coachNames.contains(name) }
                    matchesDiscordId || matchesName
                }
                .mapNotNull { entry -> parseTransactionDate(entry.transactionDate)?.let { entry to it } }
                .sortedBy { it.second }

        val stints = mutableListOf<Stint>()
        val open = mutableMapOf<String, LocalDateTime>()
        for ((entry, date) in entries) {
            val team = entry.team ?: continue
            when (entry.transaction) {
                TransactionType.HIRED, TransactionType.HIRED_INTERIM -> open[team] = date
                TransactionType.FIRED -> open.remove(team)?.let { stints.add(Stint(team, it, date)) }
                else -> {}
            }
        }
        open.forEach { (team, start) -> stints.add(Stint(team, start, null)) }
        return stints
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
