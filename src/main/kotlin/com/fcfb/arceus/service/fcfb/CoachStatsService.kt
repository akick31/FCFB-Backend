package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.Game
import com.fcfb.arceus.model.GameStats
import com.fcfb.arceus.model.SeasonStats
import com.fcfb.arceus.repositories.GameRepository
import com.fcfb.arceus.repositories.GameStatsRepository
import com.fcfb.arceus.repositories.TeamRepository
import com.fcfb.arceus.repositories.UserRepository
import com.fcfb.arceus.service.fcfb.coach.CoachStintService
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CoachStatsService(
    private val gameStatsRepository: GameStatsRepository,
    private val gameRepository: GameRepository,
    private val teamRepository: TeamRepository,
    private val seasonStatsService: SeasonStatsService,
    private val userRepository: UserRepository,
    private val coachStintService: CoachStintService,
) {
    fun getCoachStats(coach: String): List<SeasonStats> {
        val user = userRepository.findByUsername(coach)
        val stints = coachStintService.getStintsForCoach(coach, user)
        val currentTeam = user?.team
        val teams = (stints.map { it.team } + listOfNotNull(currentTeam)).toSet()
        if (teams.isEmpty()) {
            return emptyList()
        }

        val gamesByTeam = teams.associateWith { teamGames(it) }
        val gameStatsByTeam = teams.associateWith { gameStatsRepository.findByTeam(it) }
        coachStintService.backdateStartsToGames(stints, earliestGameByTeam(gamesByTeam))

        val coachGameStats = mutableListOf<GameStats>()
        val seen = mutableSetOf<String>()
        for (team in teams) {
            val statsByGameId = (gameStatsByTeam[team] ?: emptyList()).associateBy { it.gameId }
            for (game in gamesByTeam[team] ?: emptyList()) {
                val timestamp = coachStintService.parseGameTimestamp(game.timestamp) ?: continue
                if (!coachStintService.countsForCoach(stints, currentTeam, team, timestamp)) continue
                val stats = statsByGameId[game.gameId] ?: continue
                if (seen.add("${game.gameId}_$team")) {
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

    private fun earliestGameByTeam(gamesByTeam: Map<String, List<Game>>): Map<String, LocalDateTime> =
        gamesByTeam.mapNotNull { (team, games) ->
            games.mapNotNull { coachStintService.parseGameTimestamp(it.timestamp) }.minOrNull()?.let { team to it }
        }.toMap()
}
